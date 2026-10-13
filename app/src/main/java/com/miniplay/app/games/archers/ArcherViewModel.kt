package com.miniplay.app.games.archers

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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

enum class ArcherScreen { MENU, PLAYING }
enum class ArcherMode { VS_AI, PRACTICE }

data class ArcherUiState(
    val screen: ArcherScreen = ArcherScreen.MENU,
    val mode: ArcherMode = ArcherMode.VS_AI,
    val settings: ArcherSettings = ArcherSettings(),
    val stats: ArcherStats = ArcherStats(),
    val phase: ArcherPhase = ArcherPhase.AIMING,
    val isPlayerTurn: Boolean = true,
    val winner: Int? = null,
    val healthP0: Float = 1f,
    val healthP1: Float = 1f,
    val paused: Boolean = false,
    val showTutorial: Boolean = false,
    val aiming: Boolean = false,
    val aimAngle: Float = 0.7f,
    val aimPower: Float = 0.6f,
    val aiThinking: Boolean = false,
    val preview: List<Pair<Float, Float>> = emptyList(),
) {
    val playing: Boolean get() = screen == ArcherScreen.PLAYING
    val canAim: Boolean
        get() = playing && !paused && winner == null && phase == ArcherPhase.AIMING && isPlayerTurn
}

/**
 * Drives Ragdoll Archers: a fixed-timestep physics loop (fed by the view's frame
 * clock so it is framerate-independent), turn flow, drag-to-aim with live
 * trajectory preview, on-device AI scheduling, Practice mode, lifecycle
 * pause/resume, audio/haptics, and result/stat recording. The authoritative
 * [match] stays a pure simulation; this class only sequences it and exposes a
 * small snapshot for Compose — a seam a future online transport can sit behind.
 */
