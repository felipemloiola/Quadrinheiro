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
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject
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
        databaseId = try {
            context.getString(R.string.firestore_database_id)
        } catch (_: Exception) {
            "ai-studio-android-quadrinh-d0141987-7d2c-491d-b885-fc910c102b71"
        }

        val app = if (FirebaseApp.getApps(context).isEmpty()) {
            val newApp = FirebaseApp.initializeApp(
                context,
                FirebaseOptions.Builder()
                    .setProjectId(PROJECT_ID)
                    .setApiKey("fake-api-key-for-emulator")
                    .setApplicationId("1:1234567890:android:abcdef")
                    .build()
            )
            val newFirestore = FirebaseFirestore.getInstance(newApp, databaseId)
            newFirestore.useEmulator(EMULATOR_HOST, FIRESTORE_PORT)
            newFirestore.firestoreSettings = FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(MemoryCacheSettings.newBuilder().build())
                .build()

            val newAuth = FirebaseAuth.getInstance(newApp)
            newAuth.useEmulator(EMULATOR_HOST, AUTH_PORT)
            newApp
        } else {
            FirebaseApp.getInstance()
        }

        db = FirebaseFirestore.getInstance(app, databaseId)
        auth = FirebaseAuth.getInstance(app)

        clearFirestoreEmulator()
        clearAuthEmulator()
    }

    @After
    open fun tearDown() {
        auth.signOut()
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
            checkNotNull(result.user?.uid) { "User auth failed" }
        }
    }

    private fun clearFirestoreEmulator() {
        for (dbName in listOf("(default)", "demo-database", databaseId)) {
            try {
                val url = URL("http://$EMULATOR_HOST:$FIRESTORE_PORT/emulator/v1/projects/$PROJECT_ID/databases/$dbName/documents")
                (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "DELETE"
                    connectTimeout = 2000
                    readTimeout = 2000
                    responseCode
                    disconnect()
                }
            } catch (_: Exception) {
            }
        }
    }

    private fun clearAuthEmulator() {
        try {
            val url = URL("http://$EMULATOR_HOST:$AUTH_PORT/emulator/v1/projects/$PROJECT_ID/accounts")
            (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "DELETE"
                connectTimeout = 2000
                readTimeout = 2000
                responseCode
                disconnect()
            }
        } catch (_: Exception) {
        }
    }

    companion object {
        const val EMULATOR_HOST = "127.0.0.1"
        const val FIRESTORE_PORT = 8085
        const val AUTH_PORT = 9099
        const val PROJECT_ID = "demo-no-project"
        const val DEFAULT_PASSWORD = "password123"
        const val AUTH_TIMEOUT_MS = 5000L
    }
}
