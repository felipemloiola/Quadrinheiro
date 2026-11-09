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
}
