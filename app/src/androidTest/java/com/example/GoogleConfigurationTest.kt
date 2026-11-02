package com.example

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.data.GoogleClientId
import com.google.firebase.FirebaseApp
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GoogleConfigurationTest {
    @Test fun generatedResourcesInitializeTheCorrectFirebaseProjectAndWebClient() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val app = FirebaseApp.getApps(context).firstOrNull() ?: FirebaseApp.initializeApp(context)
        assertNotNull("Google Services must generate the Firebase app resources", app)
        assertEquals("gen-lang-client-0375936605", app!!.options.projectId)
        assertEquals("1:637011560599:android:c41b44fbba74a4d0e5c1d2", app.options.applicationId)
        assertEquals(
            "637011560599-sdht9lfjpl2058idv8usoovo1jpd40ar.apps.googleusercontent.com",
            GoogleClientId.required(context)
        )
    }
}
