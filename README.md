# Quadrinheiro

Quadrinheiro is an Android app for keeping track of the comics, graphic novels and manga you own and read. You build a personal library, log your progress page by page, and keep a dated diary of everything you finish, with a rating, tags and a written review.

The interface uses a translucent "Liquid Glass" look, and everything is saved to your account in the cloud, so your library follows you across devices.

![Android 7.0+](https://img.shields.io/badge/Android-7.0%2B-3DDC84?logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-Jetpack%20Compose-7F52FF?logo=kotlin&logoColor=white)
![Firebase](https://img.shields.io/badge/Firebase-Auth%20%2B%20Firestore-FFCA28?logo=firebase&logoColor=black)

## What it does

- **Library:** save titles to your shelf as *Want to read*, *Reading* or *Read*.
- **Search:** look up books through more than one catalog API and import the title, authors, publisher, ISBN, cover and synopsis.
- **Progress:** record the current page against the total page count.
- **Diary:** keep the date you finished each book, how many times you reread it, a 0 to 5 star rating, up to 30 tags and a review of up to 5,000 characters.
- **Favorites:** flag the titles you love.
- **Goals:** set a yearly reading goal and a daily pages goal, and choose whether progress shows on the home screen.
- **Profile:** display name, @handle, bio, favorite publisher or universe, and an avatar color.
- **Accounts:** sign in with Google. Each user's data is stored separately.

## Built with

| Area | Tools |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose, Material 3 |
| Images | Coil |
| Networking | OkHttp |
| Async | Kotlin Coroutines |
| Auth | Firebase Auth, Credential Manager, Google ID |
| Database | Cloud Firestore |
| AI | Gemini API, called server-side |
| Build | Gradle (Kotlin DSL), KSP, Secrets Gradle Plugin |
| Tests | JUnit, Robolectric, Espresso, Compose UI Test |

The app targets Android 7.0 (API 24) and up, compiles against API 36, and uses Java 11.

## Data model

Firestore holds two kinds of documents, both under the signed-in user:

```
/users/{userId}                    UserProfile
/users/{userId}/comics/{comicId}   CloudComic
```

`UserProfile` stores the name, handle, bio, favorite universe, avatar preset, the two goals and the home-screen toggle.

`CloudComic` stores the bibliographic data (title, authors, publisher, genre, format, year, ISBN, cover, synopsis), the reading state (status, current page, page count, rating, favorite, rereads, read date) and the user's own tags and review.

The full schema is in [`firebase-blueprint.json`](firebase-blueprint.json), and the access rules are in [`firestore.rules`](firestore.rules).

## Running it locally

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

Run the unit tests with `./gradlew test`.

Keep `.env`, keystores and passwords out of version control.

## Repository layout

```
app/                      Android module: Kotlin code, resources, manifest
gradle/                   Version catalog and wrapper
.env.example              Template for local secrets
firebase.json             Firebase configuration
firebase-blueprint.json   Firestore entity schema
firestore.rules           Firestore security rules
metadata.json             App metadata
```

## Contributions

This is a personal project and it is not open to outside contributions. Pull requests and unsolicited code changes will not be merged. If you find a problem, you are welcome to open an issue.

## License

All rights reserved. The source is public for reference only, and no permission is granted to copy, modify or redistribute it without written consent from the author.

## Author

Felipe Loiola ([@felipemloiola](https://github.com/felipemloiola))
