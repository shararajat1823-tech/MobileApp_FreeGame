package com.miniplay.app.games.ludo

/**
 * Pure, versioned (de)serialization of a [LudoState] to a compact string, so an
 * in-progress local match can be saved to DataStore when the app is backgrounded
 * and restored exactly. Kept Android-free so round-tripping is unit-tested.
 */
object LudoSerialization {

    private const val VERSION = "v1"

    fun encode(state: LudoState): String {
        val players = state.players.joinToString(",") { it.name }
        val tokens = state.players.joinToString(";") { c ->
            "${c.name}:" + state.tokens.getValue(c).joinToString(",")
        }
        val r = state.rules
        val rules = listOf(
            if (r.enterOnSix) 1 else 0,
            if (r.extraTurnOnSix) 1 else 0,
            if (r.extraTurnOnCapture) 1 else 0,
            if (r.extraTurnOnFinish) 1 else 0,
            if (r.threeSixesForfeits) 1 else 0,
            r.tokensToWin,
        ).joinToString(",")
        return buildString {
            appendLine(VERSION)
            appendLine("players=$players")
            appendLine("tokens=$tokens")
            appendLine("current=${state.current}")
            appendLine("phase=${state.phase.name}")
            appendLine("dice=${state.dice ?: ""}")
            appendLine("sixes=${state.consecutiveSixes}")
            appendLine("winner=${state.winner?.name ?: ""}")
            appendLine("turn=${state.turnId}")
            appendLine("legal=${state.legalMoves.joinToString(",")}")
            append("rules=$rules")
        }
    }

    fun decode(text: String?): LudoState? {
        if (text.isNullOrBlank()) return null
        return runCatching {
            val lines = text.trim().lines()
            if (lines.firstOrNull() != VERSION) return null
            val map = lines.drop(1).mapNotNull { line ->
                val i = line.indexOf('=')
                if (i < 0) null else line.substring(0, i) to line.substring(i + 1)
            }.toMap()

            val players = map.getValue("players").split(",").filter { it.isNotEmpty() }.map { LudoColor.valueOf(it) }
            if (players.size !in 2..4) return null
            val tokens = map.getValue("tokens").split(";").associate { part ->
                val (name, values) = part.split(":")
                LudoColor.valueOf(name) to values.split(",").map { it.toInt() }
            }
            // Every declared player must have exactly four tokens.
            if (players.any { tokens[it]?.size != LudoBoard.TOKENS }) return null

            val rulesParts = map.getValue("rules").split(",").map { it.toInt() }
            val rules = LudoRules(
                enterOnSix = rulesParts[0] == 1,
                extraTurnOnSix = rulesParts[1] == 1,
                extraTurnOnCapture = rulesParts[2] == 1,
                extraTurnOnFinish = rulesParts[3] == 1,
                threeSixesForfeits = rulesParts[4] == 1,
                tokensToWin = rulesParts[5],
            )
            LudoState(
                players = players,
                tokens = tokens,
                current = map.getValue("current").toInt().coerceIn(0, players.size - 1),
                phase = LudoPhase.valueOf(map.getValue("phase")),
                dice = map["dice"]?.takeIf { it.isNotEmpty() }?.toInt(),
                consecutiveSixes = map["sixes"]?.toIntOrNull() ?: 0,
                winner = map["winner"]?.takeIf { it.isNotEmpty() }?.let { LudoColor.valueOf(it) },
                legalMoves = map["legal"]?.takeIf { it.isNotEmpty() }?.split(",")?.map { it.toInt() } ?: emptyList(),
                rules = rules,
                turnId = map["turn"]?.toIntOrNull() ?: 0,
            )
        }.getOrNull()
    }
}
