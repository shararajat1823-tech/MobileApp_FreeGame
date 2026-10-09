package com.miniplay.app.games.ludo

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.miniplay.app.domain.model.GameDifficulty
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** A restored in-progress match: engine state plus the seating it was played with. */
data class SavedMatch(
    val state: LudoState,
    val seats: List<Seat>,
    val mode: LudoMode,
)

/** Ludo settings, including the two supported rule variations. */
data class LudoSettings(
    val reducedEffects: Boolean = false,
    val botDifficulty: GameDifficulty = GameDifficulty.MEDIUM,
    /** Rule variation: finish just 1 token to win (a fast game) vs all 4 (standard). */
    val quickWin: Boolean = false,
    /** Rule variation: three consecutive sixes forfeit the turn. */
    val threeSixesForfeits: Boolean = true,
    val tutorialSeen: Boolean = false,
) {
    fun toRules(): LudoRules = LudoRules(
        threeSixesForfeits = threeSixesForfeits,
        tokensToWin = if (quickWin) 1 else LudoBoard.TOKENS,
    )
}

/** Aggregate local statistics shown on the Ludo menu. */
data class LudoStats(
    val played: Int = 0,
    val won: Int = 0,
    val captures: Int = 0,
    val tokensHome: Int = 0,
    val moves: Int = 0,
) {
    val winPercent: Int get() = if (played == 0) 0 else (won * 100) / played
}

/** One past match, newest first in history. [humanWon] is null for pass-and-play matches. */
data class LudoMatchRecord(
    val timestamp: Long,
    val modeLabel: String,
    val winner: LudoColor?,
    val humanWon: Boolean?,
)

/**
 * Ludo-local persistence on the shared app DataStore: settings, lifetime stats,
 * recent match history and a single saved in-progress match (so a backgrounded
 * game can resume). It writes only `ludo_*` keys, leaving every other game's
 * saved data — Snake scores included — untouched.
 */
class LudoPrefs(private val dataStore: DataStore<Preferences>) {

    fun observeSettings(): Flow<LudoSettings> = dataStore.data.map { p ->
        LudoSettings(
            reducedEffects = p[REDUCED_EFFECTS] ?: false,
            botDifficulty = p[BOT_DIFF]?.let { runCatching { GameDifficulty.valueOf(it) }.getOrNull() }
                ?: GameDifficulty.MEDIUM,
            quickWin = p[QUICK_WIN] ?: false,
            threeSixesForfeits = p[THREE_SIXES] ?: true,
            tutorialSeen = p[TUTORIAL_SEEN] ?: false,
        )
    }

    fun observeStats(): Flow<LudoStats> = dataStore.data.map { p ->
        LudoStats(
            played = p[STAT_PLAYED] ?: 0,
            won = p[STAT_WON] ?: 0,
            captures = p[STAT_CAPTURES] ?: 0,
            tokensHome = p[STAT_HOME] ?: 0,
            moves = p[STAT_MOVES] ?: 0,
        )
    }

    fun observeHistory(): Flow<List<LudoMatchRecord>> = dataStore.data.map { p ->
        decodeHistory(p[HISTORY])
    }

    fun observeHasSavedMatch(): Flow<Boolean> = dataStore.data.map { p ->
        !p[SAVED_MATCH].isNullOrBlank()
    }

    suspend fun setReducedEffects(value: Boolean) { dataStore.edit { it[REDUCED_EFFECTS] = value } }
    suspend fun setBotDifficulty(value: GameDifficulty) { dataStore.edit { it[BOT_DIFF] = value.name } }
    suspend fun setQuickWin(value: Boolean) { dataStore.edit { it[QUICK_WIN] = value } }
    suspend fun setThreeSixes(value: Boolean) { dataStore.edit { it[THREE_SIXES] = value } }
    suspend fun setTutorialSeen() { dataStore.edit { it[TUTORIAL_SEEN] = true } }

