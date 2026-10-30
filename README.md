# Quadrinheiro

![Quadrinheiro Logo](https://github.com/felipemloiola/Quadrinheiro/raw/main/app/src/main/res/mipmap-xxxhdpi/ic_launcher.png)

> A neo-comic Liquid Glass library and reading diary for comics, graphic novels and manga.

Quadrinheiro is an Android app for keeping track of the comics you own and read. You build a personal library, log your progress page by page, and keep a dated diary of everything you finish, with a rating, tags and a written review.

The interface uses a dark, translucent **Liquid Glass** look inspired by modern iOS, and your data is saved to your account in the cloud so your library follows you across devices.

![Android 7.0+](https://img.shields.io/badge/Android-7.0%2B-3DDC84?logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-Jetpack%20Compose-7F52FF?logo=kotlin&logoColor=white)
![Firebase](https://img.shields.io/badge/Firebase-Auth%20%2B%20Firestore-FFCA28?logo=firebase&logoColor=black)

---

## Overview

Quadrinheiro combines a personal comics catalog with a detailed reading diary:

- Save every edition you own or discover.
- Track progress page by page.
- Log ratings, rereads, tags and long-form reviews.
- Set yearly and daily goals to keep your reading habit visible.

Whether your shelf is full of superhero runs, indie graphic novels or long-running manga, Quadrinheiro keeps everything organized in one place.

---

## Features

- **Library:**
  - Save titles to your shelf as **Want to read**, **Reading** or **Read**.
  - See recent additions, series sequences and status at a glance.

- **Search:**
  - Look up books across more than one catalog API.
  - Import title, authors, publisher, ISBN, cover and synopsis with a single tap.

- **Progress:**
  - Record the current page against the total page count.
  - Use native stepper controls to update progress quickly.

- **Diary:**
  - Keep the date you finished each book.
  - Track how many times you reread it.
  - Add a 0–5 star rating, up to 30 tags and a review of up to 5,000 characters.

- **Favorites & Ratings:**
  - Flag the titles you love.
  - Browse based on rating and status.

- **Goals:**
  - Set a yearly reading goal and a daily pages goal.
  - Choose whether progress surfaces on the home screen.

- **Profile:**
  - Configure display name, @handle, bio and favorite universe or publisher.
  - Pick avatar and appearance presets.

- **Accounts:**
  - Sign in with Google.
  - Each user's data is stored separately in Firestore.

---

## Design & Identity

Quadrinheiro is designed as a **neo-comic Liquid Glass** experience:

- **Liquid Glass UI:**
  - Dark, translucent surfaces with soft highlights and edge refraction.
  - Cards and toolbars float over content, mimicking modern iOS aesthetics.

- **Origami Q Logo (in-app icon):**
  - Implemented as a VectorDrawable in [`ic_launcher_foreground.xml`](app/src/main/res/drawable/ic_launcher_foreground.xml).
  - A folded comics page in origami form, built from irregular panels with gradient fills:
    - Left panel: **carmine red → neon pink**.
    - Top panel: **golden yellow → vibrant orange**.
    - Lower tail panel: **royal blue → electric cyan**, extending beyond the square to form the stylized "Q" tail.
  - Sarjetas (gutters) and faceted highlights suggest depth and motion, creating a dynamic negative-space **"Q"**.

- **Reading Experience:**
  - Responsive layouts for recent reads, sequences and diary.
  - Subtle motion and glass effects tuned to keep the app smooth on real devices.

---

## Tech Stack

| Area | Tools |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose, Material 3 |
| Images | Coil |
| Networking | OkHttp |
| Async | Kotlin Coroutines |
| Auth | Firebase Auth, Credential Manager, Google ID |
| Database | Cloud Firestore |
| AI | Gemini API (server-side) |
| Build | Gradle (Kotlin DSL), KSP, Secrets Gradle Plugin |
| Tests | JUnit, Robolectric, Espresso, Compose UI Test |

The app targets **Android 7.0 (API 24)** and up, compiles against **API 36**, and uses **Java 11**.

---

## Data Model

Firestore holds two kinds of documents, both under the signed-in user:

```text
/users/{userId}                    UserProfile
/users/{userId}/comics/{comicId}   CloudComic
```

`UserProfile` stores the name, handle, bio, favorite universe, avatar preset, the two goals and the home-screen toggle.

`CloudComic` stores the bibliographic data (title, authors, publisher, genre, format, year, ISBN, cover, synopsis), the reading state (status, current page, page count, rating, favorite, rereads, read date) and the user's own tags and review.

The full schema is in [`firebase-blueprint.json`](firebase-blueprint.json), and the access rules are in [`firestore.rules`](firestore.rules).

---

## Running It Locally

You need Android Studio with SDK 36, JDK 11 or newer, and a Firebase project with Google sign-in and Firestore turned on.

```bash
git clone https://github.com/felipemloiola/Quadrinheiro.git
cd Quadrinheiro
cp .env.example .env
```

1. Download `google-services.json` from your Firebase project and put it in `app/`.
2. Open `.env` and set `GEMINI_API_KEY`.
3. Deploy the security rules with `firebase deploy --only firestore:rules`.
4. Build with `./gradlew assembleDebug`, or press Run in Android Studio.

Run the unit tests with:

```bash
./gradlew test
```

Keep `.env`, keystores and passwords out of version control.

---

## Repository Layout

```text
app/                      Android module: Kotlin code, resources, manifest
gradle/                   Version catalog and wrapper
.env.example              Template for local secrets
firebase.json             Firebase configuration
firebase-blueprint.json   Firestore entity schema
firestore.rules           Firestore security rules
metadata.json             App metadata
```

---

## Contributions

This is a personal project and it is not open to outside contributions. Pull requests and unsolicited code changes will not be merged. If you find a problem, you are welcome to open an issue.

---

## License

All rights reserved. The source is public for reference only, and no permission is granted to copy, modify or redistribute it without written consent from the author.

---

## Author

**Felipe Loiola** ([@felipemloiola](https://github.com/felipemloiola))
