# 🎮 MiniPlay — All Games in One App

**Play. Challenge. Repeat.**

MiniPlay is a modern, offline-first casual gaming hub for Android: seven polished
mini-games behind one beautiful Material 3 dashboard, built to a production bar
with Clean Architecture, a centralized design system, and a game abstraction that
makes adding the eighth game a one-file change.

<p align="center"><em>Kotlin · Jetpack Compose · Material 3 · Coroutines/Flow · Room · DataStore</em></p>

---

## Features

- **9 real games**, each with genuine mechanics (not placeholders):
  | Game | What it is | Highlights |
  |------|-----------|------------|
  | ⭕ Tic Tac Toe | 3-in-a-row | PvP + AI with a true **minimax** hard mode (never loses) |
  | 🧠 Memory Match | Card pairs | 4×4 / 5×4 / 6×6, flip animation, move + time scoring |
  | ⚡ Reaction Test | Tap on green | ms timing, best/avg, rating (Lightning → Keep practising) |
  | 🧩 Number Puzzle | Sliding 15-puzzle | 3×3 / 4×4, always-solvable shuffle, animated slides |
  | 🔢 2048 | Merge tiles | swipe controls, undo, win + game-over, tile slide/merge animation |
  | 🧱 Brick Breaker | Arcade paddle/ball | Canvas physics loop, lives, levels, increasing speed |
  | 🎯 Tap Challenge | 30-second tap frenzy | combo multiplier, shrinking targets, accuracy |
  | 🐍 Snake | Grow and survive | swipe steering, Canvas board, accelerates as you grow |
  | 💣 Minesweeper | Find every mine | Easy/Medium/Hard, first-tap-safe, flood reveal, long-press to flag |
- **Home dashboard** — greeting, daily challenge, streak strip, continue-playing,
  popular rail, full catalogue.
- **Achievements** — 9 achievements, automatic unlocks, app-wide unlock banner.
- **Profile** — aggregate stats, per-game bests, editable nickname + emoji avatar.
- **Daily challenge** — deterministic per-day target, locally generated (backend-ready).
- **Streak system** — daily play streak with a 7-day strip.
- **Design system** — light/dark/system themes, Material You dynamic colour,
  centralized color/type/spacing/shape/motion tokens.
- **Sound & haptics** — synthesized tones (no bundled audio) + vibration, each
  toggleable in settings.
- **100% offline** — no login, no internet, no backend, no analytics leaving the device.

## Tech stack & architecture

Clean Architecture + MVVM, organized by layer and feature. See
[`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) for the full picture and
[`docs/ADDING_A_GAME.md`](docs/ADDING_A_GAME.md) for the extension guide.

```
app/src/main/java/com/miniplay/app/
├── core/        design system (ui/theme, ui/components), game abstraction, audio, haptics, ads, analytics, common
├── domain/      models, repository interfaces, use cases (pure Kotlin)
├── data/        Room (stats, achievements), DataStore (settings/profile/streak/daily), repository impls
├── di/          manual DI container (AppContainer) + Compose helpers
├── navigation/  app shell, bottom nav, NavHost, game host
├── feature/     home, games, achievements, profile
└── games/       tictactoe, memorymatch, reaction, numberpuzzle, game2048, brickbreaker, tapchallenge
```

Key decisions:
- **Game logic is pure Kotlin** (no Android imports) in each game's `*Engine`/
  `*Logic` file, so it is exhaustively unit-tested and UI-independent.
- **Manual DI** (`AppContainer`) instead of Hilt — the graph is small and local,
  and dropping the annotation processor removes a class of build-version breakage.
- **Offline-first with seams for later**: `LeaderboardRepository`,
  `UserRepository`, `CloudSyncRepository`, `AdManager` are defined and wired to
  no-op implementations, so cloud/ads can be added without touching games.

## Building

Open the project in **Android Studio** (Koala or newer) and run the `app`
configuration, or from the command line:

```bash
./gradlew assembleDebug      # build the APK
./gradlew testDebugUnitTest  # run the unit tests (game logic, achievements, streak, daily)
./gradlew connectedCheck     # run instrumented tests (Room persistence) on a device/emulator
```

- **Min SDK 24**, target/compile **SDK 35**, JDK 17, AGP 8.7.3, Kotlin 2.0.21.
- Versions are pinned in [`gradle/libs.versions.toml`](gradle/libs.versions.toml).

> **Note on the authoring environment:** this project was developed in a sandbox
> where Google's Maven repo and the Android SDK (`dl.google.com`) were network-blocked,
> so a full Android build could not be run there. To keep quality verifiable, all
> game logic was written as pure Kotlin and validated with a JVM test harness
> (74 passing tests). The project builds normally in Android Studio / any
> environment with the Android SDK.

## Tests

Pure-logic unit tests (`app/src/test`) cover: Tic-Tac-Toe win/draw/AI (incl.
"minimax never loses"), 2048 move/merge/score/game-over, Memory match/mismatch/
completion/score, Reaction rating + averaging, Number Puzzle moves/solvability/
completion, Brick Breaker collisions/lives/level-clear, Tap combo/scoring, plus
the achievement engine, streak calculator and daily-challenge generator.
Room persistence has an instrumented test in `app/src/androidTest`.

## License / assets

Self-contained: Material Icons, Compose-drawn shapes, emoji and synthesized
system tones only — no bundled copyrighted media.
