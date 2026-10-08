# Adding a new game

The hub discovers games through one abstraction, so adding a game is additive —
you create a new package and register it in exactly one place. Nothing in home,
games, profile, achievements or persistence needs to change.

## 1. Pick an id and strings

- Add a constant to `domain/model/GameIds.kt` (this string is a **persistence
  key** — never change it later).
- Add the game's strings to `res/values/strings.xml` (title, short description,
  "how to play").
- (Optional) give the game an accent in `core/ui/theme/ExtendedColors.kt`
  (`Accents.byId`); otherwise it falls back to the brand violet.

## 2. Create the package `games/<yourgame>/`

Mirror an existing game (Tic-Tac-Toe is the simplest reference). Create:

1. **`<Game>Engine.kt` — pure Kotlin, no Android imports.** All rules and state
   transitions live here as pure functions on immutable data. This is what you
   unit-test.
2. **`<Game>ViewModel.kt`** — a `ViewModel` exposing a `StateFlow<…UiState>`.
   It sequences input, calls the engine, triggers `SoundManager`/`HapticManager`,
   and on game end builds a `GameResult` and calls `recordGameResult(result)`
   exactly once (guard with a flag). Use the correct `GameIds` id, `score`,
   `won` (null for non-win/lose games), `difficulty`, `durationMillis`,
   `flawless`, and any `metrics`.
3. **`<Game>Screen.kt`** — `@Composable fun <Game>Screen(onExit: () -> Unit)`.
   Obtain the ViewModel with `rememberViewModel { container -> … }`, wrap the UI
   in `GameScaffold`, offer `HowToPlayDialog`, and show `GameResultOverlay` on
   completion. Read tokens from `MiniPlayTheme`/`MaterialTheme` — no hardcoded
   colors or dimensions.
4. **`<Game>Descriptor.kt`**:
   ```kotlin
   object YourGameDescriptor : GameDescriptor {
       override val metadata = GameMetadata(
           id = GameIds.YOUR_GAME,
           titleRes = R.string.yourgame_title,
           descriptionRes = R.string.yourgame_desc,
           howToPlayRes = R.string.yourgame_how_to,
           iconEmoji = "🎲",
           category = GameCategory.PUZZLE,
           difficulties = listOf(GameDifficulty.MEDIUM),
           scoreDirection = ScoreDirection.HIGHER_IS_BETTER, // or LOWER_IS_BETTER
           tracksHighScore = true,
           popularity = 50,
       )
       @Composable override fun Screen(onExit: () -> Unit) = YourGameScreen(onExit)
   }
   ```

## 3. Register it (the only wiring change)

Add the descriptor to the list in `di/AppContainer.kt`:

```kotlin
val gameRegistry = GameRegistry(
    listOf(
        …,
        YourGameDescriptor,
    ),
)
```

That's it. The game now appears on Home and in the catalogue, records stats and
best scores, feeds the streak and daily challenge, and can back achievements.

## 4. Test the engine

Add a unit test under `app/src/test/java/com/miniplay/app/` that drives the pure
engine (win/lose/scoring/edge cases). Because the engine has no Android deps, the
test runs on the plain JVM via `./gradlew testDebugUnitTest`.

## 5. (Optional) add achievements

Add an `AchievementDefinition` to `domain/usecase/AchievementCatalog.kt`. If it
needs a new signal, extend `AchievementCondition` + `AchievementSnapshot` and the
mapping in `RecordGameResultUseCase.buildSnapshot`.