    /** Records a completed match: bumps lifetime stats and prepends a history row. */
    suspend fun recordMatch(
        modeLabel: String,
        winner: LudoColor?,
        humanWon: Boolean?,
        captures: Int,
        tokensHome: Int,
        moves: Int,
        timestamp: Long = System.currentTimeMillis(),
    ) {
        dataStore.edit { p ->
            p[STAT_PLAYED] = (p[STAT_PLAYED] ?: 0) + 1
            if (humanWon == true) p[STAT_WON] = (p[STAT_WON] ?: 0) + 1
            p[STAT_CAPTURES] = (p[STAT_CAPTURES] ?: 0) + captures
            p[STAT_HOME] = (p[STAT_HOME] ?: 0) + tokensHome
            p[STAT_MOVES] = (p[STAT_MOVES] ?: 0) + moves
            val existing = decodeHistory(p[HISTORY])
            val record = LudoMatchRecord(timestamp, modeLabel, winner, humanWon)
            p[HISTORY] = encodeHistory((listOf(record) + existing).take(MAX_HISTORY))
        }
    }

    suspend fun saveMatch(state: LudoState, seats: List<Seat>, mode: LudoMode) {
        dataStore.edit {
            it[SAVED_MATCH] = LudoSerialization.encode(state)
            it[SAVED_SEATS] = seats.joinToString(";") { s -> "${s.color.name},${s.type.name},${s.difficulty.name}" }
            it[SAVED_MODE] = mode.name
        }
    }

    suspend fun loadSavedMatch(): SavedMatch? {
        val prefs = dataStore.data.first()
        val state = LudoSerialization.decode(prefs[SAVED_MATCH]) ?: return null
        val seats = runCatching {
            prefs[SAVED_SEATS]?.split(";")?.filter { it.isNotEmpty() }?.map { part ->
                val (color, type, diff) = part.split(",")
                Seat(LudoColor.valueOf(color), SeatType.valueOf(type), GameDifficulty.valueOf(diff))
            }
        }.getOrNull() ?: return null
        if (seats.isNullOrEmpty() || seats.map { it.color } != state.players) return null
        val mode = runCatching { LudoMode.valueOf(prefs[SAVED_MODE] ?: "") }.getOrNull() ?: return null
        return SavedMatch(state, seats, mode)
    }

    suspend fun clearSavedMatch() {
        dataStore.edit {
            it.remove(SAVED_MATCH)
            it.remove(SAVED_SEATS)
            it.remove(SAVED_MODE)
        }
    }

    private fun encodeHistory(records: List<LudoMatchRecord>): String =
        records.joinToString("\n") { r ->
            "${r.timestamp}|${r.modeLabel}|${r.winner?.name ?: ""}|${r.humanWon?.toString() ?: ""}"
        }

    private fun decodeHistory(text: String?): List<LudoMatchRecord> {
        if (text.isNullOrBlank()) return emptyList()
        return text.lines().mapNotNull { line ->
            runCatching {
                val parts = line.split("|")
                LudoMatchRecord(
                    timestamp = parts[0].toLong(),
                    modeLabel = parts[1],
                    winner = parts[2].takeIf { it.isNotEmpty() }?.let { LudoColor.valueOf(it) },
                    humanWon = parts[3].takeIf { it.isNotEmpty() }?.toBoolean(),
                )
            }.getOrNull()
        }
    }

    private companion object {
        val REDUCED_EFFECTS = booleanPreferencesKey("ludo_reduced_effects")
        val BOT_DIFF = stringPreferencesKey("ludo_bot_diff")
        val QUICK_WIN = booleanPreferencesKey("ludo_quick_win")
        val THREE_SIXES = booleanPreferencesKey("ludo_three_sixes")
        val TUTORIAL_SEEN = booleanPreferencesKey("ludo_tutorial_seen")
        val STAT_PLAYED = intPreferencesKey("ludo_stat_played")
        val STAT_WON = intPreferencesKey("ludo_stat_won")
        val STAT_CAPTURES = intPreferencesKey("ludo_stat_captures")
        val STAT_HOME = intPreferencesKey("ludo_stat_home")
        val STAT_MOVES = intPreferencesKey("ludo_stat_moves")
        val HISTORY = stringPreferencesKey("ludo_history")
        val SAVED_MATCH = stringPreferencesKey("ludo_saved_match")
        val SAVED_SEATS = stringPreferencesKey("ludo_saved_seats")
        val SAVED_MODE = stringPreferencesKey("ludo_saved_mode")
        const val MAX_HISTORY = 15
    }
}
