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

    @Before
    open fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        databaseId = if (System.getenv("GITHUB_ACTIONS") == "true") {
            "(default)"
        } else {
            context.getString(R.string.firestore_database_id)
        }

        synchronized(FirestoreEmulatorTestBase::class.java) {
            val existing = FirebaseApp.getApps(context).firstOrNull { it.name == TEST_APP_NAME }
            val app = existing ?: FirebaseApp.initializeApp(
                context,
                FirebaseOptions.Builder()
                    .setProjectId(PROJECT_ID)
                    .setApiKey("fake-api-key-for-emulator")
                    .setApplicationId("1:1234567890:android:abcdef")
                    .build(),
                TEST_APP_NAME
            )
            check(app.options.projectId == PROJECT_ID) { "Rule tests must use only the demo Firebase project" }
            db = FirebaseFirestore.getInstance(app, databaseId)
            auth = FirebaseAuth.getInstance(app)
            if (existing == null) {
                // Configure SDK clients once, before their first operation.
                db.useEmulator(EMULATOR_HOST, FIRESTORE_PORT)
                db.firestoreSettings = FirebaseFirestoreSettings.Builder()
                    .setLocalCacheSettings(MemoryCacheSettings.newBuilder().build())
                    .build()
                auth.useEmulator(EMULATOR_HOST, AUTH_PORT)
            }
        }

        // Reset test state, not FirebaseApp: Auth internals can retain app references.
        auth.signOut()
        resetEmulator("http://$EMULATOR_HOST:$FIRESTORE_PORT/emulator/v1/projects/$PROJECT_ID/databases/$databaseId/documents")
        resetEmulator("http://$EMULATOR_HOST:$AUTH_PORT/emulator/v1/projects/$PROJECT_ID/accounts")
    }

    @After
    open fun tearDown() {
        if (::auth.isInitialized) auth.signOut()
        // Do not delete FirebaseApp or terminate the shared Firestore client here.
        // Emulator data is reset before each test; the JVM owns SDK lifetime.
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
        private const val TEST_APP_NAME = "quadrinheiro-rules-emulator-tests"
        const val EMULATOR_HOST = "127.0.0.1"
        const val FIRESTORE_PORT = 8085
        const val AUTH_PORT = 9099
        const val PROJECT_ID = "demo-no-project"
        const val DEFAULT_PASSWORD = "password123"
        const val AUTH_TIMEOUT_MS = 10000L
    }
}
