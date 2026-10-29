package com.kyant.backdrop.catalog

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.toPath

@Composable
fun rememberFlightIconPainter(): Painter {
    return remember { FlightIconPainter() }
}

private class FlightIconPainter : Painter() {

    override val intrinsicSize: Size = Size.Unspecified

    private val path = PathBuilder().apply {
        moveTo(10.18f, 9f)
        lineTo(10.18f, 9f)
        close()
        moveTo(21f, 16f)
        verticalLineToRelative(-2f)
        lineToRelative(-8f, -5f)
        verticalLineTo(3.5f)
        curveToRelative(0f, -0.83f, -0.67f, -1.5f, -1.5f, -1.5f)
        reflectiveCurveTo(10f, 2.67f, 10f, 3.5f)
        verticalLineTo(9f)
        lineToRelative(-8f, 5f)
        verticalLineToRelative(2f)
        lineToRelative(8f, -2.5f)
        verticalLineTo(19f)
        lineToRelative(-2f, 1.5f)
        verticalLineTo(22f)
        lineToRelative(3.5f, -1f)
        lineToRelative(3.5f, 1f)
        verticalLineToRelative(-1.5f)
        lineTo(13f, 19f)
        verticalLineToRelative(-5.5f)
        lineToRelative(8f, 2.5f)
        close()
    }.nodes.toPath().apply {
        fillType = PathFillType.EvenOdd
    }

    override fun DrawScope.onDraw() {
        val w = size.width
        val h = size.height
        scale(w / 24f, h / 24f, Offset.Zero) {
            drawPath(path, Color.Black)
        }
    }
}
