package com.example.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
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
    companion object {
        /** Strong custom ease-out curve (Emil Kowalski standard for responsive UI entrances). */
        val EaseOutStrong: Easing = CubicBezierEasing(0.22f, 1.0f, 0.36f, 1.0f)
        /** Quintic ease-out curve for crisp, immediate deceleration. */
        val EaseOutQuint: Easing = CubicBezierEasing(0.23f, 1.0f, 0.32f, 1.0f)
        const val UI_ENTER_MS: Int = 220
        const val UI_EXIT_MS: Int = 150
    }

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
        durationMillis: Int = UI_ENTER_MS,
        delayMillis: Int = 0,
        easing: Easing = EaseOutStrong
    ): TweenSpec<T> = composeTween(if (reduced) 0 else durationMillis, if (reduced) 0 else delayMillis, easing)
}

val LocalAppMotion = staticCompositionLocalOf { AppMotion() }

@Composable
fun rememberReducedMotion(): () -> Boolean {
    val state = rememberUpdatedState(LocalAppMotion.current.reduced)
    return { state.value }
}
