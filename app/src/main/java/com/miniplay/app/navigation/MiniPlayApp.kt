package com.miniplay.app.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.miniplay.app.core.ui.theme.Motion
import com.miniplay.app.feature.achievements.AchievementUnlockHost
import com.miniplay.app.feature.achievements.AchievementsScreen
import com.miniplay.app.feature.games.GamesScreen
import com.miniplay.app.feature.home.HomeScreen
import com.miniplay.app.feature.profile.ProfileScreen

/**
 * App shell: a [Scaffold] with the bottom navigation bar (shown only on the four
 * top-level tabs) wrapping the [NavHost]. Game screens render full-screen with a
 * slide-up transition and no bottom bar.
 */
@Composable
fun MiniPlayApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute in TopLevelDestination.routes

    fun openGame(gameId: String) = navController.navigate(Routes.gameRoute(gameId))

    fun navigateTab(destination: TopLevelDestination) {
        navController.navigate(destination.route) {
            popUpTo(Routes.HOME) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    Box(Modifier.fillMaxSize()) {
    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                MiniPlayBottomBar(currentRoute = currentRoute, onNavigate = ::navigateTab)
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(if (showBottomBar) innerPadding else androidx.compose.foundation.layout.PaddingValues(0.dp)),
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    onOpenGame = ::openGame,
                    onSeeAllGames = { navigateTab(TopLevelDestination.GAMES) },
                )
            }
            composable(Routes.GAMES) {
                GamesScreen(onOpenGame = ::openGame)
            }
            composable(Routes.ACHIEVEMENTS) {
                AchievementsScreen()
            }
            composable(Routes.PROFILE) {
                ProfileScreen()
            }
            composable(
                route = Routes.GAME_PATTERN,
                arguments = listOf(navArgument(Routes.GAME_ARG) { type = NavType.StringType }),
                enterTransition = {
                    slideInVertically(tween(Motion.Duration.medium)) { it / 6 } + fadeIn(tween(Motion.Duration.medium))
                },
                exitTransition = { fadeOut(tween(Motion.Duration.fast)) },
                popExitTransition = {
                    slideOutVertically(tween(Motion.Duration.medium)) { it / 6 } + fadeOut(tween(Motion.Duration.medium))
                },
            ) { entry ->
                val gameId = entry.arguments?.getString(Routes.GAME_ARG).orEmpty()
                GameHost(gameId = gameId, onExit = { navController.popBackStack() })
            }
        }
    }
        // App-wide achievement unlock banner, above everything.
        AchievementUnlockHost(Modifier.align(Alignment.TopCenter))
    }
}
