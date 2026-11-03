package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.*
import com.example.ui.theme.*

private data class AppleFeatureRow(val title: String, val subtitle: String, val icon: ImageVector, val tintColor: Color)

/** Each stage measures its visible content; the hidden feature page reserves no space. */
@Composable
fun WelcomeIntroScreen(onContinue: () -> Unit) {
    val motion = LocalAppMotion.current
    val theme = LocalIOS27Theme.current
    val s = LocalAppleMaterials.current.scheme
    var isFeaturesView by remember { mutableStateOf(false) }
    BackHandler(isFeaturesView) { isFeaturesView = false }
    val logoSize by animateDpAsState(if (isFeaturesView) 44.dp else 96.dp, motion.spring(1f, 650f), label = "welcomeLogoSize")
    val features = remember(s.isLight) {
        listOf(
            AppleFeatureRow(
                title = "Sua biblioteca",
                subtitle = "Capas, fichas e sua coleção em um só lugar.",
                icon = LucideIcons.BookOpen,
                tintColor = if (s.isLight) Color(0xFF007AFF) else Color(0xFF0A84FF) // iOS System Blue
            ),
            AppleFeatureRow(
                title = "Descobertas e sequências",
                subtitle = "Encontre sua próxima leitura e acompanhe séries.",
                icon = LucideIcons.Layers,
                tintColor = if (s.isLight) Color(0xFFFF9500) else Color(0xFFFF9F0A) // iOS System Orange
            ),
            AppleFeatureRow(
                title = "Histórico de leitura",
                subtitle = "Registre páginas, notas e resenhas.",
                icon = Icons.Default.BarChart,
                tintColor = if (s.isLight) Color(0xFF34C759) else Color(0xFF30D158) // iOS System Green
            ),
            AppleFeatureRow(
                title = "Conta e sincronização",
                subtitle = "Use sua conta Google ou o modo convidado.",
                icon = Icons.Default.CloudSync,
                tintColor = if (s.isLight) Color(0xFFAF52DE) else Color(0xFFBF5AF2) // iOS System Purple
            )
        )
    }


    BoxWithConstraints(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(theme.bgPrimary,
        androidx.compose.ui.graphics.lerp(theme.bgPrimary, theme.accent, 0.04f), theme.bgPrimary)))
        .windowInsetsPadding(WindowInsets.safeDrawing).padding(horizontal = 28.dp).testTag("welcome_intro_root"),
        contentAlignment = Alignment.Center) {
        val compactWelcome = maxHeight < 680.dp || androidx.compose.ui.platform.LocalDensity.current.fontScale > 1.15f
        Column(Modifier.widthIn(max = 520.dp).fillMaxWidth().heightIn(min = maxHeight)
            .padding(vertical = 18.dp), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center) {
            QuadrinheiroLogo(variant = QuadrinheiroLogoVariant.Primary, titleVisible = false,
                modifier = Modifier.size(logoSize).testTag("welcome_hero_logo"))
            Spacer(Modifier.height(if (isFeaturesView) 12.dp else 20.dp))
            AnimatedContent(targetState = isFeaturesView, transitionSpec = {
                (fadeIn(motion.tween(180)) togetherWith fadeOut(motion.tween(100))).using(
                    SizeTransform(clip = false, sizeAnimationSpec = { _, _ -> motion.spring(1f, 650f) }))
            }, label = "welcomeStage") { featuresVisible ->
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    if (featuresVisible) Text("Bem-vindo ao", fontSize = 25.sp, fontWeight = FontWeight.Bold,
                        color = theme.textPrimary, textAlign = TextAlign.Center)
                    QuadrinheiroBrandText(text = "Quadrinheiro", fontSize = 34.sp, color = theme.textPrimary,
                        modifier = Modifier.testTag("welcome_brand"))
                    if (!featuresVisible) {
                        Spacer(Modifier.height(8.dp))
                        Text("Sua biblioteca definitiva de quadrinhos", fontSize = 15.sp, lineHeight = 21.sp,
                            color = theme.textSecondary, textAlign = TextAlign.Center)
                    } else {
                        Spacer(Modifier.height(16.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(if (compactWelcome) 12.dp else 16.dp)) {
                            features.forEach { feature ->
                                Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.Top) {
                                    Box(Modifier.size(if (compactWelcome) 34.dp else 40.dp).clip(RoundedCornerShape(12.dp))
                                        .background(feature.tintColor.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
                                        Icon(feature.icon, null, tint = feature.tintColor, modifier = Modifier.size(23.dp))
                                    }
                                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                        Text(feature.title, fontSize = 16.sp, lineHeight = 21.sp, fontWeight = FontWeight.SemiBold, color = theme.textPrimary)
                                        if (!compactWelcome) Text(feature.subtitle, fontSize = 14.sp, lineHeight = 20.sp, color = theme.textSecondary)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(if (isFeaturesView) 24.dp else 32.dp))
            Button(onClick = { if (isFeaturesView) onContinue() else isFeaturesView = true },
                modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp)
                    .testTag(if (isFeaturesView) "welcome_primary_button" else "welcome_start_button"),
                shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = theme.accent,
                    contentColor = if (contrastRatio(Color.White, theme.accent) > contrastRatio(Color.Black, theme.accent)) Color.White else Color.Black)) {
                Text("Continuar", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
