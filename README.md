<div align="center">

# 📚 Quadrinheiro

**A catalog, reading tracker and chronological diary for comics, graphic novels and manga.**

Liquid Glass design · Multi-API search · Cloud sync

![Android](https://img.shields.io/badge/Android-API%2024%2B-3DDC84?logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-Jetpack%20Compose-7F52FF?logo=kotlin&logoColor=white)
![Material 3](https://img.shields.io/badge/Material-3-6750A4?logo=materialdesign&logoColor=white)
![Firebase](https://img.shields.io/badge/Firebase-Auth%20%2B%20Firestore-FFCA28?logo=firebase&logoColor=black)
![Gemini](https://img.shields.io/badge/Gemini-API-4285F4?logo=googlegemini&logoColor=white)
![Version](https://img.shields.io/badge/version-1.0-blue)

</div>

---

## ✨ About

**Quadrinheiro** is a native Android app for comic lovers. It brings together your **personal shelf**, **reading progress** (current page, goals and stats) and a **chronological diary** with notes, ratings and tags for every title you read.

The interface follows a **Liquid Glass** visual style: translucent surfaces, soft layers and a modern, pleasant experience. Your data is synced to the cloud with secure Google sign-in.

## 🚀 Features

- **Personal catalog**: organize comics, graphic novels and manga in a cloud library.
- **Multi-API search**: find titles across several catalogs and import title, authors, publisher, ISBN, cover and synopsis.
- **Reading status**: `Want to read`, `Reading` and `Read`, with current page and total page tracking.
- **Chronological diary**: log the read date, rereads, a 0–5 star rating and a review of up to 5,000 characters.
- **Favorites and tags**: mark beloved titles and classify them with up to 30 review tags.
- **Reading goals**: set a yearly comics goal and a daily pages goal, and optionally show progress on the home screen.
- **Collector profile**: name, @handle, bio, favorite universe (Marvel, DC, manga, etc.) and customizable avatar.
- **Google sign-in**: authentication through Firebase Auth and Credential Manager.
- **Cloud sync**: private, per-user data in Cloud Firestore.

## 🧰 Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 (extended icons) |
| Images | Coil |
| Networking | OkHttp |
| Concurrency | Kotlin Coroutines |
| Authentication | Firebase Auth + Credential Manager + Google ID |
| Database | Cloud Firestore |
| AI | Gemini API (server-side) |
| Build | Gradle Kotlin DSL, KSP, Secrets Gradle Plugin |
| Testing | JUnit, Robolectric, Espresso, Compose UI Test |

**Android requirements:** `minSdk 24` · `targetSdk 36` · `compileSdk 36` · Java 11.

## 🗂️ Data Model

Data lives in Firestore, isolated per user:

```
/users/{userId}                    → UserProfile
/users/{userId}/comics/{comicId}   → CloudComic
```

**UserProfile**: `name`, `handle`, `bio`, `favoriteUniverse`, `avatarPreset`, `yearlyGoal`, `dailyPagesGoal`, `showGoalOnHome`, `updatedAt`.

**CloudComic**: `title`, `authors`, `publisher`, `genre`, `format`, `releaseYear`, `pageCount`, `currentPage`, `status` (`READ` | `READING` | `WANT_TO_READ`), `rating`, `favorite`, `rereadCount`, `tags`, `review`, `readDate`, `isbn`, `coverUrl`, `synopsis`, `sourceLabel`, `addedAt`, `updatedAt`.

The full schema is in [`firebase-blueprint.json`](firebase-blueprint.json).

## 🔒 Security

Firestore access rules are defined in [`firestore.rules`](firestore.rules). Each authenticated user can access only their own data, and fields are validated (size limits, types and allowed values).

## ⚙️ Getting Started

### Prerequisites

- Up-to-date Android Studio (with SDK 36 support)
- JDK 11 or later
- A [Firebase](https://console.firebase.google.com/) project with **Authentication (Google)** and **Firestore** enabled

### Setup

1. **Clone the repository**
   ```bash
   git clone https://github.com/felipemloiola/Quadrinheiro.git
   cd Quadrinheiro
   ```
2. **Configure Firebase**: download `google-services.json` from your project and place it in `app/`.
3. **Set environment variables**: copy the template and fill in your key.
   ```bash
   cp .env.example .env
   ```
   ```env
   GEMINI_API_KEY=your_key_here
   ```
4. **Deploy the Firestore rules**
   ```bash
   firebase deploy --only firestore:rules
   ```
5. **Build and install**
   ```bash
   ./gradlew assembleDebug
   ```
   Or open the project in Android Studio and click **Run ▶**.

### Tests

```bash
./gradlew test
```

> ⚠️ **Never** commit your `.env` file, keystores or passwords. The `.gitignore` already covers the most common sensitive files.

## 📁 Project Structure

```
Quadrinheiro/
├── app/                      # Android module (Kotlin code, resources, manifest)
├── gradle/                   # Version catalog and wrapper
├── .env.example              # Environment variable template
├── build.gradle.kts          # Root Gradle configuration
├── settings.gradle.kts
├── firebase.json             # Firebase configuration
├── firebase-blueprint.json   # Firestore entity schema
├── firestore.rules           # Security rules
└── metadata.json             # App metadata
```

## 🗺️ Roadmap

- [ ] Yearly reading statistics and charts
- [ ] Library export and import
- [ ] Custom lists and collections
- [ ] AI-powered recommendations (Gemini)
- [ ] Improved offline mode

## 🤝 Contributing

Suggestions and improvements are welcome!

1. Fork the project
2. Create a branch: `git checkout -b feature/my-idea`
3. Commit your changes: `git commit -m "feat: my idea"`
4. Push: `git push origin feature/my-idea`
5. Open a Pull Request

## 👤 Author

**Felipe Loiola** · [@felipemloiola](https://github.com/felipemloiola)

---

<div align="center">

Made with ❤️ for comic book lovers.

</div>
