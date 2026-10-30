package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalIOS27Theme
import com.kyant.shapes.ContinuousRoundedRectangle

/**
 * Logotipo oficial do aplicativo "Quadrinheiro" construído puramente via Canvas.
 *
 * Conceito Criativo: "Página de HQ em Origâmi"
 * Os painéis clássicos de quadrinhos e as linhas de sarjeta (gutters) se fundem
 * em dobras isométricas tridimensionais formando a letra "Q".
 *
 * Diretrizes:
 * 1. Ilusão Tridimensional (Folded Origami Look): Gradientes suaves simulam iluminação
 *    em diferentes ângulos de inclinação das facetas de papel dobrado.
 * 2. Cauda do "Q" Dinâmica: O bloco inferior direito projeta-se em ângulo agudo para fora
 *    do limite imaginário, quebrando a rigidez do quadrado em um corte de ação moderno.
 * 3. Cortes de Mangá/Ação: Polígonos irregulares com ângulos dinâmicos.
 * 4. Paleta Neo-Comic:
 *    - Painel Esquerdo: Vermelho Carmim -> Rosa Neon
 *    - Painel Superior: Amarelo Ouro -> Laranja Vibrante
 *    - Painel Inferior (Cauda do Q): Azul Royal -> Ciano Elétrico
 * 5. Sarjetas transparentes e vazias separando os blocos com respiro orgânico.
 */
