package com.miniplay.app.games.snake

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miniplay.app.core.audio.SoundEffect
import com.miniplay.app.core.audio.SoundManager
import com.miniplay.app.core.haptics.HapticFeedbackType
import com.miniplay.app.core.haptics.HapticManager
import com.miniplay.app.domain.model.GameDifficulty
import com.miniplay.app.domain.model.GameIds
import com.miniplay.app.domain.model.GameResult
import com.miniplay.app.domain.usecase.RecordGameResultUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class SnakePhase { MENU, COUNTDOWN, RUNNING, PAUSED, GAME_OVER }

/** Fired when food is eaten, so the UI can spawn a particle burst at [cell]. */
data class SnakeEatEvent(val cell: Cell, val type: FoodType, val id: Long)

data class SnakeUiState(
    val phase: SnakePhase = SnakePhase.MENU,
    val mode: SnakeMode = SnakeMode.CLASSIC,
    val game: SnakeState,
    val settings: SnakeSettings = SnakeSettings(),
    val bests: Map<SnakeMode, Int> = emptyMap(),
    val countdownValue: Int = 0,
    val showGo: Boolean = false,
    val timeLeftMillis: Long? = null,
    val tick: Long = 0L,
    val tickIntervalMillis: Long = 200L,
    val isNewBest: Boolean = false,
    val showTutorial: Boolean = false,
) {
    val bestForMode: Int get() = bests[mode] ?: 0
    val running: Boolean get() = phase == SnakePhase.RUNNING
    val gameOver: Boolean get() = phase == SnakePhase.GAME_OVER
    val skin: SnakeSkin get() = SnakeSkins.byId(settings.skinId)
}

/**
 * Drives Snake across its lifecycle (menu → countdown → running → paused →
 * game over). The pure engine owns the rules; this class owns the deterministic,
 * time-based clock, a 2-deep input buffer for responsive turns, mode/timer logic,
 * feedback, persistence (per-mode bests) and result recording.
 */
