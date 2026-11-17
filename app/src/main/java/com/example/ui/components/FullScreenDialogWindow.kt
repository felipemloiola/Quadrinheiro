package com.example.ui.components

import android.view.WindowManager.LayoutParams.MATCH_PARENT
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import com.example.ui.theme.LocalIsDarkTheme

@Composable
internal fun FullScreenDialogWindow(lightStatusBar: Boolean = !LocalIsDarkTheme.current) {
    val view = LocalView.current
    val window = (view.parent as? DialogWindowProvider)?.window ?: return
    val lightNavigationBar = !LocalIsDarkTheme.current
    DisposableEffect(window) {
        WindowCompat.enableEdgeToEdge(window)
        window.setLayout(MATCH_PARENT, MATCH_PARENT)
        window.setWindowAnimations(0)
        window.setDimAmount(0f)
        onDispose {}
    }
    SideEffect {
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = lightStatusBar
            isAppearanceLightNavigationBars = lightNavigationBar
        }
    }
}
