
# Quadrinheiro

<p align="center">
  <img src="branding/quadrinheiro-logo.svg" alt="Quadrinheiro logo" width="144" />
</p>

<p align="center">
  <strong>Your comics, your reading journey, your collection.</strong><br />
  An Android app for organizing comic books, tracking reading progress, and preserving the story of your collection.
</p>

<p align="center">
  <a href="https://github.com/felipemloiola/Quadrinheiro/releases/latest">Download APK</a> ·
  <a href="https://github.com/felipemloiola/Quadrinheiro/issues">Report an issue</a>
</p>

**Android 7.0+ · Kotlin · Jetpack Compose · Material 3**

## App preview

https://github.com/user-attachments/assets/651c7d4e-e995-4a12-a539-594bf86fcc77

The video highlights the real Home, Explore, and Profile screens, along with the comic library and reading features. The original in-app screenshots are shown in Portuguese; the promotional captions are in English.

Quadrinheiro pairs a comic-book library with reading goals, personalized discovery, profiles, and a community. Its interface uses translucent materials, cover-inspired lighting, and spring-based motion. The appearance can be customized for different color schemes and accessibility needs.

## Features

### Home — keep reading

- Pick up your current comic with page-by-page progress and quick reading controls.
- See annual reading goals, completed titles, and reading statistics.
- Jump back into recently added books and your personal library.
- Track reading activity through a chronological journal.

### Explore — discover your next read

- Browse editions and recommendations informed by your library, ratings, favorite publisher, and other preferences.
- Explore categories with paginated loading and duplicate-aware catalog results that preserve distinct editions.
- Switch between **Discover** and **Community** inside Explore.
- Read and share reviews, ratings, posts, and comments in the Community. A signed-in account is required for community participation.
- Recent discovery improvements avoid filling a personalized feed with generic showcase recommendations when user preferences or collection signals already exist.

### Profile and Library — make the collection yours

- Personalize your profile with a banner, avatar, biography, reading statistics, four favorite comics, and four favorite characters.
- Crop and reframe wide profile banners; saved profile artwork and favorites are associated with your account.
- Open **Library** from Profile, then switch between the **Shelf** and **Journal**.
- Browse the shelf in grid, list, or sequence layouts; sort, search, and filter by reading status and genre.
- Register comics manually and save tags, notes, ratings, reviews, and reading progress.
- Keep notes and progress when moving a comic between **Read**, **Reading**, and **Want to Read**.

### Catalog and comic details

- Edition metadata, page counts, credits, and covers from **Metron**, with **Comic Vine**, **Open Library**, **Google Books**, and **Mercado Livre** as supplementary sources.
- Character information supported by the **Superhero API**.
- Expandable synopses and a direct action to view a comic's source page.
- Original-language edition details are kept distinct from translated metadata. Missing publisher, synopsis, credits, or page counts are not fabricated.

### Personalization, reading, and updates

- Dark and light surfaces, accent and launcher-icon color options, adjustable text size, contrast, transparency, and reduced-motion preferences.
- Reading-progress notifications and optional Live Updates / Now Bar behavior on compatible Android devices.
- Home-screen widgets for current reading, recent comics, and goals, with theme-aware appearance.
- Shareable reading, completion, and goal cards.
- Google sign-in and per-account storage with Firebase synchronization.
- In-app APK download, verification, and handoff to the Android package installer.

**Recently improved:** personalized discovery behavior, theme-aware launcher icon previews, synchronization of launcher appearance with theme settings, and application of icon appearance changes after the app moves to the background.

## Installation

