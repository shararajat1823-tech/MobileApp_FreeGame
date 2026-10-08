# Architecture

MiniPlay follows **Clean Architecture + MVVM**, with a strict dependency rule:
`presentation → domain ← data`. The `domain` layer is pure Kotlin and never
depends on Android, Compose, Room or DataStore.

## Layers

### `domain/` (pure Kotlin)
- **model/** — `GameMetadata`, `GameResult`, `GameStats`, `Achievement*`,
  `DailyChallenge`, `StreakInfo`, `PlayerProfile`, `AppSettings`, `GameIds`, …
- **repository/** — interfaces only: `GameStatsRepository`,
  `AchievementRepository`, `ProfileRepository`, `SettingsRepository`,
  `StreakRepository`, `DailyChallengeRepository`, `GameFactsRepository`,
  `GameCatalog`, plus the disabled-for-now `LeaderboardRepository`,
  `UserRepository`, `CloudSyncRepository`, `RemoteAnalyticsSink`.
- **usecase/** — `RecordGameResultUseCase` (the single funnel every game ends
  through), `AchievementEngine`, `AchievementCatalog`, `StreakCalculator`,
  `DailyChallengeGenerator`.

### `data/`
- **local/** — Room (`MiniPlayDatabase`, `GameStatsDao`, `AchievementStateDao`,
  entities) for list-shaped state; a single preferences **DataStore**
  (`PreferencesStore`) for settings, profile, streak, daily progress and
  achievement facts.
- **repository/** — implementations of every domain interface, plus
  `DisabledOnlineRepositories` (no-op leaderboard/user/cloud/analytics).

### `core/`
- **ui/theme/** — the design system: color tokens (`Palette`, `MiniPlayColors`,
  `Accents`), `Typography`, `Spacing`, `Shape`, `Motion`, and `MiniPlayTheme`.
- **ui/components/** — the shared UI kit (cards, buttons, chips, `GameScaffold`,
  `GameResultOverlay`, `HowToPlayDialog`, `DifficultySelector`, states, …).
- **game/** — the game abstraction: `GameDescriptor` + `GameRegistry` (which also
  implements the domain `GameCatalog`).
- **audio/**, **haptics/**, **ads/**, **analytics/**, **common/** — platform
  services behind interfaces (`SoundManager`, `HapticManager`, `AdManager`,
  `Analytics`, `Clock`).

### `di/`
- `AppContainer` — the manual DI graph (singletons, wiring, settings→services
  sync, `resetAllProgress`). Exposed to Compose via `LocalAppContainer`;
  ViewModels are built with the `rememberViewModel { container -> … }` helper.

### `navigation/` & `feature/`
- `MiniPlayApp` hosts the bottom-nav `Scaffold` + `NavHost` and the app-wide
  achievement unlock banner. `feature/{home,games,achievements,profile}` each
  hold a ViewModel + screen(s).

### `games/<game>/`
Each game is a self-contained package: a **pure engine** (`*Engine`/`*Logic`/
`*World`), a **ViewModel + UI state**, a **Screen**, and a **Descriptor**.

## Data flow (playing a game)

```
Game Screen ──user input──▶ Game ViewModel ──▶ pure Engine (new immutable state)
      ▲                              │
      └────────── StateFlow ◀────────┘
                                     │ on game end
                                     ▼
                         RecordGameResultUseCase(GameResult)
                                     │
      ┌──────────────┬──────────────┼───────────────┬────────────────┐
      ▼              ▼              ▼                ▼                ▼
 GameStats      Streak        DailyChallenge   GameFacts      AchievementEngine
 (Room)        (DataStore)     (DataStore)     (DataStore)    → AchievementRepo (Room)
```

Every repository read is a `Flow`, so the dashboard, profile and achievements
update live the moment a result is recorded.

## Why these choices

- **Pure engines** make the hardest, bug-prone logic (minimax, 2048 merges,
  15-puzzle solvability, collisions) fully unit-testable with zero Android deps.
- **Manual DI** keeps the build free of annotation processors beyond Room's KSP.
- **Offline-first with online seams**: the online interfaces exist and are wired
  to no-ops, so adding a backend is an implementation swap, not a refactor.
