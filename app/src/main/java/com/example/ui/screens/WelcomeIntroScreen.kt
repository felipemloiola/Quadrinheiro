package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Spring
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
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
 * 1. Arquitetura 100% Contínua e Coreografada: A logo sobe com prioridade de camada (z-index),
 *    os textos iniciais desvanecem imediatamente e o título "Bem-vindo" junto aos recursos
 *    surgem com delay elegante (apenas após a logo desimpedir o caminho central).
 * 2. Simetria Vertical Estrita: A distância do topo da tela à logo e da base do botão à borda
 *    inferior são rigorosamente iguais, centralizando o conteúdo em qualquer proporção de tela.
 * 3. Botão Coeso a 24dp: O botão de ação fica a exatamente 24dp abaixo de "Busca Online e Nuvem".
 * 4. Animação Fluida iOS: Logo sobe suavemente e desescala, botão desce suavemente e textos transitam
 *    com física de mola crítica do iOS (Spring.DampingRatioNoBouncy).
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

    // Curva de mola contínua, crítica e ultra-fluida do iOS (sem saltos de limiar, sem overshoot brusco)
    val transitionProgress by animateFloatAsState(
        targetValue = if (isFeaturesView) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = 300f
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

    // Distâncias de viagem calculadas estritamente em pixels para execução via RenderThread (GPU)
    val logoTravelPx = with(density) { 152.dp.toPx() }
    val buttonTravelPx = with(density) { 266.dp.toPx() }
    val splashTravelPx = with(density) { 152.dp.toPx() }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
            .padding(horizontal = 28.dp)
            .testTag("welcome_intro_root"),
        contentAlignment = Alignment.Center
    ) {
        // Container principal que centraliza perfeitamente no eixo vertical
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 520.dp)
                .verticalScroll(scrollState, enabled = isFeaturesView),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Logo Oficial (escala e viaja suavemente do centro para o topo com prioridade de camada zIndex)
            QuadrinheiroLogo(
                variant = QuadrinheiroLogoVariant.Primary,
                titleVisible = false,
                modifier = Modifier
                    .zIndex(10f)
                    .size(60.dp)
                    .graphicsLayer {
                        val p = transitionProgress.coerceIn(0f, 1f)
                        translationY = (1f - p) * logoTravelPx
                        val scale = 1.55f - 0.55f * p
                        scaleX = scale
                        scaleY = scale
                    }
                    .testTag("welcome_hero_logo")
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 2. Área Central de Conteúdo (layout fixo, transições 100% por opacidade e deslocamento GPU)
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.TopCenter
            ) {
                // Conteúdo da Tela 2: Recursos Canônicos (revelados com fade apenas após a logo desimpedir o caminho)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            val p = transitionProgress.coerceIn(0f, 1f)
                            // Abertura escalonada: surge graciosamente a partir de p >= 0.44f
                            val contentProgress = ((p - 0.44f) / 0.56f).coerceIn(0f, 1f)
                            alpha = contentProgress
                            translationY = (1f - contentProgress) * 22f
                        },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
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

                    Spacer(modifier = Modifier.height(24.dp))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
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

                // Conteúdo da Tela 1: Textos da Capa (desvanecem rapidamente na saída liberando a área)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            val p = transitionProgress.coerceIn(0f, 1f)
                            translationY = (1f - p) * splashTravelPx
                            // Desvanece nos primeiros 30% da animação para deixar o caminho livre para a subida da logo
                            alpha = (1f - p * 3.3f).coerceIn(0f, 1f)
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

            // Espaçamento estrito de 24dp imediatamente após "Busca Online e Nuvem"
            Spacer(modifier = Modifier.height(24.dp))

            // 3. Botão de Ação ("Entrar" -> "Continuar") com descida e transição suaves
            Button(
                onClick = {
                    if (!isFeaturesView) {
                        isFeaturesView = true
                    } else if (transitionProgress >= 0.85f) {
                        onContinue()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .graphicsLayer {
                        val p = transitionProgress.coerceIn(0f, 1f)
                        translationY = -(1f - p) * buttonTravelPx
                    }
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
                            val p = transitionProgress.coerceIn(0f, 1f)
                            alpha = (1f - p * 3f).coerceIn(0f, 1f)
                            translationY = -p * 8f
                        }
                    )
                    Text(
                        text = "Continuar",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = SfProTextFontFamily,
                        letterSpacing = (-0.3).sp,
                        modifier = Modifier.graphicsLayer {
                            val p = transitionProgress.coerceIn(0f, 1f)
                            val contentProgress = ((p - 0.44f) / 0.56f).coerceIn(0f, 1f)
                            alpha = contentProgress
                            translationY = (1f - contentProgress) * 8f
                        }
                    )
                }
            }
        }
    }
}
