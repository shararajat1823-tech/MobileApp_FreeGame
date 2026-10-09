package com.miniplay.app.games.ludo

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
import kotlin.random.Random

enum class LudoScreenPhase { MENU, PLAYING }

data class LudoUiState(
    val screen: LudoScreenPhase = LudoScreenPhase.MENU,
    val game: LudoState? = null,
    val seats: List<Seat> = emptyList(),
    val mode: LudoMode = LudoMode.VS_COMPUTER,
    val settings: LudoSettings = LudoSettings(),
    val stats: LudoStats = LudoStats(),
    val history: List<LudoMatchRecord> = emptyList(),
    val hasSavedMatch: Boolean = false,
    val diceFace: Int = 1,
    val diceRolling: Boolean = false,
    val animating: Boolean = false,
    val paused: Boolean = false,
    val showTutorial: Boolean = false,
    val winner: LudoColor? = null,
) {
    val inputLocked: Boolean get() = diceRolling || animating || paused
    val currentSeat: Seat? get() = game?.let { g -> seats.firstOrNull { it.color == g.currentColor } }
    val isCurrentHuman: Boolean get() = currentSeat?.type == SeatType.HUMAN
    val canRoll: Boolean
        get() = game?.phase == LudoPhase.AWAIT_ROLL && isCurrentHuman && !inputLocked && winner == null
}

/**
 * Orchestrates a Ludo match on top of the pure [LudoEngine]: dice animation,
 * turn flow, bot scheduling, legal-move prompting, single-move auto-apply, input
 * locking during animation, extra turns, win handling, stats/result recording,
 * and lifecycle save/restore. The engine stays the single source of truth; this
 * class never computes rules itself.
 */
