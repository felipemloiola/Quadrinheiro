package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.shapes.ContinuousRoundedRectangle

/**
 * Logo oficial do aplicativo Quadrinheiro:
 * Inspirado nos quadros e sarjetas (gutters) clássicos de histórias em quadrinhos,
 * com três painéis coloridos vibrantes (Magenta/Vermelho, Âmbar/Amarelo, Azul)
 * separados por sarjetas limpas que mantêm proporção perfeita de 1:1.
 */
@Composable
fun QuadrinheiroLogo(
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .aspectRatio(1f) // keep perfect square
    ) {
        val w = size.width
        val h = size.height

        // Gutters (sarjetas) ~4% of width
        val gutter = w * 0.04f
        val halfGutter = gutter / 2f

        // Slanted divider between left and right blocks.
        // Wider at the top, narrower at the bottom (leaning to the left).
        val dividerTopX = w * 0.40f
        val dividerBottomX = w * 0.28f

        // Helper: x on the slanted divider for a given y, with optional offset
        fun slantedX(y: Float, offset: Float = 0f): Float {
            val t = y / h
            val baseX = dividerTopX + (dividerBottomX - dividerTopX) * t
            return baseX + offset
        }

        // Horizontal divider between top (yellow) and bottom (blue) right blocks.
        val horizontalCenterY = h * 0.52f
        val topBlockBottomY = horizontalCenterY - halfGutter
        val bottomBlockTopY = horizontalCenterY + halfGutter

        // Colors (Material-style vibrant palette)
        val magentaRed = Color(0xFFF50057)  // Vibrant Pink/Red
        val amberYellow = Color(0xFFFFC107) // Amber
        val blue = Color(0xFF2196F3)        // Vibrant Blue
        val gutterColor = Color.White

        // 1) Left vertical trapezoid (magenta red)
        val leftPath = Path().apply {
            moveTo(0f, 0f)
            lineTo(slantedX(0f, -halfGutter), 0f)
            lineTo(slantedX(h, -halfGutter), h)
            lineTo(0f, h)
            close()
        }
        drawPath(path = leftPath, color = magentaRed)

        // 2) Top-right block (yellow), with left border following the slanted divider
        val topRightPath = Path().apply {
            moveTo(slantedX(0f, +halfGutter), 0f)
            lineTo(w, 0f)
            lineTo(w, topBlockBottomY)
            lineTo(slantedX(topBlockBottomY, +halfGutter), topBlockBottomY)
            close()
        }
        drawPath(path = topRightPath, color = amberYellow)

        // 3) Bottom-right block (blue), separated by a clean horizontal gutter
        val bottomRightPath = Path().apply {
            moveTo(slantedX(bottomBlockTopY, +halfGutter), bottomBlockTopY)
            lineTo(w, bottomBlockTopY)
            lineTo(w, h)
            lineTo(slantedX(h, +halfGutter), h)
            close()
        }
        drawPath(path = bottomRightPath, color = blue)

        // 4) Vertical white gutter (slanted strip between left and right blocks)
        val verticalGutterPath = Path().apply {
            moveTo(slantedX(0f, -halfGutter), 0f)
            lineTo(slantedX(0f, +halfGutter), 0f)
            lineTo(slantedX(h, +halfGutter), h)
            lineTo(slantedX(h, -halfGutter), h)
            close()
        }
        drawPath(path = verticalGutterPath, color = gutterColor)

        // 5) Horizontal white gutter (straight line between yellow and blue blocks)
        drawRect(
            color = gutterColor,
            topLeft = Offset(slantedX(horizontalCenterY, +halfGutter), horizontalCenterY - halfGutter),
            size = Size(
                width = w - slantedX(horizontalCenterY, +halfGutter),
                height = gutter
            )
        )
    }
}

/**
 * Emblema em Liquid Glass com o QuadrinheiroLogo emoldurado com cantos contínuos e brilho óptico.
 */
@Composable
fun QuadrinheiroLogoBadge(
    modifier: Modifier = Modifier,
    sizeDp: Dp = 56.dp,
    cornerRadius: Dp = 16.dp,
    elevation: Dp = 8.dp
) {
    val shape = ContinuousRoundedRectangle(cornerRadius)
    Box(
        modifier = modifier
            .size(sizeDp)
            .shadow(
                elevation = elevation,
                shape = shape,
                spotColor = Color(0xFFF50057).copy(alpha = 0.35f)
            )
            .clip(shape)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.22f),
                        Color.White.copy(alpha = 0.08f)
                    )
                )
            )
            .border(
                width = 1.2.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.70f),
                        Color.White.copy(alpha = 0.20f)
                    )
                ),
                shape = shape
            ),
        contentAlignment = Alignment.Center
    ) {
        QuadrinheiroLogo(
            modifier = Modifier
                .size(sizeDp)
                .clip(shape)
        )
    }
}
