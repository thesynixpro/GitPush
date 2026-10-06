# GitPush — Push files directly to GitHub from Android

Native Android app (Kotlin + Jetpack Compose) that pushes files and entire folder
structures to a GitHub repository using only a **GitHub Personal Access Token (PAT)**
and the **GitHub REST API over HTTPS**. No Termux, computer, Git CLI, SSH, or backend.

> Your files are sent directly from your device to GitHub.

## First-run flow

```
Splash → Select Folder to Push → Review Files → Repo / PAT Config → Confirm → Progress → Success
```

If configuration is already saved: `Splash → Select Folder → Review → Push`.

## Features

- Animated splash + dark-first futuristic UI (glass cards, neon accent, lightweight animations)
- SAF folder picker (`ACTION_OPEN_DOCUMENT_TREE`), recursive scan, Unicode/Bengali/space filenames
- Review screen: expand/collapse, select/deselect files & folders, search, refresh, counts + size + tree preview
- PAT setup: username, token (show/hide), owner, repo, branch, dest path, Test Connection, Save
- Keystore-backed `EncryptedSharedPreferences` — token never logged, never in plain prefs, HTTPS only
- Repo verification via API (exists, branch exists, push permission)
- Push via **Git Data API as a single commit** (blobs → tree → commit → ref update); per-file fallback ready
- Existing-file policy: Ask / Replace / Skip (per-file dialog + apply-to-all)
- Progress: %, bar, done/remaining, current file/folder, speed, ETA, failed count; cancel + retry
- Offline detection, safe stop, retry/cancel; history (Room, on-device only) with SHA, date, status
- Settings: push defaults, appearance (dark/light/system + accent), security (clear PAT/history), about
- Low-RAM design: streaming base64, sequential blobs, virtualized lists, coroutines off main thread
- Permissions: `INTERNET` + `ACCESS_NETWORK_STATE` only

## PAT permissions

- Fine-grained PAT: repository access + **read metadata / write contents** on the target repo.
- Classic PAT: `repo` scope.
- Nothing more is requested.

## Project structure

```
app/src/main/java/com/aprax/gitpush/
  MainActivity.kt, GitPushApplication.kt
  model/       ScannedFile, PushRequest, PushProgress, PushOutcome…
  storage/     SecureCredentialStore, AppPreferences, HistoryDatabase
  network/     NetworkMonitor
  github/      GitHubApi, GitHubModels, GitHubRepository
  scanner/     FolderScanner (SAF, iterative DFS)
  util/        FormatUtils, ErrorMapper
  viewmodel/   PushSessionViewModel, SettingsViewModel
  ui/theme/      GitPushTheme, accents
  ui/components/ GlassCard, GradientButton, StatusBadge, NeonProgress
  ui/navigation/ Routes
  ui/screens/    Splash, Home, Review, RepoConfig, PushFlow (Confirm/Progress/Success), History, Settings
```

## Build

Requirements: Android Studio Hedgehog+, JDK 17, Android SDK 34.

```bash
cd GitPush
./gradlew :app:assembleDebug
# install on device / emulator and open GitPush
```

Create a PAT at GitHub → Settings → Developer settings → Personal access tokens,
then in the app: Review → GitHub settings → paste token → Test Connection → Save.

## Security notes

- Token stored only in Keystore-backed encrypted prefs; never logged or shown after save.
- All GitHub calls go to `https://api.github.com` with `Authorization: Bearer`.
- No analytics, no intermediary server, history never leaves the device.
- Error messages are human-readable; raw stack traces hidden (technical `HTTP xxx` available on demand).

## License

MIT — see app About screen. GitHub API™ belongs to GitHub, Inc.