@Composable
fun QuadrinheiroLogo(
    modifier: Modifier = Modifier,
    contentDesc: String = "Logotipo do Quadrinheiro"
) {
    Canvas(
        modifier = modifier
            .aspectRatio(1f)
            .semantics { contentDescription = contentDesc }
    ) {
        val w = size.width
        val h = size.height

        // -------------------------------------------------------------------------
        // 1. PALETA NEO-COMIC COM GRADIENTES TRIDIMENSIONAIS (ILUMINAÇÃO DE ORIGÂMI)
        // -------------------------------------------------------------------------

        // Painel Esquerdo: Vermelho Carmim rico para Rosa Neon elétrico
        val leftPanelBrush = Brush.linearGradient(
            colors = listOf(
                Color(0xFFFF1493), // Rosa Neon (reflexo superior/externo)
                Color(0xFFE11D48), // Vermelho Carmim (tom médio)
                Color(0xFF881337)  // Carmim Profundo (sombra na sarjeta)
            ),
            start = Offset(w * 0.08f, h * 0.12f),
            end = Offset(w * 0.36f, h * 0.78f)
        )

        // Painel Superior: Amarelo Ouro quente para Laranja Vibrante
        val topPanelBrush = Brush.linearGradient(
            colors = listOf(
                Color(0xFFFFD000), // Amarelo Ouro
                Color(0xFFFF9100), // Âmbar Intenso
                Color(0xFFFF3D00)  // Laranja Vibrante de Ação
            ),
            start = Offset(w * 0.44f, h * 0.08f),
            end = Offset(w * 0.90f, h * 0.45f)
        )

        // Painel Inferior (Cauda do Q): Azul Royal Noturno para Ciano Elétrico
        val bottomTailBrush = Brush.linearGradient(
            colors = listOf(
                Color(0xFF1E3A8A), // Azul Royal Noturno (sombra de vinco)
                Color(0xFF2563EB), // Azul Royal Vibrante
                Color(0xFF00B4D8), // Azul Elétrico
                Color(0xFF00F5FF)  // Ciano Elétrico (na ponta da cauda)
            ),
            start = Offset(w * 0.30f, h * 0.68f),
            end = Offset(w * 0.98f, h * 0.95f)
        )

        // Brilho de crista/dobra de papel (origami facet highlight)
        val foldSpecularBrush = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.38f),
                Color.White.copy(alpha = 0.08f),
                Color.Transparent
            ),
            start = Offset(w * 0.10f, h * 0.10f),
            end = Offset(w * 0.70f, h * 0.70f)
        )

        // -------------------------------------------------------------------------
        // 2. POLÍGONOS MODERNOS (PATH) COM SARJETAS VAZIAS TRANSPARENTES
        // -------------------------------------------------------------------------

        // A) PAINEL ESQUERDO: Coluna esquerda angular formando o primeiro arco do "Q"
        val leftPanelPath = Path().apply {
            moveTo(w * 0.36f, h * 0.08f)
            lineTo(w * 0.12f, h * 0.18f)
            lineTo(w * 0.06f, h * 0.50f)
            lineTo(w * 0.16f, h * 0.82f)
            lineTo(w * 0.28f, h * 0.74f)
            lineTo(w * 0.33f, h * 0.60f)
            lineTo(w * 0.29f, h * 0.48f)
            lineTo(w * 0.34f, h * 0.28f)
            close()
        }

        // B) PAINEL SUPERIOR: Coroa superior e ombro direito do "Q"
        val topPanelPath = Path().apply {
            moveTo(w * 0.43f, h * 0.08f)
            lineTo(w * 0.70f, h * 0.08f)
            lineTo(w * 0.90f, h * 0.22f)
            lineTo(w * 0.90f, h * 0.45f)
            lineTo(w * 0.65f, h * 0.45f)
            lineTo(w * 0.62f, h * 0.36f)
            lineTo(w * 0.49f, h * 0.27f)
            lineTo(w * 0.41f, h * 0.28f)
            close()
        }

        // C) PAINEL INFERIOR & CAUDA DO "Q":
        // Fecha o loop inferior e se projeta agressivamente para fora no canto inferior direito
        val bottomTailPath = Path().apply {
            moveTo(w * 0.65f, h * 0.52f)
            lineTo(w * 0.88f, h * 0.52f)
            lineTo(w * 0.84f, h * 0.65f)

            // --- CAUDA DO Q (PROJEÇÃO DINÂMICA FORA DO CORPO) ---
            lineTo(w * 0.98f, h * 0.86f) // Aresta superior externa da cauda
            lineTo(w * 0.95f, h * 0.95f) // Vértice afiado da cauda
            lineTo(w * 0.73f, h * 0.87f) // Corte inferior voltando ao arco

            lineTo(w * 0.52f, h * 0.90f)
            lineTo(w * 0.29f, h * 0.85f)
            lineTo(w * 0.38f, h * 0.68f)
            lineTo(w * 0.50f, h * 0.64f)
            close()
        }

        // -------------------------------------------------------------------------
        // 3. FACETAS DE DOBRA EM ORIGÂMI (EFEITO ISOMÉTRICO 3D)
        // -------------------------------------------------------------------------

        // Sub-faceta superior do painel esquerdo (simula dobra iluminada)
        val leftUpperFacet = Path().apply {
            moveTo(w * 0.36f, h * 0.08f)
            lineTo(w * 0.12f, h * 0.18f)
            lineTo(w * 0.06f, h * 0.50f)
            lineTo(w * 0.29f, h * 0.48f)
            lineTo(w * 0.34f, h * 0.28f)
            close()
        }

        // Sub-faceta da cauda do Q (crista luminosa em ciano)
        val tailLightFacet = Path().apply {
            moveTo(w * 0.65f, h * 0.52f)
            lineTo(w * 0.88f, h * 0.52f)
            lineTo(w * 0.84f, h * 0.65f)
            lineTo(w * 0.98f, h * 0.86f)
            lineTo(w * 0.95f, h * 0.95f)
            lineTo(w * 0.50f, h * 0.64f)
            close()
        }

        // -------------------------------------------------------------------------
        // 4. DESENHO DOS PAINÉIS PRINCIPAIS COM OS GRADIENTES
        // -------------------------------------------------------------------------

        // Painel Esquerdo (Vermelho Carmim -> Rosa Neon)
        drawPath(path = leftPanelPath, brush = leftPanelBrush)

        // Painel Superior (Amarelo Ouro -> Laranja Vibrante)
        drawPath(path = topPanelPath, brush = topPanelBrush)

        // Painel Inferior + Cauda Dinâmica (Azul Royal -> Ciano Elétrico)
        drawPath(path = bottomTailPath, brush = bottomTailBrush)

        // -------------------------------------------------------------------------
        // 5. ILUMINAÇÃO DE DOBRA (3D ORIGAMI OVERLAYS)
        // -------------------------------------------------------------------------

        // Reflexo de luz na dobra superior esquerda
        drawPath(
            path = leftUpperFacet,
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.26f),
                    Color.White.copy(alpha = 0.04f),
                    Color.Transparent
                ),
                start = Offset(w * 0.12f, h * 0.18f),
                end = Offset(w * 0.30f, h * 0.50f)
            )
        )

        // Reflexo na lâmina da cauda do Q
        drawPath(
            path = tailLightFacet,
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.22f),
                    Color(0xFF00F5FF).copy(alpha = 0.35f),
                    Color.Transparent
                ),
                start = Offset(w * 0.65f, h * 0.52f),
                end = Offset(w * 0.98f, h * 0.95f)
            )
        )

        // -------------------------------------------------------------------------
        // 6. LINHAS DE CRISTA (ARESTAS DE PAPEL AFIADAS)
        // -------------------------------------------------------------------------
        val strokeWidthPx = (w * 0.012f).coerceAtLeast(1.2f)

        drawPath(
            path = leftPanelPath,
            brush = foldSpecularBrush,
            style = Stroke(width = strokeWidthPx)
        )
        drawPath(
            path = topPanelPath,
            brush = foldSpecularBrush,
            style = Stroke(width = strokeWidthPx)
        )
        drawPath(
            path = bottomTailPath,
            brush = foldSpecularBrush,
            style = Stroke(width = strokeWidthPx)
        )
    }
}

