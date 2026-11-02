package com.example

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.material3.Text
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.ui.components.AppleTextField
import com.example.ui.components.AppleStepper
import com.example.ui.components.LiquidGlassSegmentedControl
import com.example.ui.theme.QuadrinheiroTheme
import com.kyant.backdrop.backdrops.emptyBackdrop
import com.kyant.backdrop.catalog.components.LiquidBottomTab
import com.kyant.backdrop.catalog.components.LiquidBottomTabs
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File

@RunWith(AndroidJUnit4::class)
class NavigationTouchTest {
    @get:Rule val compose = createComposeRule()



    @Test fun clearingTextDoesNotChangeTheFieldHeight() {
        val text = mutableStateOf("Felipe")
        compose.setContent {
            QuadrinheiroTheme(settings = com.example.model.AppSettings(colorScheme = "dark")) {
                Column(Modifier.background(Color(0xFF152531)).padding(16.dp)) {
                    AppleTextField(text.value, { text.value = it }, "Nome", modifier = Modifier.testTag("field"))
                    AppleStepper(20, onChange = {}, modifier = Modifier.testTag("stepper"))
                }
            }
        }
        compose.onNodeWithTag("field").assertHeightIsEqualTo(48.dp)
        compose.onNodeWithTag("stepper").assertHeightIsEqualTo(48.dp)
        val screenshot = compose.onRoot().captureToImage().asAndroidBitmap()
        val file = File(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null), "compact-controls.png")
        file.outputStream().use { screenshot.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        compose.onNodeWithContentDescription("Limpar").performClick()
        compose.onNodeWithTag("field").assertHeightIsEqualTo(48.dp)
    }
}