class LudoViewModel(
    private val recordGameResult: RecordGameResultUseCase,
    private val soundManager: SoundManager,
    private val hapticManager: HapticManager,
    private val prefs: LudoPrefs,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LudoUiState())
    val uiState = _uiState.asStateFlow()

    /** Token-move animations for the board (buffered so a dropped frame can't desync state). */
    private val _moveAnims = MutableSharedFlow<LudoMoveAnim>(extraBufferCapacity = 8)
    val moveAnims = _moveAnims.asSharedFlow()

    private val dice = Dice()
    private var pendingJob: Job? = null
    private var busy = false
    private var animId = 0L

    // Per-match counters for stats.
    private var movesThisMatch = 0
    private var capturesThisMatch = 0
    private var tokensHomeThisMatch = 0
    private var startedAt = 0L

    init {
        viewModelScope.launch { prefs.observeSettings().collect { s -> _uiState.update { it.copy(settings = s) } } }
        viewModelScope.launch { prefs.observeStats().collect { s -> _uiState.update { it.copy(stats = s) } } }
        viewModelScope.launch { prefs.observeHistory().collect { h -> _uiState.update { it.copy(history = h) } } }
        viewModelScope.launch { prefs.observeHasSavedMatch().collect { b -> _uiState.update { it.copy(hasSavedMatch = b) } } }
    }

    // ---------------------------- start / navigation ----------------------------

    fun startGame(mode: LudoMode, playerCount: Int) {
        pendingJob?.cancel()
        busy = false
        val settings = _uiState.value.settings
        val humans = when (mode) {
            LudoMode.LOCAL -> playerCount
            LudoMode.VS_COMPUTER, LudoMode.PRACTICE -> 1
        }
        val difficulty = if (mode == LudoMode.PRACTICE) GameDifficulty.EASY else settings.botDifficulty
        val seats = LudoSetup.seats(mode, playerCount, humans, difficulty)
        val game = LudoEngine.newGame(seats.map { it.color }, settings.toRules())
        resetCounters()
        viewModelScope.launch { prefs.clearSavedMatch() }
        _uiState.update {
            it.copy(
                screen = LudoScreenPhase.PLAYING,
                game = game,
                seats = seats,
                mode = mode,
                paused = false,
                winner = null,
                diceFace = 1,
                diceRolling = false,
                animating = false,
                showTutorial = !it.settings.tutorialSeen,
            )
        }
        scheduleBotOrAuto()
    }

    fun resumeSavedMatch() {
        viewModelScope.launch {
            val saved = prefs.loadSavedMatch() ?: return@launch
            pendingJob?.cancel()
            busy = false
            resetCounters()
            _uiState.update {
                it.copy(
                    screen = LudoScreenPhase.PLAYING,
                    game = saved.state,
                    seats = saved.seats,
                    mode = saved.mode,
                    paused = false,
                    winner = saved.state.winner,
                    diceFace = saved.state.dice ?: 1,
                    diceRolling = false,
                    animating = false,
                    showTutorial = false,
                )
            }
            scheduleBotOrAuto()
        }
    }

    fun backToMenu() {
        pendingJob?.cancel()
        busy = false
        val s = _uiState.value
        val game = s.game
        // Persist an unfinished match so it can be resumed later.
        if (game != null && !game.isOver && s.winner == null) {
            viewModelScope.launch { prefs.saveMatch(game, s.seats, s.mode) }
        }
        _uiState.update { it.copy(screen = LudoScreenPhase.MENU, paused = false) }
    }

    // ---------------------------- settings ----------------------------

    fun setBotDifficulty(value: GameDifficulty) { viewModelScope.launch { prefs.setBotDifficulty(value) } }
    fun setReducedEffects(value: Boolean) { viewModelScope.launch { prefs.setReducedEffects(value) } }
    fun setQuickWin(value: Boolean) { viewModelScope.launch { prefs.setQuickWin(value) } }
    fun setThreeSixes(value: Boolean) { viewModelScope.launch { prefs.setThreeSixes(value) } }
    fun dismissTutorial() {
        _uiState.update { it.copy(showTutorial = false) }
        viewModelScope.launch { prefs.setTutorialSeen() }
    }

    // ---------------------------- pause / lifecycle ----------------------------

    fun pauseGame() {
        if (_uiState.value.game?.isOver != false) return
        pendingJob?.cancel()
        _uiState.update { it.copy(paused = true) }
    }

    fun resumeGame() {
        if (!_uiState.value.paused) return
        _uiState.update { it.copy(paused = false) }
        scheduleBotOrAuto()
    }

    /** Called when the app is backgrounded: pause and persist the match. */
    fun onStop() {
        val s = _uiState.value
        val game = s.game ?: return
        if (s.screen != LudoScreenPhase.PLAYING || game.isOver) return
        pendingJob?.cancel()
        _uiState.update { it.copy(paused = true) }
        viewModelScope.launch { prefs.saveMatch(game, s.seats, s.mode) }
    }

    // ---------------------------- human input ----------------------------

    fun onRollClick() {
        val s = _uiState.value
        if (!s.canRoll || busy) return
        viewModelScope.launch { doRoll() }
    }

    fun onTokenTap(color: LudoColor, tokenIndex: Int) {
        val s = _uiState.value
        val game = s.game ?: return
        if (s.inputLocked || busy || s.winner != null) return
        if (game.phase != LudoPhase.AWAIT_MOVE) return
        if (color != game.currentColor || !s.isCurrentHuman) return
        if (tokenIndex !in game.legalMoves) return
        viewModelScope.launch { doMove(tokenIndex) }
    }

    // ---------------------------- core flow ----------------------------

    private fun scheduleBotOrAuto() {
        pendingJob?.cancel()
        val s = _uiState.value
        val game = s.game ?: return
        if (s.paused || game.isOver || s.winner != null) return
        val seat = s.currentSeat ?: return
        val turnAtSchedule = game.turnId
        when (game.phase) {
            LudoPhase.AWAIT_ROLL -> if (seat.isBot) {
                pendingJob = viewModelScope.launch {
                    delay(BOT_THINK_MS)
                    if (stillCurrent(turnAtSchedule)) doRoll()
                }
            }
            LudoPhase.AWAIT_MOVE -> {
                if (seat.isBot) {
                    pendingJob = viewModelScope.launch {
                        delay(BOT_THINK_MS)
                        if (stillCurrent(turnAtSchedule)) {
                            val choice = LudoBot.chooseMove(game, seat.difficulty)
                            if (choice >= 0) doMove(choice)
                        }
                    }
                } else if (game.legalMoves.size == 1) {
                    // Exactly one legal move: play it automatically after a short beat.
                    pendingJob = viewModelScope.launch {
                        delay(AUTO_MOVE_MS)
                        if (stillCurrent(turnAtSchedule)) doMove(game.legalMoves.first())
                    }
                }
                // else: human with a choice — wait for a tap.
            }
            LudoPhase.GAME_OVER -> Unit
        }
    }

    private fun stillCurrent(turnId: Int): Boolean {
        val s = _uiState.value
        return !s.paused && s.game?.turnId == turnId && s.game?.isOver == false && s.winner == null
    }

    private suspend fun doRoll() {
        if (busy) return
        val game = _uiState.value.game ?: return
        if (game.phase != LudoPhase.AWAIT_ROLL || game.isOver) return
        busy = true
        try {
            val reduced = _uiState.value.settings.reducedEffects
            _uiState.update { it.copy(diceRolling = true) }
            val frames = if (reduced) 4 else 9
            repeat(frames) {
                _uiState.update { it.copy(diceFace = Random.nextInt(1, 7)) }
                soundManager.play(SoundEffect.CLICK)
                delay(if (reduced) 45 else 55)
            }
            val value = dice.roll()
            _uiState.update { it.copy(diceFace = value, diceRolling = false) }
            hapticManager.perform(HapticFeedbackType.TICK)

            val rolled = LudoEngine.roll(game, value)
            _uiState.update { it.copy(game = rolled) }
            if (rolled.phase == LudoPhase.AWAIT_ROLL && !rolled.isOver) {
                // No usable roll — turn passed to the next player.
                hapticManager.perform(HapticFeedbackType.LIGHT)
                delay(400)
            } else {
                delay(180)
            }
        } finally {
            busy = false
        }
        scheduleBotOrAuto()
    }

    private suspend fun doMove(tokenIndex: Int) {
        if (busy) return
        val before = _uiState.value.game ?: return
        if (before.phase != LudoPhase.AWAIT_MOVE || before.isOver) return
        if (tokenIndex !in before.legalMoves) return
        busy = true
        try {
            val next = LudoEngine.applyMove(before, tokenIndex)
            val event = next.lastEvent
            if (next.turnId == before.turnId || event == null) return

            movesThisMatch++
            capturesThisMatch += event.captured.size
            if (event.finishedToken) tokensHomeThisMatch++

            val capturedCells = if (event.toProgress in 0..LudoBoard.LAST_RING && event.captured.isNotEmpty()) {
                listOf(LudoBoard.cellFor(event.color!!, event.toProgress))
            } else {
                emptyList()
            }
            val anim = LudoMoveAnim(
                id = animId++,
                color = event.color!!,
                tokenIndex = event.tokenIndex,
                fromProgress = event.fromProgress,
                toProgress = event.toProgress,
                captured = capturedCells,
                entered = event.enteredBoard,
                finished = event.finishedToken,
            )
            val reduced = _uiState.value.settings.reducedEffects
            _uiState.update { it.copy(game = next, animating = true) }
            _moveAnims.tryEmit(anim)

            val steps = if (event.enteredBoard) 1 else (event.toProgress - event.fromProgress).coerceAtLeast(1)
            val perStep = if (reduced) 45L else 85L
            delay(120 + steps * perStep)
            _uiState.update { it.copy(animating = false) }

            when {
                event.captured.isNotEmpty() -> {
                    soundManager.play(SoundEffect.WRONG)
                    hapticManager.perform(HapticFeedbackType.HEAVY)
                }
                event.finishedToken -> {
                    soundManager.play(SoundEffect.CORRECT)
                    hapticManager.perform(HapticFeedbackType.SUCCESS)
                }
                else -> {
                    soundManager.play(SoundEffect.CARD_FLIP)
                    hapticManager.perform(HapticFeedbackType.LIGHT)
                }
            }

            if (next.isOver) {
                finalize(next)
                return
            }
        } finally {
            busy = false
        }
        scheduleBotOrAuto()
    }

    private fun finalize(state: LudoState) {
        pendingJob?.cancel()
        val winner = state.winner
        soundManager.play(SoundEffect.WIN)
        hapticManager.perform(HapticFeedbackType.SUCCESS)
        _uiState.update { it.copy(game = state, winner = winner, paused = false) }

        val s = _uiState.value
        val humanColors = s.seats.filter { it.type == SeatType.HUMAN }.map { it.color }
        val humanWon: Boolean? = if (s.mode == LudoMode.LOCAL) null else winner in humanColors
        val duration = System.currentTimeMillis() - startedAt
        viewModelScope.launch {
            prefs.clearSavedMatch()
            prefs.recordMatch(
                modeLabel = s.mode.name,
                winner = winner,
                humanWon = humanWon,
                captures = capturesThisMatch,
                tokensHome = tokensHomeThisMatch,
                moves = movesThisMatch,
            )
            recordGameResult(
                GameResult(
                    gameId = GameIds.LUDO,
                    score = 0,
                    won = humanWon,
                    difficulty = s.settings.botDifficulty,
                    durationMillis = duration,
                    completed = true,
                    metrics = mapOf(
                        "players" to s.seats.size.toLong(),
                        "mode" to s.mode.ordinal.toLong(),
                        "captures" to capturesThisMatch.toLong(),
                    ),
                ),
            )
        }
    }

    private fun resetCounters() {
        movesThisMatch = 0
        capturesThisMatch = 0
        tokensHomeThisMatch = 0
        startedAt = System.currentTimeMillis()
    }

    private companion object {
        const val BOT_THINK_MS = 650L
        const val AUTO_MOVE_MS = 350L
    }
}
