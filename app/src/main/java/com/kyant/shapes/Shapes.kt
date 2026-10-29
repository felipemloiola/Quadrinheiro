package com.kyant.shapes

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

enum class RoundedCornerStyle {
    Circular,
    Continuous
}

class Corners(
    val topLeft: Float,
    val topRight: Float,
    val bottomRight: Float,
    val bottomLeft: Float
)

interface RoundedRectangularShape : Shape {
    val style: RoundedCornerStyle
    fun corners(size: Size, layoutDirection: LayoutDirection, density: Density): Corners
}

class Capsule(
    override val style: RoundedCornerStyle = RoundedCornerStyle.Continuous
) : RoundedRectangularShape {
    override fun corners(size: Size, layoutDirection: LayoutDirection, density: Density): Corners {
        val radius = if (size != Size.Unspecified) size.minDimension * 0.5f else 0f
        return Corners(radius, radius, radius, radius)
    }

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val radius = if (size != Size.Unspecified) size.minDimension * 0.5f else 0f
        val rect = if (size != Size.Unspecified) size.toRect() else androidx.compose.ui.geometry.Rect.Zero
        return Outline.Rounded(RoundRect(rect, CornerRadius(radius, radius)))
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Capsule) return false
        return style == other.style
    }

    override fun hashCode(): Int = style.hashCode()

    override fun toString(): String = "Capsule(style=$style)"
}

class RoundedRectangle(
    val cornerRadius: Dp,
    override val style: RoundedCornerStyle = RoundedCornerStyle.Continuous
) : RoundedRectangularShape {
    override fun corners(size: Size, layoutDirection: LayoutDirection, density: Density): Corners {
        val radiusPx = with(density) { cornerRadius.toPx() }
        return Corners(radiusPx, radiusPx, radiusPx, radiusPx)
    }

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val radiusPx = with(density) { cornerRadius.toPx() }
        val rect = if (size != Size.Unspecified) size.toRect() else androidx.compose.ui.geometry.Rect.Zero
        return Outline.Rounded(RoundRect(rect, CornerRadius(radiusPx, radiusPx)))
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RoundedRectangle) return false
        return cornerRadius == other.cornerRadius && style == other.style
    }

    override fun hashCode(): Int = 31 * cornerRadius.hashCode() + style.hashCode()

    override fun toString(): String = "RoundedRectangle(cornerRadius=$cornerRadius, style=$style)"
}

fun ContinuousRoundedRectangle(cornerRadius: Dp): RoundedRectangle = RoundedRectangle(cornerRadius, RoundedCornerStyle.Continuous)
val ContinuousCapsule: Capsule = Capsule(RoundedCornerStyle.Continuous)

