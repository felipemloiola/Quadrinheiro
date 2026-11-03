package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.ui.theme.LocalAppMotion

data class AppPage(val tab: String, val settings: String?, val parent: String?, val statistics: Boolean) {
    val depth: Int get() = if (statistics) 3 else if (settings != null) 1 + if (parent != null) 1 else 0 else 0
}

/** Tabs exchange gently; hierarchical destinations enter and leave on symmetric paths. */
@Composable
fun AppPageTransition(page: AppPage, content: @Composable (AppPage) -> Unit) {
    val motion = LocalAppMotion.current
    AnimatedContent(page, Modifier.fillMaxSize(), transitionSpec = {
        if (motion.reduced || initialState.tab != targetState.tab) {
            fadeIn(motion.tween(160)) togetherWith fadeOut(motion.tween(110))
        } else {
            val direction = if (targetState.depth >= initialState.depth) 1 else -1
            (slideInHorizontally(motion.spring(1f, 520f)) { direction * it } + fadeIn(motion.tween(150))) togetherWith
                (slideOutHorizontally(motion.spring(1f, 520f)) { -direction * it / 4 } + fadeOut(motion.tween(180)))
        }.using(SizeTransform(clip = true))
    }, label = "appPage") { destination -> content(destination) }
}
