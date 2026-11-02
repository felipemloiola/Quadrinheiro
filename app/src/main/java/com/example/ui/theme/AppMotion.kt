package com.example.ui.theme

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.spring as composeSpring
import androidx.compose.animation.core.tween as composeTween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf

/** Shared policy for touch feedback, sheets, navigation and decorative motion. */
@Immutable
data class AppMotion(val reduced: Boolean = false) {
    fun <T> spring(
        dampingRatio: Float = 1f,
        stiffness: Float = Spring.StiffnessMedium,
        visibilityThreshold: T? = null
    ): FiniteAnimationSpec<T> = if (reduced) snap() else composeSpring(
        dampingRatio = dampingRatio,
        stiffness = stiffness,
        visibilityThreshold = visibilityThreshold
    )

    fun <T> tween(
        durationMillis: Int = 300,
        delayMillis: Int = 0,
        easing: Easing = FastOutSlowInEasing
    ): TweenSpec<T> = composeTween(if (reduced) 0 else durationMillis, if (reduced) 0 else delayMillis, easing)
}

val LocalAppMotion = staticCompositionLocalOf { AppMotion() }

@Composable
fun rememberReducedMotion(): () -> Boolean {
    val state = rememberUpdatedState(LocalAppMotion.current.reduced)
    return { state.value }
}