/**
 * Variante temática para uso dentro do app, adaptando fundo à paleta atual.
 * O logo original é preservado para a tela de login; nos demais contextos,
 * use esta versão para integrar com o tema claro/escuro.
 */
@Composable
fun QuadrinheiroLogoThemed(
    modifier: Modifier = Modifier,
    contentDesc: String = "Logotipo do Quadrinheiro (tema)"
) {
    val themeColors = LocalIOS27Theme.current
    val shape = ContinuousRoundedRectangle(14.dp)

    Box(
        modifier = modifier
            .clip(shape)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        themeColors.bgPrimary.copy(alpha = if (themeColors.isDark) 0.60f else 0.40f),
                        themeColors.accent.copy(alpha = 0.16f)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.45f),
                        Color.White.copy(alpha = 0.12f)
                    )
                ),
                shape = shape
            )
            .semantics { contentDescription = contentDesc },
        contentAlignment = Alignment.Center
    ) {
        QuadrinheiroLogo(
            modifier = Modifier.fillMaxSize(0.78f),
            contentDesc = contentDesc
        )
    }
}

/**
 * Emblema em Liquid Glass com o QuadrinheiroLogo emoldurado com cantos contínuos e brilho óptico.
 * Usado principalmente em contextos de ícone; o logo dentro do badge é ligeiramente menor
 * e o fundo acompanha o tema atual para evitar a sensação de "vazar".
 */
@Composable
fun QuadrinheiroLogoBadge(
    modifier: Modifier = Modifier,
    sizeDp: Dp = 56.dp,
    cornerRadius: Dp = 16.dp,
    elevation: Dp = 8.dp
) {
    val themeColors = LocalIOS27Theme.current
    val shape = ContinuousRoundedRectangle(cornerRadius)

    Box(
        modifier = modifier
            .size(sizeDp)
            .shadow(
                elevation = elevation,
                shape = shape,
                spotColor = themeColors.accent.copy(alpha = 0.35f)
            )
            .clip(shape)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        themeColors.bgPrimary.copy(alpha = if (themeColors.isDark) 0.70f else 0.45f),
                        themeColors.bgPrimary.copy(alpha = if (themeColors.isDark) 0.90f else 0.65f)
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
            modifier = Modifier.fillMaxSize(0.70f)
        )
    }
}
