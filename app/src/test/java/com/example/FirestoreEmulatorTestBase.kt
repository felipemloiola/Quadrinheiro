package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.MemoryCacheSettings
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@LooperMode(LooperMode.Mode.INSTRUMENTATION_TEST)
abstract class FirestoreEmulatorTestBase {
    lateinit var db: FirebaseFirestore
    lateinit var auth: FirebaseAuth
    lateinit var context: Context
    lateinit var databaseId: String
    private var isolatedApp: FirebaseApp? = null

    @Before
    open fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        // Match firebase.ci.json explicitly; never create an implicit open-rules database.
        databaseId = "(default)"
        resetEmulator("http://$EMULATOR_HOST:$FIRESTORE_PORT/emulator/v1/projects/$PROJECT_ID/databases/$databaseId/documents")
        resetEmulator("http://$EMULATOR_HOST:$AUTH_PORT/emulator/v1/projects/$PROJECT_ID/accounts")

        // Never reuse the production/default FirebaseApp, even when google-services.json exists.
        val app = FirebaseApp.initializeApp(
            context,
            FirebaseOptions.Builder()
                .setProjectId(PROJECT_ID)
                .setApiKey("fake-api-key-for-emulator")
                .setApplicationId("1:1234567890:android:abcdef")
                .build(),
            "rules-test-${UUID.randomUUID()}"
        )
        isolatedApp = app
        db = FirebaseFirestore.getInstance(app, databaseId)
        db.useEmulator(EMULATOR_HOST, FIRESTORE_PORT)
        db.firestoreSettings = FirebaseFirestoreSettings.Builder()
            .setLocalCacheSettings(MemoryCacheSettings.newBuilder().build())
            .build()
        auth = FirebaseAuth.getInstance(app)
        auth.useEmulator(EMULATOR_HOST, AUTH_PORT)
    }

    @After
    open fun tearDown() {
        if (::auth.isInitialized) auth.signOut()
        if (::db.isInitialized) db.terminate()
        isolatedApp?.delete()
        isolatedApp = null
    }

    protected suspend fun signInTestUser(
        email: String = "alice@example.com",
        password: String = DEFAULT_PASSWORD
    ): String = withContext(Dispatchers.IO) {
        withTimeout(AUTH_TIMEOUT_MS) {
            val result = try {
                auth.signInWithEmailAndPassword(email, password).await()
            } catch (_: FirebaseAuthInvalidUserException) {
                auth.createUserWithEmailAndPassword(email, password).await()
            } catch (_: Exception) {
                auth.createUserWithEmailAndPassword(email, password).await()
            }
            checkNotNull(result.user?.uid) { "Emulator user authentication failed" }
        }
    }

    private fun resetEmulator(endpoint: String) {
        val connection = URL(endpoint).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "DELETE"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            val status = connection.responseCode
            check(status in 200..299) { "Firebase emulator reset failed ($status). Run tests through firebase emulators:exec with firebase.ci.json." }
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        const val EMULATOR_HOST = "127.0.0.1"
        const val FIRESTORE_PORT = 8085
        const val AUTH_PORT = 9099
        const val PROJECT_ID = "demo-no-project"
        const val DEFAULT_PASSWORD = "password123"
        const val AUTH_TIMEOUT_MS = 10000L
    }
}
