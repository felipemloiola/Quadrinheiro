package com.example

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.IntRect
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.model.AppSettings
import com.example.ui.components.*
import com.example.ui.theme.LocalIOS27Theme
import com.example.ui.theme.QuadrinheiroTheme
import com.kyant.backdrop.backdrops.emptyBackdrop
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.max
import kotlin.math.min

@RunWith(AndroidJUnit4::class)
class CurationGlassContrastTest {
    @get:Rule val compose=createComposeRule()
    @Test fun lightMenuKeepsTextReadableOverBlack()=checkContrast(false)
    @Test fun darkMenuKeepsTextReadableOverWhite()=checkContrast(true)

    private fun checkContrast(dark:Boolean) {
        var foreground=Color.Unspecified
        lateinit var host:LiquidGlassMenuHostState
        compose.setContent {
            QuadrinheiroTheme(AppSettings(colorScheme=if(dark)"dark" else "light",reduceMotion=true,
                reduceTransparency=false,glassOpacity=.1f,enableLensRefraction=false)) {
                host=remember {LiquidGlassMenuHostState()}
                val ink=LocalIOS27Theme.current.textPrimary
                SideEffect {foreground=ink}
                Box(Modifier.fillMaxSize().background(if(dark)Color.White else Color.Black)) {
                    LiquidGlassMenuOverlayHost(host,emptyBackdrop(),Modifier.fillMaxSize())
                }
            }
        }
        compose.runOnIdle {host.showMenu(LiquidGlassMenuRequest("contrast",IntRect(100,150,220,220),
            items=listOf(LiquidGlassMenuItemData("Escolher capa"){}),onDismiss={}))}
        val bounds=compose.onNodeWithTag("retained_glass_menu").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val pixels=compose.onRoot().captureToImage().toPixelMap()
        // Above the glyphs, inside the rounded surface and clear of its border/shadow.
        val paper=pixels[bounds.center.x.toInt(),(bounds.top+bounds.height*.18f).toInt()]
        val ink=foreground.compositeOver(paper)
        val ratio=(max(ink.luminance(),paper.luminance())+.05f)/(min(ink.luminance(),paper.luminance())+.05f)
        assertTrue("Contraste do texto sobre a superfície: $ratio",ratio>=4.5f)
    }
}
