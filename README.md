# Quadrinheiro

<p align="left">
  <img src="https://github.com/felipemloiola/Quadrinheiro/raw/main/branding/quadrinheiro-logo.svg" alt="Quadrinheiro Logo" width="120" />
</p>

> The definitive ecosystem for organizing, cataloging, and tracking your collection of comics, graphic novels, and manga with a modern Liquid Glass design, cloud synchronization, and multi-source search intelligence.

**Quadrinheiro** is a native Android application built with Jetpack Compose. It features a translucent, tactile aesthetic inspired by modern iOS design, backed by GPU hardware acceleration, single-pass rendering pipelines, and custom AGSL shaders.

![Android 7.0+](https://img.shields.io/badge/Android-7.0%2B-3DDC84?logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-Jetpack%20Compose-7F52FF?logo=kotlin&logoColor=white)
![Firebase](https://img.shields.io/badge/Firebase-Auth%20%2B%20Firestore-FFCA28?logo=firebase&logoColor=black)
![License](https://img.shields.io/badge/License-All%20Rights%20Reserved-red)

---

## App Highlights

- **Apple-Style Welcome & Onboarding Experience:**
  - Distinctive first-launch flow designed with strict vertical symmetry and spring physics transitions.
  - Interactive hero brand mark with matrix transform scaling (`withTransform`) introducing the app's four foundational pillars: *Personal Library*, *Sequences & Recommendations*, *Reading Goals & Diary*, and *Online Multi-API Cloud Search*.

- **Liquid Glass & Optical Material Design:**
  - Translucent surfaces with edge refraction, glass menisci, subtle chromatic aberration, and progressive backdrop blurring powered by AGSL shaders.
  - Atmospheric ambient background lighting that extracts vibrant dominant colors directly from the active comic's cover in real time.
  - Dynamic palette switching with eight curated themes (Ocean Blue, Carmine Red, Emerald Green, Imperial Purple, Sunset Orange, Neon Pink, Cyber Teal, and Obsidian/Silver).

- **Comprehensive Library & Shelf Management:**
  - Real-time reading status: **Reading**, **Read**, and **Want to Read (Queue)**.
  - Flexible browsing modes: Responsive Grid (2, 3, or 4 columns), Compact List, or Chronological Series Sequences (#1, #2, #3...).
  - Interactive 3D spine effects, star ratings, and non-blocking deferred reading progress bars over cover art.

- **Real-Time Multi-API Search & Auto-Localization:**
  - Unified search engine querying Google Books, MangaUpdates, and international catalog APIs simultaneously.
  - Automatic metadata enrichment with Portuguese (PT-BR) title and synopsis translations backed by persistent local caching.
  - One-tap shelf additions with cover pre-fetching or detailed manual entries.

- **Reading Goals & Chronological Diary:**
  - Apple Health-inspired activity rings displaying annual book completion targets and daily page goals.
  - Chronological reading diary recording finish dates, reread counts, star ratings (with half-star support), quick review tags, and in-depth reviews of up to 5,000 characters.

- **Hybrid Offline-First Authentication & Cloud Sync:**
  - Seamless Sign-In with Google (via Android Credential Manager + Firebase Auth), salted SHA-256 local email accounts, and a 100% isolated, full-featured Guest Mode.
  - Background synchronization via Cloud Firestore with conflict resolution, dirty-state tracking, and standalone backup export (ZIP/HTML).

---

## Architecture & Performance Engineering

| Layer / Concern | Technical Stack & Implementation |
|---|---|
| **Language & UI** | Kotlin 2.1+, Jetpack Compose, Material 3, Backdrop AGSL Shaders |
| **Graphics & Acceleration** | Explicit hardware acceleration (`android:hardwareAccelerated="true"`), `largeHeap="true"` for high-res cover buffers, single-pass `recordLayer` capture |
| **Image Pipeline** | Coil with direct GPU VRAM `HardwareBuffer` allocations, L1 memory caching, and disk caching |
| **Local Storage** | Multi-partition `SafeStorage` with isolated state per account/guest and self-contained HTML/ZIP backup generation |
| **Cloud & Identity** | Firebase Authentication, Google Identity Credential Manager, Cloud Firestore |
| **CI/CD Quality Gate** | GitHub Actions with Android API 34 emulators, touch delivery regression tests, and pixel-equivalence validations |

---

## Repository Structure

```text
├── app/
│   ├── src/main/java/com/example/
│   │   ├── MainActivity.kt                  # Entry point, Coil initialization, and top-level navigation
│   │   ├── data/                            # SafeStorage, AuthManager, CloudRepository, and Search Engine
│   │   ├── model/                           # Immutable models, AppSettings, and UI state wrappers
│   │   ├── ui/
│   │   │   ├── components/                  # QuadrinheiroLogo, LiquidGlass, BookCover, and UI controls
│   │   │   ├── screens/                     # WelcomeIntroScreen, AuthScreen, MainTabs, and Settings
│   │   │   └── theme/                       # Liquid Glass themes, Apple color palettes, and typography
│   │   └── viewmodel/                       # QuadrinheiroViewModel and reactive state orchestration
│   └── src/androidTest/                     # Pixel equivalence and touch regression instrumentation tests
├── .github/
│   ├── workflows/                           # CI/CD pipelines and automated APK release workflows
│   └── scripts/                             # Verified optimization scripts and audit trails
├── firestore.rules                          # Cloud Firestore security rules
└── README.md
```

---

## Local Development & Setup

### Prerequisites
- Android Studio Ladybug (or newer)
- Android SDK 36 & JDK 21 (Temurin recommended)
- A Firebase project configured with Google Sign-In and Cloud Firestore enabled

### Building the Project

```bash
# Clone the repository
git clone https://github.com/felipemloiola/Quadrinheiro.git
cd Quadrinheiro

# Place your Firebase configuration file
cp /path/to/your/google-services.json app/google-services.json

# Build the Debug APK
./gradlew assembleDebug

# Run unit tests
./gradlew testDebugUnitTest
```

---

## License

All rights reserved. This source code is made publicly visible for portfolio demonstration and technical reference only. No permission is granted to reproduce, modify, or distribute any part of this software without prior written consent from the author.

---

## Author

**Felipe Loiola** ([@felipemloiola](https://github.com/felipemloiola))