class ArcherViewModel(
    private val recordGameResult: RecordGameResultUseCase,
    private val soundManager: SoundManager,
    private val hapticManager: HapticManager,
    private val prefs: ArcherPrefs,
) : ViewModel() {

    val match = ArcherMatch()

    private val _uiState = MutableStateFlow(ArcherUiState())
    val uiState = _uiState.asStateFlow()

    private var lastNanos = 0L
    private var accNanos = 0L
    private val stepNanos = (match.config.world.dt * 1_000_000_000.0).toLong()
    private var lastHitCount = 0
    private var endedHandled = false
    private var aiJob: Job? = null

    init {
        viewModelScope.launch { prefs.observeSettings().collect { s -> _uiState.update { it.copy(settings = s) } } }
        viewModelScope.launch { prefs.observeStats().collect { s -> _uiState.update { it.copy(stats = s) } } }
    }

    // ------------------------------ navigation ------------------------------

    fun startMatch(mode: ArcherMode) {
        aiJob?.cancel()
        match.reset()
        lastNanos = 0L
        accNanos = 0L
        lastHitCount = match.hitCount
        endedHandled = false
        _uiState.update {
            it.copy(
                screen = ArcherScreen.PLAYING,
                mode = mode,
                paused = false,
                winner = null,
                phase = match.phase,
                isPlayerTurn = true,
                healthP0 = 1f,
                healthP1 = 1f,
                aiming = false,
                preview = emptyList(),
                aiThinking = false,
                showTutorial = !it.settings.tutorialSeen && mode == ArcherMode.VS_AI,
            )
        }
    }

    fun backToMenu() {
        aiJob?.cancel()
        _uiState.update { it.copy(screen = ArcherScreen.MENU, paused = false, aiming = false, preview = emptyList()) }
    }

    // ------------------------------ settings ------------------------------

    fun setDifficulty(value: GameDifficulty) { viewModelScope.launch { prefs.setDifficulty(value) } }
    fun setScreenShake(value: Boolean) { viewModelScope.launch { prefs.setScreenShake(value) } }
    fun setReducedEffects(value: Boolean) { viewModelScope.launch { prefs.setReducedEffects(value) } }
    fun resetProgress() { viewModelScope.launch { prefs.resetProgress() } }
    fun dismissTutorial() {
        _uiState.update { it.copy(showTutorial = false) }
        viewModelScope.launch { prefs.setTutorialSeen() }
    }

    // ------------------------------ lifecycle ------------------------------

    fun pause() {
        if (_uiState.value.winner != null) return
        aiJob?.cancel()
        _uiState.update { it.copy(paused = true, aiThinking = false) }
    }

    fun resume() {
        if (!_uiState.value.paused) return
        lastNanos = 0L
        _uiState.update { it.copy(paused = false) }
    }

    fun onStop() {
        if (_uiState.value.playing && _uiState.value.winner == null) pause()
    }

    // ------------------------------ input ------------------------------

    /** Called by the board as the player drags; updates the live preview. */
    fun updateAim(angle: Float, power: Float) {
        if (!_uiState.value.canAim) return
        val preview = match.previewTrajectory(angle, power, ArcherWeapon.STANDARD, maxSamples = 20)
        _uiState.update { it.copy(aiming = true, aimAngle = angle, aimPower = power, preview = preview) }
    }

    /** Called by the board on release; fires if it's the player's turn. */
    fun releaseAim() {
        val s = _uiState.value
        if (!s.canAim || !s.aiming) {
            _uiState.update { it.copy(aiming = false, preview = emptyList()) }
            return
        }
        soundManager.play(SoundEffect.CORRECT)
        hapticManager.perform(HapticFeedbackType.MEDIUM)
        match.fire(s.aimAngle, s.aimPower, ArcherWeapon.STANDARD)
        _uiState.update { it.copy(aiming = false, preview = emptyList(), phase = match.phase) }
    }

    fun cancelAim() {
        _uiState.update { it.copy(aiming = false, preview = emptyList()) }
    }

    // ------------------------------ sim loop ------------------------------

    /** Fed by the board's withFrameNanos loop. Advances the sim in fixed steps. */
    fun onFrame(now: Long) {
        val s = _uiState.value
        if (!s.playing || s.paused) {
            lastNanos = now
            return
        }
        if (lastNanos == 0L) {
            lastNanos = now
            return
        }
        var delta = now - lastNanos
        lastNanos = now
        if (delta < 0) delta = 0
        if (delta > 50_000_000L) delta = 50_000_000L // clamp after a hitch/resume
        accNanos += delta
        var steps = 0
        while (accNanos >= stepNanos && steps < 8) {
            match.tick()
            accNanos -= stepNanos
            steps++
        }
        postStep()
    }

    private fun postStep() {
        // Practice keeps the human shooting.
        if (_uiState.value.mode == ArcherMode.PRACTICE &&
            match.phase == ArcherPhase.AIMING && match.current == 1 && match.winner == null
        ) {
            match.setTurn(0)
        }

        // Hit cue (one-shot per hit).
        if (match.hitCount > lastHitCount) {
            lastHitCount = match.hitCount
            soundManager.play(SoundEffect.WRONG)
            hapticManager.perform(HapticFeedbackType.HEAVY)
        }

        // AI turn scheduling.
        if (_uiState.value.mode == ArcherMode.VS_AI &&
            match.phase == ArcherPhase.AIMING && match.current == 1 && match.winner == null
        ) {
            scheduleAi()
        }

        // Match end.
        if (match.winner != null && !endedHandled) handleEnd()

        syncSnapshot()
    }

    private fun scheduleAi() {
        if (aiJob?.isActive == true) return
        _uiState.update { it.copy(aiThinking = true) }
        aiJob = viewModelScope.launch {
            delay(750)
            if (!_uiState.value.paused && match.phase == ArcherPhase.AIMING && match.current == 1 && match.winner == null) {
                val shot = ArcherAI.chooseShot(match, _uiState.value.settings.difficulty, Random.Default)
                soundManager.play(SoundEffect.CLICK)
                match.fire(shot.angle, shot.power, ArcherWeapon.STANDARD)
            }
            _uiState.update { it.copy(aiThinking = false) }
        }
    }

    private fun handleEnd() {
        endedHandled = true
        aiJob?.cancel()
        val winner = match.winner
        if (_uiState.value.mode == ArcherMode.PRACTICE) {
            // Keep the range going: respawn the dummy after a beat.
            viewModelScope.launch {
                delay(1400)
                if (_uiState.value.playing) {
                    match.reset()
                    lastHitCount = match.hitCount
                    endedHandled = false
                    lastNanos = 0L
                    syncSnapshot()
                }
            }
            return
        }
        val playerWon = winner == 0
        soundManager.play(if (playerWon) SoundEffect.WIN else SoundEffect.GAME_OVER)
        hapticManager.perform(if (playerWon) HapticFeedbackType.SUCCESS else HapticFeedbackType.ERROR)
        val difficulty = _uiState.value.settings.difficulty
        viewModelScope.launch {
            prefs.recordMatch(playerWon)
            recordGameResult(
                GameResult(
                    gameId = GameIds.ARCHERS,
                    score = 0,
                    won = playerWon,
                    difficulty = difficulty,
                    durationMillis = 0L,
                    completed = true,
                    metrics = mapOf("difficulty" to difficulty.ordinal.toLong()),
                ),
            )
        }
    }

    private fun syncSnapshot() {
        val hp0 = match.archers[0].healthFraction
        val hp1 = match.archers[1].healthFraction
        val phase = match.phase
        val playerTurn = match.current == 0
        val winner = match.winner
        val cur = _uiState.value
        if (cur.phase == phase && cur.isPlayerTurn == playerTurn && cur.winner == winner &&
            cur.healthP0 == hp0 && cur.healthP1 == hp1
        ) {
            return
        }
        _uiState.update {
            it.copy(phase = phase, isPlayerTurn = playerTurn, winner = winner, healthP0 = hp0, healthP1 = hp1)
        }
    }
}
