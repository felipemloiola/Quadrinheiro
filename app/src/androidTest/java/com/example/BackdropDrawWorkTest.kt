package com.example

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class BackdropDrawWorkTest {
    @get:Rule val compose=createComposeRule()
    @Test fun reusedCaptureDrawsOnceAndStillUpdatesLiveContent() {
        val reuse=mutableStateOf(false)
        val tint=mutableStateOf(Color.Red)
        val draws=AtomicInteger()
        compose.setContent {
            val backdrop=rememberLayerBackdrop()
            Box(Modifier.size(120.dp).testTag("capture").layerBackdrop(backdrop,reuseRecordedContent=reuse.value)) {
                Canvas(Modifier.size(120.dp)){draws.incrementAndGet();drawRect(tint.value)}
            }
        }
        compose.waitForIdle()
        draws.set(0)
        compose.runOnIdle{tint.value=Color.Green}
        compose.waitForIdle()
        val duplicated=draws.get()
        compose.runOnIdle{reuse.value=true}
        compose.waitForIdle()
        draws.set(0)
        compose.runOnIdle{tint.value=Color.Blue}
        compose.waitForIdle()
        val reused=draws.get()
        assertTrue("Capture must still draw changed content",reused>0)
        assertTrue("Reuse must reduce child draw invocations: legacy=$duplicated reused=$reused",reused<duplicated)
        val pixel=compose.onNodeWithTag("capture").captureToImage().toPixelMap()[10,10]
        assertTrue(pixel.blue>.9f && pixel.red<.1f)
        println("BACKDROP_DRAW_WORK legacy=$duplicated reused=$reused")
    }
}