class SnakeViewModel(
    private val recordGameResult: RecordGameResultUseCase,
    private val soundManager: SoundManager,
    private val hapticManager: HapticManager,
    private val prefs: SnakePrefs,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SnakeUiState(game = SnakeEngine.newGame(SnakeModes.config(SnakeMode.CLASSIC))),
    )
    val uiState = _uiState.asStateFlow()

    private val _eatEvents = MutableSharedFlow<SnakeEatEvent>(extraBufferCapacity = 8)
    val eatEvents = _eatEvents.asSharedFlow()

    // Buffered directional input: at most two queued turns so fast flicks aren't lost.
    private val inputQueue = ArrayDeque<SnakeDir>()

    private var loopJob: Job? = null
    private var countdownJob: Job? = null
    private var startedAt = 0L
    private var resultRecorded = false
    private var eventId = 0L

    init {
        viewModelScope.launch {
            prefs.observeSettings().collect { s -> _uiState.update { it.copy(settings = s) } }
        }
        viewModelScope.launch {
            prefs.observeBests().collect { b -> _uiState.update { it.copy(bests = b) } }
        }
    }

    // ------------------------------- input -------------------------------

    /** Queues a turn. Rejects duplicates/180s relative to the latest intended direction. */
    fun turn(dir: SnakeDir) {
        if (_uiState.value.phase != SnakePhase.RUNNING) return
        val reference = inputQueue.lastOrNull() ?: _uiState.value.game.dir
        if (dir == reference || dir.isOpposite(reference)) return
        if (inputQueue.size >= 2) return
        inputQueue.addLast(dir)
    }

    // ---------------------------- lifecycle ------------------------------

    fun startNewGame(mode: SnakeMode) {
        loopJob?.cancel()
        countdownJob?.cancel()
        inputQueue.clear()
        resultRecorded = false
        _uiState.update {
            it.copy(
                mode = mode,
                game = SnakeEngine.newGame(SnakeModes.config(mode)),
                phase = SnakePhase.COUNTDOWN,
                countdownValue = 3,
                showGo = false,
                timeLeftMillis = SnakeModes.timeLimitMillis(mode),
                tick = 0L,
                isNewBest = false,
                showTutorial = !it.settings.tutorialSeen,
            )
        }
        launchCountdown()
    }

    fun retry() = startNewGame(_uiState.value.mode)

    fun backToMenu() {
        loopJob?.cancel()
        countdownJob?.cancel()
        _uiState.update { it.copy(phase = SnakePhase.MENU) }
    }

    fun pauseGame() {
        if (_uiState.value.phase != SnakePhase.RUNNING) return
        loopJob?.cancel()
        soundManager.play(SoundEffect.CLICK)
        _uiState.update { it.copy(phase = SnakePhase.PAUSED) }
    }

    fun resumeGame() {
        if (_uiState.value.phase != SnakePhase.PAUSED) return
        _uiState.update { it.copy(phase = SnakePhase.RUNNING) }
        launchLoop()
    }

    /** Called by the screen when the app is backgrounded. */
    fun pauseForLifecycle() = pauseGame()

    fun dismissTutorial() {
        _uiState.update { it.copy(showTutorial = false) }
        viewModelScope.launch { prefs.setTutorialSeen() }
    }

    // ---------------------------- settings -------------------------------

    fun setSkin(id: String) {
        viewModelScope.launch { prefs.setSkin(id) }
    }

    fun setSensitivity(value: SwipeSensitivity) {
        viewModelScope.launch { prefs.setSensitivity(value) }
    }

    fun setControlStyle(value: ControlStyle) {
        viewModelScope.launch { prefs.setControlStyle(value) }
    }

    // ------------------------------ clock --------------------------------

    private fun launchCountdown() {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            for (n in 3 downTo 1) {
                _uiState.update { it.copy(countdownValue = n, showGo = false) }
                soundManager.play(SoundEffect.CLICK)
                hapticManager.perform(HapticFeedbackType.TICK)
                delay(650)
            }
            _uiState.update { it.copy(countdownValue = 0, showGo = true) }
            soundManager.play(SoundEffect.CORRECT)
            delay(400)
            _uiState.update { it.copy(showGo = false) }
            beginRunning()
        }
    }

    private fun beginRunning() {
        _uiState.update { it.copy(phase = SnakePhase.RUNNING) }
        startedAt = System.currentTimeMillis()
        launchLoop()
    }

    private fun launchLoop() {
        loopJob?.cancel()
        loopJob = viewModelScope.launch {
            while (_uiState.value.phase == SnakePhase.RUNNING) {
                val interval = SnakeModes.intervalMillis(_uiState.value.mode, _uiState.value.game.length)
                delay(interval)
                if (_uiState.value.phase != SnakePhase.RUNNING) break

                val timeLeft = _uiState.value.timeLeftMillis
                if (timeLeft != null) {
                    val left = (timeLeft - interval).coerceAtLeast(0)
                    _uiState.update { it.copy(timeLeftMillis = left) }
                    if (left <= 0L) { finishGame(); break }
                }

                stepOnce()
                if (_uiState.value.game.status == SnakeStatus.GAME_OVER) break
            }
        }
    }

    private fun stepOnce() {
        val prev = _uiState.value.game
        if (prev.status != SnakeStatus.RUNNING) return

        val buffered = inputQueue.removeFirstOrNull()
        val afterTurn = if (buffered != null) SnakeEngine.turn(prev, buffered) else prev
        val next = SnakeEngine.step(afterTurn)

        _uiState.update {
            it.copy(
                game = next,
                tick = it.tick + 1,
                tickIntervalMillis = SnakeModes.intervalMillis(it.mode, next.length),
            )
        }

        if (next.score > prev.score) {
            val eaten = prev.food
            _eatEvents.tryEmit(SnakeEatEvent(eaten.cell, eaten.type, eventId++))
            when (eaten.type) {
                FoodType.GOLDEN -> {
                    soundManager.play(SoundEffect.WIN)
                    hapticManager.perform(HapticFeedbackType.SUCCESS)
                }
                FoodType.BONUS -> {
                    soundManager.play(SoundEffect.CORRECT)
                    hapticManager.perform(HapticFeedbackType.LIGHT)
                }
                FoodType.NORMAL -> {
                    soundManager.play(SoundEffect.CORRECT)
                    hapticManager.perform(HapticFeedbackType.TICK)
                }
            }
        }

        if (next.status == SnakeStatus.GAME_OVER) finishGame()
    }

    private fun finishGame() {
        if (_uiState.value.phase == SnakePhase.GAME_OVER) return
        loopJob?.cancel()
        soundManager.play(SoundEffect.GAME_OVER)
        hapticManager.perform(HapticFeedbackType.ERROR)

        val finished = _uiState.value
        _uiState.update { it.copy(phase = SnakePhase.GAME_OVER) }

        if (resultRecorded) return
        resultRecorded = true
        val duration = System.currentTimeMillis() - startedAt
        val score = finished.game.score
        viewModelScope.launch {
            recordGameResult(
                GameResult(
                    gameId = GameIds.SNAKE,
                    score = score.toLong(),
                    won = null,
                    difficulty = GameDifficulty.MEDIUM,
                    durationMillis = duration,
                    completed = true,
                    metrics = mapOf(
                        "length" to finished.game.length.toLong(),
                        "mode" to finished.mode.ordinal.toLong(),
                    ),
                ),
            )
            val newBest = prefs.updateBest(finished.mode, score)
            if (newBest) _uiState.update { it.copy(isNewBest = true) }
        }
    }
}
