package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import com.example.ui.components.LucideIcons
import com.example.ui.components.QuadrinheiroLogo
import com.example.ui.components.QuadrinheiroLogoVariant
import com.example.ui.theme.LocalAppleMaterials
import com.example.ui.theme.LocalAppSettings
import com.example.ui.theme.LocalIOS27Theme
import com.example.ui.theme.SfProDisplayFontFamily
import com.example.ui.theme.SfProTextFontFamily
import com.kyant.shapes.ContinuousRoundedRectangle

private data class AppleFeatureRow(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val tintColor: Color
)

/**
 * Tela de Entrada e Boas-vindas (Apple-style Onboarding & Welcome Flow):
 * 1. Simetria Vertical Estrita: A distância da borda superior até a logo e a distância da base do
 *    botão até a borda inferior são rigorosamente idênticas (distância do topo == distância da base).
 * 2. Espaçamento Coeso: O botão de ação fica a exatos 24dp imediatamente abaixo de "Busca Online e Nuvem".
 * 3. Animação Fluida: Transição com curva física de mola autêntica do iOS.
 */
@Composable
fun WelcomeIntroScreen(
    onContinue: () -> Unit
) {
    val theme = LocalIOS27Theme.current
    val mat = LocalAppleMaterials.current
    val s = mat.scheme
    val appSettings = LocalAppSettings.current
    val density = LocalDensity.current

    // Fundo atmosférico padronizado: idêntico ao AuthBackdropLayer da tela de login
    val backgroundBrush = remember(theme.bgPrimary, theme.accent, appSettings.reduceTransparency) {
        val accentWeight = if (appSettings.reduceTransparency) 0.03f else 0.11f
        Brush.verticalGradient(
            listOf(
                theme.bgPrimary,
                androidx.compose.ui.graphics.lerp(theme.bgPrimary, theme.accent, accentWeight),
                theme.bgPrimary
            )
        )
    }

    // Estado do fluxo: false = capa inicial centrada, true = tela de recursos expandida
    var isFeaturesView by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    // Suporte ao botão voltar do sistema para retroceder a animação suavemente
    BackHandler(enabled = isFeaturesView) {
        isFeaturesView = false
    }

    LaunchedEffect(isFeaturesView) {
        if (!isFeaturesView && scrollState.value > 0) {
            scrollState.animateScrollTo(0)
        }
    }

    // Curva física de mola autêntica do iOS (resposta suave e amortecimento natural)
    val transitionProgress by animateFloatAsState(
        targetValue = if (isFeaturesView) 1f else 0f,
        animationSpec = spring(
            dampingRatio = 0.82f,
            stiffness = 260f
        ),
        label = "welcomeFlowProgress"
    )

    // Paleta de cores semânticas do iOS (Human Interface Guidelines)
    val features = remember(s.isLight) {
        listOf(
            AppleFeatureRow(
                title = "Sua Biblioteca de Quadrinhos",
                subtitle = "Organize encadernados, graphic novels e mangás com sinopses completas, capas em alta definição e fichas técnicas detalhadas.",
                icon = LucideIcons.BookOpen,
                tintColor = if (s.isLight) Color(0xFF007AFF) else Color(0xFF0A84FF) // iOS System Blue
            ),
            AppleFeatureRow(
                title = "Sequências e Recomendações",
                subtitle = "Acompanhe edições em ordem cronológica (#1, #2, #3...) e descubra obras semelhantes para graphic novels como Watchmen e Maus.",
                icon = LucideIcons.Layers,
                tintColor = if (s.isLight) Color(0xFFFF9500) else Color(0xFFFF9F0A) // iOS System Orange
            ),
            AppleFeatureRow(
                title = "Metas e Diário de Leitura",
                subtitle = "Acompanhe seu ritmo de leitura com metas anuais, anéis de progresso diário e histórico de páginas lidas.",
                icon = Icons.Default.BarChart,
                tintColor = if (s.isLight) Color(0xFF34C759) else Color(0xFF30D158) // iOS System Green
            ),
            AppleFeatureRow(
                title = "Busca Online e Nuvem",
                subtitle = "Pesquise em múltiplos acervos online com tradução em português e mantenha seus dados salvos com o Google ou Modo Convidado.",
                icon = Icons.Default.CloudSync,
                tintColor = if (s.isLight) Color(0xFFAF52DE) else Color(0xFFBF5AF2) // iOS System Purple
            )
        )
    }

    val buttonTextColor = if (theme.accent.luminance() > 0.179f) Color.Black else Color.White

    // Medição dinâmica em tempo real da altura real do bloco de conteúdo
    var measuredContentHeightPx by remember { mutableStateOf(0) }
    val measuredContentHeightDp = with(density) { measuredContentHeightPx.toDp() }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
            .padding(horizontal = 28.dp)
            .testTag("welcome_intro_root"),
        contentAlignment = Alignment.TopCenter
    ) {
        val totalScreenHeight = maxHeight

        // Altura real ou estimada do conteúdo do bloco
        val estimatedSplashHeight = 240.dp
        val estimatedFeaturesHeight = 520.dp
        val actualContentHeight = if (measuredContentHeightDp > 100.dp) {
            measuredContentHeightDp
        } else {
            lerp(estimatedSplashHeight, estimatedFeaturesHeight, transitionProgress)
        }

        // Espaçador perfeitamente simétrico: (altura total da tela - altura do conteúdo) / 2
        // Garante rigorosamente que a distância do topo até a logo seja idêntica à distância do botão até a base da tela
        val symmetricPadding = ((totalScreenHeight - actualContentHeight) / 2f).coerceAtLeast(36.dp)

        // Tamanho da logo: 96dp na capa inicial -> 60dp no topo da tela de recursos
        val iconSize = lerp(96.dp, 60.dp, transitionProgress)

        // Distância entre a base da logo e o topo do título: 10dp na capa -> 14dp nos recursos
        val currentLogoToTitleSpacer = lerp(10.dp, 14.dp, transitionProgress)

        // Altura animada da área de conteúdo (64dp da capa -> 404dp dos recursos)
        val contentBoxHeight = lerp(64.dp, 404.dp, transitionProgress)
        val contentBoxModifier = if (transitionProgress < 0.99f) {
            Modifier
                .fillMaxWidth()
                .height(contentBoxHeight)
        } else {
            Modifier
                .fillMaxWidth()
                .wrapContentHeight()
        }

        // Espaçamento estrito de 20dp na capa e 24dp imediatamente abaixo de "Busca Online e Nuvem"
        val currentButtonTopSpacer = lerp(20.dp, 24.dp, transitionProgress)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 520.dp)
                .verticalScroll(scrollState, enabled = transitionProgress > 0.92f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Espaçador do Topo (distância do topo da tela até a logo)
            Spacer(modifier = Modifier.height(symmetricPadding))

            // 2. Bloco de Conteúdo Medido Unificado (Logo + Textos + Recursos + Botão)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .onSizeChanged { measuredContentHeightPx = it.height },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Ícone Hero oficial
                QuadrinheiroLogo(
                    variant = QuadrinheiroLogoVariant.Primary,
                    titleVisible = false,
                    modifier = Modifier
                        .size(iconSize)
                        .testTag("welcome_hero_logo")
                )

                // Espaçamento de design entre a base do logo e o topo do título
                Spacer(modifier = Modifier.height(currentLogoToTitleSpacer))

                // Conteúdo central / de recursos com transição coordenada
                Box(
                    modifier = contentBoxModifier,
                    contentAlignment = Alignment.TopCenter
                ) {
                    // 1. TELA INICIAL (CAPA): Nome do app e subtítulo minimalista centralizados
                    if (transitionProgress < 0.99f) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer {
                                    alpha = (1f - transitionProgress * 2.2f).coerceIn(0f, 1f)
                                    translationY = -transitionProgress * 20f
                                },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Quadrinheiro",
                                fontSize = 34.sp,
                                lineHeight = 40.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = SfProDisplayFontFamily,
                                letterSpacing = 0.37.sp,
                                color = theme.textPrimary,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "Sua biblioteca definitiva de quadrinhos",
                                fontSize = 15.sp,
                                lineHeight = 20.sp,
                                fontWeight = FontWeight.Normal,
                                fontFamily = SfProTextFontFamily,
                                letterSpacing = (-0.2).sp,
                                color = theme.textSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    // 2. TELA DE RECURSOS: Título Large Title e lista de 4 recursos do HIG
                    if (transitionProgress > 0.01f) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer {
                                    alpha = ((transitionProgress - 0.2f) / 0.8f).coerceIn(0f, 1f)
                                    translationY = (1f - transitionProgress) * 36f
                                },
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Título Apple HIG: Large Title em SF Pro Display Bold
                            Text(
                                text = "Bem-vindo ao\nQuadrinheiro",
                                fontSize = 34.sp,
                                lineHeight = 40.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = SfProDisplayFontFamily,
                                letterSpacing = 0.37.sp,
                                color = theme.textPrimary,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(26.dp))

                            // Lista canônica de recursos da Apple (espaçamento de 18dp entre itens)
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(18.dp)
                            ) {
                                features.forEach { feature ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(46.dp)
                                                .clip(ContinuousRoundedRectangle(13.dp))
                                                .background(
                                                    feature.tintColor.copy(alpha = if (s.isLight) 0.12f else 0.18f)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = feature.icon,
                                                contentDescription = null,
                                                tint = feature.tintColor,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }

                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Text(
                                                text = feature.title,
                                                fontSize = 16.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                fontFamily = SfProTextFontFamily,
                                                letterSpacing = (-0.3).sp,
                                                color = theme.textPrimary
                                            )
                                            Text(
                                                text = feature.subtitle,
                                                fontSize = 14.sp,
                                                lineHeight = 19.sp,
                                                fontWeight = FontWeight.Normal,
                                                fontFamily = SfProTextFontFamily,
                                                letterSpacing = (-0.2).sp,
                                                color = theme.textSecondary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Espaçamento estrito de 24dp imediatamente após "Busca Online e Nuvem"
                Spacer(modifier = Modifier.height(currentButtonTopSpacer))

                // Botão de Ação ("Entrar" -> "Continuar")
                Button(
                    onClick = {
                        if (!isFeaturesView) {
                            isFeaturesView = true
                        } else {
                            onContinue()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag(if (isFeaturesView) "welcome_primary_button" else "welcome_start_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = theme.accent,
                        contentColor = buttonTextColor
                    )
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "Entrar",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = SfProTextFontFamily,
                            letterSpacing = (-0.3).sp,
                            modifier = Modifier.graphicsLayer {
                                alpha = (1f - transitionProgress * 2.2f).coerceIn(0f, 1f)
                                translationY = -transitionProgress * 12f
                            }
                        )
                        Text(
                            text = "Continuar",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = SfProTextFontFamily,
                            letterSpacing = (-0.3).sp,
                            modifier = Modifier.graphicsLayer {
                                alpha = ((transitionProgress - 0.35f) / 0.65f).coerceIn(0f, 1f)
                                translationY = (1f - transitionProgress) * 12f
                            }
                        )
                    }
                }
            }

            // 3. Espaçador da Base (rigorosamente igual ao do topo: distância do topo == distância da base)
            Spacer(modifier = Modifier.height(symmetricPadding))
        }
    }
}
