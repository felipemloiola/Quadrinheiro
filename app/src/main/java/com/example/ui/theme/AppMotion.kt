package com.example.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring as composeSpring
import androidx.compose.animation.core.tween as composeTween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import kotlin.math.abs

/**
 * Shared motion & iOS 26 spring physics policy for touch feedback, sheets, floating menus,
 * Liquid Glass transitions, and gesture momentum across the app.
 *
 * Synthesizes Apple WWDC *Designing Fluid Interfaces* + Emil Kowalski's design engineering rules:
 * - Never `ease-in` on UI entrances; strong custom `ease-out` (`EaseOutQuint` / `EaseOutStrong`)
 * - Crisp 120ms press feedback (`0.97` scale) on everyday cards; elastic jelly spring on Liquid Glass controls
 * - Exit transitions ~20% snappier than entrances
 * - Bounce reserved exclusively for momentum-carrying gestures and elastic Liquid Glass controls
 * - Full `reducedMotion` compliance built into every preset
 */
@Immutable
data class AppMotion(val reduced: Boolean = false) {
    companion object {
        /** Strong custom ease-out curve for responsive UI entrances. */
        val EaseOutStrong: Easing = CubicBezierEasing(0.22f, 1.0f, 0.36f, 1.0f)

        /** Quintic ease-out curve (Emil Kowalski standard: immediate start, silky settle). */
        val EaseOutQuint: Easing = CubicBezierEasing(0.23f, 1.0f, 0.32f, 1.0f)

        /** Strong ease-in-out curve for on-screen movement / segmented indicators. */
        val EaseInOutStrong: Easing = CubicBezierEasing(0.77f, 0.0f, 0.175f, 1.0f)

        /** Authentic iOS sheet deceleration curve (steep launch, whisper-soft landing). */
        val EaseSheet: Easing = CubicBezierEasing(0.32f, 0.72f, 0.0f, 1.0f)

        const val PRESS_FEEDBACK_MS: Int = 120
        const val UI_ENTER_MS: Int = 220
        const val UI_EXIT_MS: Int = 150

        /** Global iOS 26 spring stiffness / damping reference parameters. */
        const val IOS26_MODAL_DAMPING: Float = 0.86f
        const val IOS26_MODAL_STIFFNESS: Float = 380f
        const val IOS26_MODAL_EXIT_DAMPING: Float = 0.94f
        const val IOS26_MODAL_EXIT_STIFFNESS: Float = 560f
        const val IOS26_DETAIL_ZOOM_DAMPING: Float = 0.92f
        const val IOS26_DETAIL_ZOOM_STIFFNESS: Float = 380f
        const val IOS26_JELLY_DAMPING: Float = 0.56f
        const val IOS26_JELLY_STIFFNESS: Float = 390f
        const val IOS26_MENU_X_DAMPING: Float = 0.56f
        const val IOS26_MENU_X_STIFFNESS: Float = 430f
        const val IOS26_MENU_Y_DAMPING: Float = 0.60f
        const val IOS26_MENU_Y_STIFFNESS: Float = 390f
        const val IOS26_MENU_COLLAPSE_DAMPING: Float = 0.88f
        const val IOS26_MENU_COLLAPSE_STIFFNESS: Float = 720f

        /**
         * Apple WWDC exponential-decay momentum projection:
         * determines where a flick is headed so fast short swipes commit naturally.
         * Use `decelerationRate = 0.99f` for snappy sheets and `0.998f` for scroll-like inertia.
         */
        fun projectMomentum(
            position: Float,
            velocityPxPerSec: Float,
            decelerationRate: Float = 0.99f
        ): Float = position + (velocityPxPerSec / 1000f) * decelerationRate / (1f - decelerationRate)

        /**
         * Progressive rubber-band boundary resistance:
         * the further past an edge the finger pulls, the less the surface yields.
         */
        fun rubberband(
            overshoot: Float,
            dimension: Float,
            constant: Float = 0.55f
        ): Float {
            if (overshoot == 0f || dimension <= 0f) return 0f
            return (overshoot * dimension * constant) / (dimension + constant * abs(overshoot))
        }
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

    /** Global iOS 26 organic entrance spring for modals, dialogs, and floating sheets. */
    fun <T> ios26ModalSpring(visibilityThreshold: T? = null): FiniteAnimationSpec<T> = spring(
        dampingRatio = IOS26_MODAL_DAMPING,
        stiffness = IOS26_MODAL_STIFFNESS,
        visibilityThreshold = visibilityThreshold
    )

    /** Faster (~20% snappier), critically damped dismissal spring for closing modals & sheets. */
    fun <T> ios26ModalExitSpring(visibilityThreshold: T? = null): FiniteAnimationSpec<T> = spring(
        dampingRatio = IOS26_MODAL_EXIT_DAMPING,
        stiffness = IOS26_MODAL_EXIT_STIFFNESS,
        visibilityThreshold = visibilityThreshold
    )

    /** Liquid Glass zoom & receding background spring for Comic List <-> Detail transitions. */
    fun <T> ios26DetailZoomSpring(visibilityThreshold: T? = null): FiniteAnimationSpec<T> = spring(
        dampingRatio = IOS26_DETAIL_ZOOM_DAMPING,
        stiffness = IOS26_DETAIL_ZOOM_STIFFNESS,
        visibilityThreshold = visibilityThreshold
    )

    /** Balanced iOS 26 / Tab Bar jelly spring for Liquid Glass buttons and interactive pills. */
    fun <T> ios26JellyPressSpring(visibilityThreshold: T? = null): FiniteAnimationSpec<T> = spring(
        dampingRatio = IOS26_JELLY_DAMPING,
        stiffness = IOS26_JELLY_STIFFNESS,
        visibilityThreshold = visibilityThreshold
    )

    /** Floating Liquid Glass menu morph spring (axis 0 = X, axis 1 = Y for organic phase offset). */
    fun <T> ios26FloatingMenuSpring(axis: Int = 0, visibilityThreshold: T? = null): FiniteAnimationSpec<T> = spring(
        dampingRatio = if (axis == 0) IOS26_MENU_X_DAMPING else IOS26_MENU_Y_DAMPING,
        stiffness = if (axis == 0) IOS26_MENU_X_STIFFNESS else IOS26_MENU_Y_STIFFNESS,
        visibilityThreshold = visibilityThreshold
    )

    /** Snappy collapse spring for floating menus (exit faster than entry). */
    fun <T> ios26FloatingMenuCollapseSpring(visibilityThreshold: T? = null): FiniteAnimationSpec<T> = spring(
        dampingRatio = IOS26_MENU_COLLAPSE_DAMPING,
        stiffness = IOS26_MENU_COLLAPSE_STIFFNESS,
        visibilityThreshold = visibilityThreshold
    )

    /**
     * Drag-release spring following Apple HIG:
     * slight tactile bounce ONLY when the gesture carried velocity/momentum;
     * critically damped (`1.0f`) when settled programmatically via a button press.
     */
    fun <T> ios26DragReleaseSpring(
        carriedMomentum: Boolean,
        visibilityThreshold: T? = null
    ): FiniteAnimationSpec<T> = spring(
        dampingRatio = if (carriedMomentum) 0.82f else 1.0f,
        stiffness = if (carriedMomentum) 440f else 480f,
        visibilityThreshold = visibilityThreshold
    )

    /** Crisp 120ms EaseOutQuint tween for high-frequency card press feedback (`scale 0.97`). */
    fun <T> pressFeedbackTween(): TweenSpec<T> = tween(
        durationMillis = PRESS_FEEDBACK_MS,
        easing = EaseOutQuint
    )
}

val LocalAppMotion = staticCompositionLocalOf { AppMotion() }

@Composable
fun rememberReducedMotion(): () -> Boolean {
    val state = rememberUpdatedState(LocalAppMotion.current.reduced)
    return { state.value }
}