1. Open the [latest GitHub Release](https://github.com/felipemloiola/Quadrinheiro/releases/latest).
2. Download **`quadrinheiro.apk`**.
3. Open the APK on a device running **Android 7.0 (API 24) or later**.
4. If Android asks, allow installation from the app or browser used to open the APK, then confirm installation.

Future updates can be downloaded through Quadrinheiro once the version with the in-app updater is installed. Android still requires user confirmation before installing an update. The updater checks the package identity, version, size, SHA-256 hash, and signing certificate.

> **Privacy and compatibility:** your library and preferences are private to your account. Saving a rating or review may publish it to the Community. Lock-screen progress and Now Bar behavior vary by Android version, device, permissions, and privacy settings.

## Technology

| Area | Stack |
| --- | --- |
| Android app | Kotlin, Jetpack Compose, Material 3 |
| UI and motion | Translucent / Liquid Glass-inspired surfaces, backdrop effects, shaders, Compose springs |
| Images | Coil with bounded memory and disk caching |
| Account and data | Firebase Authentication, Cloud Firestore, account-scoped SafeStorage |
| Widgets | Jetpack Glance |
| Catalog | Cloudflare Worker with upstream catalog integrations and caching |
| Build / CI | Android Gradle Plugin 9.1.1, Gradle 9.3.1, JDK 21 |
| Tests | JUnit, Robolectric, Android instrumentation tests, Firebase emulators |

The visual language takes inspiration from Apple's Human Interface Guidelines, but it is implemented using Android APIs. It is **not** an official Apple UI implementation. The app uses Android's native sans-serif font rather than bundling Apple's proprietary SF Pro fonts.

## Build from source

Prerequisites: Android Studio, **JDK 21**, Android SDK **36.1**, and **Gradle 9.3.1**. The repository currently uses an installed Gradle executable rather than including a `gradlew` wrapper script.

1. Clone the repository and open it in Android Studio.
2. Configure your own Firebase project; place its Android configuration at `app/google-services.json` (ignored by Git).
3. Enable Google Sign-In if needed, configure the Web OAuth client, and register the correct signing certificate fingerprints.
4. Build a debug APK:

```sh
gradle :app:assembleDebug
```

The resulting APK is written to `app/build/outputs/apk/debug/`.

### Run tests

```sh
gradle :app:testDebugUnitTest
gradle :app:connectedDebugAndroidTest
```

Authentication and security-rule tests may require the Firebase emulators. Instrumentation tests require a connected Android device or emulator; device-specific Samsung features still need validation on supported hardware.

### Catalog service

`catalog-service/` contains the Cloudflare Worker that mediates catalog access. Upstream provider credentials belong in **server-side secrets**, not in the APK, client configuration, or source control.

```sh
cd catalog-service
npm ci
npm test
npx wrangler login
npx wrangler secret put COMIC_VINE_API_KEY
npx wrangler secret put SUPERHERO_API_KEY
npx wrangler secret put METRON_API_KEY
npx wrangler secret put APIFY_API_TOKEN
npm run deploy
```

For the release pipeline and APK signing checks, see [Release workflow](.github/workflows/release-apk.yml). For in-app updates, see [Update documentation](docs/ATUALIZACOES-NO-APP.md).

## Repository layout

```text
app/src/main/java/com/example/
├── data/          # Catalogs, persistence, translation, sync, caches
├── model/         # Models and preferences
├── reading/       # Reading session and notifications
├── ui/components/ # Reusable controls and surfaces
├── ui/screens/    # Screens and flows
├── ui/theme/      # Colors, typography, motion policy
├── updates/       # APK download and verification
├── viewmodel/     # App state
└── widget/        # Home-screen widgets

catalog-service/     # Cloudflare catalog service
docs/                # Project and development documentation
branding/            # Official Quadrinheiro logo
```

Storage tools account for overlapping application directories without double-counting. Recoverable caches and older temporary APKs can be cleaned without deleting libraries, accounts, or user backups. Comic covers and HTTP responses have separate size limits.

## References

- [UI and reading progress review](docs/REVISAO-HIG-NOW-BAR.md)
- [Typography guidelines and Android adaptation](docs/TIPOGRAFIA.md)
- [Apple Human Interface Guidelines](https://developer.apple.com/design/human-interface-guidelines)
- [AndroidLiquidGlass / Backdrop](https://github.com/Kyant0/AndroidLiquidGlass)
- [LiquidGlassKMP](https://github.com/philipplackner/LiquidGlassKMP)
- [Third-party notices](licenses/)

## Author and rights

Created by [Felipe Loiola](https://github.com/felipemloiola).

**All rights reserved.** Public source availability does not grant permission to redistribute the app or source code. Third-party dependencies and materials remain subject to their own licenses and terms.
