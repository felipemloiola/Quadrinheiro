package com.example.updates

import com.example.BuildConfig
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.security.MessageDigest

class UpdateVerifierTest {
    @get:Rule val temporary = TemporaryFolder()
    private val bytes = "an APK download fixture".toByteArray()
    private fun hash(data: ByteArray) = MessageDigest.getInstance("SHA-256").digest(data).joinToString("") { "%02x".format(it) }

    @Test fun completeDownloadIsCopiedAndVerified() {
        val destination = temporary.newFile()
        bytes.inputStream().use { UpdateVerifier.copyVerified(it, destination, hash(bytes)) }
        assertArrayEquals(bytes, destination.readBytes())
    }

    @Test fun interruptedOrAlteredDownloadIsRejectedAndRemoved() {
        val destination = temporary.newFile()
        assertThrows(IllegalStateException::class.java) {
            bytes.dropLast(2).toByteArray().inputStream().use { UpdateVerifier.copyVerified(it, destination, hash(bytes)) }
        }
        assertFalse(destination.exists())
    }

    @Test fun oversizedDownloadIsRejectedAndRemoved() {
        val destination = temporary.newFile()
        assertThrows(IllegalStateException::class.java) {
            bytes.inputStream().use { UpdateVerifier.copyVerified(it, destination, hash(bytes), 5L) }
        }
        assertFalse(destination.exists())
    }

    @Test fun exactPackageVersionAndPermanentSignerAreAccepted() {
        UpdateVerifier.validateIdentity(BuildConfig.APPLICATION_ID, 1039L, listOf(UPDATE_CERTIFICATE), 1039L, 1038L)
    }

    @Test fun anotherPackageWrongVersionOrDowngradeAreRejected() {
        assertThrows(IllegalStateException::class.java) { UpdateVerifier.validateIdentity("another.app", 1039L, listOf(UPDATE_CERTIFICATE), 1039L, 1038L) }
        assertThrows(IllegalStateException::class.java) { UpdateVerifier.validateIdentity(BuildConfig.APPLICATION_ID, 1040L, listOf(UPDATE_CERTIFICATE), 1039L, 1038L) }
        assertThrows(IllegalStateException::class.java) { UpdateVerifier.validateIdentity(BuildConfig.APPLICATION_ID, 1038L, listOf(UPDATE_CERTIFICATE), 1038L, 1038L) }
    }

    @Test fun changedOrAdditionalSignerIsRejected() {
        assertThrows(IllegalStateException::class.java) { UpdateVerifier.validateIdentity(BuildConfig.APPLICATION_ID, 1039L, listOf("0".repeat(64)), 1039L, 1038L) }
        assertThrows(IllegalStateException::class.java) { UpdateVerifier.validateIdentity(BuildConfig.APPLICATION_ID, 1039L, listOf(UPDATE_CERTIFICATE, UPDATE_CERTIFICATE), 1039L, 1038L) }
    }
}
