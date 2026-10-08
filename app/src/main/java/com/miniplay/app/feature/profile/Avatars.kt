package com.miniplay.app.feature.profile

/** Built-in emoji avatar set (offline, no uploads). Indexed by avatarId. */
object Avatars {
    val emojis: List<String> = listOf(
        "🦊", "🐼", "🐧", "🦄", "🐸", "🐙", "🦁", "🐨",
        "🐳", "🦉", "🐝", "🦖", "🤖", "👾", "🎮", "⭐",
    )

    fun emojiFor(id: Int): String = emojis.getOrElse(id) { emojis.first() }
}
