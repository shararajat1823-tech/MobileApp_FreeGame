package com.miniplay.app.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.ui.graphics.vector.ImageVector
import com.miniplay.app.R

/** Route constants. Game routes are built with [gameRoute]. */
object Routes {
    const val HOME = "home"
    const val GAMES = "games"
    const val ACHIEVEMENTS = "achievements"
    const val PROFILE = "profile"

    const val GAME_ARG = "gameId"
    const val GAME_PATTERN = "game/{$GAME_ARG}"
    fun gameRoute(gameId: String) = "game/$gameId"
}

/** The four bottom-navigation tabs. */
enum class TopLevelDestination(
    val route: String,
    @param:StringRes val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
) {
    HOME(Routes.HOME, R.string.nav_home, Icons.Rounded.Home, Icons.Outlined.Home),
    GAMES(Routes.GAMES, R.string.nav_games, Icons.Rounded.SportsEsports, Icons.Outlined.SportsEsports),
    ACHIEVEMENTS(Routes.ACHIEVEMENTS, R.string.nav_achievements, Icons.Rounded.EmojiEvents, Icons.Outlined.EmojiEvents),
    PROFILE(Routes.PROFILE, R.string.nav_profile, Icons.Rounded.Person, Icons.Outlined.Person);

    companion object {
        val routes: Set<String> = entries.map { it.route }.toSet()
    }
}
