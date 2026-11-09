package com.example

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.example.data.CloudCacheRecovery
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import java.io.File
import java.io.RandomAccessFile
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

/** Synthetic SDK-size fixture in the emulator/test install. No recovery is confirmed. */
class CloudCacheRecoveryConsentTest {
    private lateinit var fixture: File
    @get:Rule(order=0) val oversizedCache=object:ExternalResource(){
        override fun before(){
            CloudCacheRecovery.resetForTests()
            val context=InstrumentationRegistry.getInstrumentation().targetContext
            fixture=context.getDatabasePath("firestore.consent-test."+context.getString(R.string.firestore_database_id))
            fixture.parentFile!!.mkdirs()
            RandomAccessFile(fixture,"rw").use{it.setLength(CloudCacheRecovery.reviewThresholdBytes(Runtime.getRuntime().maxMemory())+1)}
        }
        override fun after(){fixture.delete();CloudCacheRecovery.resetForTests()}
    }
    @get:Rule(order=1) val compose=createAndroidComposeRule<MainActivity>()
    @Test fun recoveryNeedsExplicitConfirmationAndCancelPreservesTheOriginal(){
        compose.onNodeWithText("Revisar sincronização").assertExists()
        compose.onNodeWithText("Preparar recuperação").performClick()
        compose.onNodeWithText("Reconstruir a fila local?").assertExists()
        compose.onNodeWithText("Criar cópia e reconstruir").assertExists()
        compose.onNodeWithText("Cancelar").performClick()
        compose.onNodeWithText("Revisar sincronização").assertExists()
        assertTrue(fixture.exists());assertTrue(fixture.length()>0)
    }
    @Test fun sdkCanClearBeforeOpeningItsClient(): Unit=runBlocking {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val app=FirebaseApp.initializeApp(context,FirebaseOptions.Builder()
            .setProjectId("demo-recovery-api").setApiKey("fake-api-key-for-local-test")
            .setApplicationId("1:1234567890:android:abcdef").build(),"recovery-local-api-test")
        val db=FirebaseFirestore.getInstance(app)
        db.useEmulator("127.0.0.1",8085)
        try {withTimeout(5000){db.clearPersistence().await()}}
        finally {withTimeout(5000){db.terminate().await()};app.delete()}
    }
}
