package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CloudCacheRecovery
import com.example.ui.components.IOSLoadingIndicator
import com.example.ui.components.LiquidGlassDialog
import com.example.ui.components.LiquidGlassPillButton
import com.example.ui.components.LocalLiquidGlassBackdrop
import com.example.ui.components.liquidGlass
import com.example.ui.components.rememberAdaptiveLiquidGlassInk
import com.example.ui.theme.LocalAppSettings
import com.example.ui.theme.LocalIOS27Theme
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.shapes.ContinuousRoundedRectangle
import kotlinx.coroutines.launch

/** No ViewModel or cloud listener is created behind this screen. */
@Composable
internal fun CloudCacheRecoveryScreen(onContinue: () -> Unit) {
    val context = LocalContext.current
    val theme = LocalIOS27Theme.current
    val settings = LocalAppSettings.current
    val scope = rememberCoroutineScope()
    var confirm by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var repaired by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    val backdrop = rememberLayerBackdrop()
    val cardShape = remember { ContinuousRoundedRectangle(28.dp) }

    CompositionLocalProvider(LocalLiquidGlassBackdrop provides backdrop) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .testTag("cloud_cache_recovery_screen"),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .layerBackdrop(backdrop)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                theme.bgPrimary,
                                lerp(theme.bgPrimary, theme.accent, if (settings.reduceTransparency) 0.03f else 0.12f),
                                theme.bgPrimary
                            )
                        )
                    )
            )

            val cardModifier = if (settings.reduceTransparency) {
                Modifier
                    .clip(cardShape)
                    .background(theme.bgSecondary, cardShape)
                    .border(1.dp, theme.divider, cardShape)
            } else {
                Modifier.liquidGlass(shape = cardShape, elevation = 12.dp)
            }

            Column(
                Modifier
                    .fillMaxWidth()
                    .widthIn(max = 480.dp)
                    .safeDrawingPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 24.dp)
                    .then(cardModifier)
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    if (repaired) "Sincronização preparada" else "Revisar sincronização",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = theme.textPrimary,
                    modifier = Modifier.semantics { heading() }
                )
                Text(
                    if (repaired) "Seu acervo foi preservado. Uma cópia de segurança ficou guardada neste aparelho. Ao continuar, os registros locais serão reconciliados com sua conta."
                    else "O armazenamento de sincronização cresceu muito e pode causar travamentos. Seu acervo continua salvo. Podemos guardar uma cópia de segurança e reconstruir essa fila.",
                    color = theme.textSecondary,
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(Modifier.height(4.dp))
                if (busy) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IOSLoadingIndicator(
                            color = theme.accent,
                            modifier = Modifier
                                .size(22.dp)
                                .semantics {
                                    contentDescription = "Preservando dados e preparando sincronização"
                                    liveRegion = LiveRegionMode.Polite
                                }
                        )
                        Text(
                            "Preservando os dados e preparando a sincronização…",
                            color = theme.textSecondary,
                            fontSize = 14.sp,
                            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                        )
                    }
                } else if (repaired) {
                    LiquidGlassPillButton(
                        text = "Continuar",
                        onClick = onContinue,
                        emphasized = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("cloud_recovery_continue")
                    )
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        LiquidGlassPillButton(
                            text = "Preparar recuperação",
                            onClick = { confirm = true },
                            emphasized = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("cloud_recovery_prepare")
                        )
                        LiquidGlassPillButton(
                            text = "Continuar sem reparar",
                            onClick = { CloudCacheRecovery.continueWithoutRepair(); onContinue() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("cloud_recovery_skip")
                        )
                    }
                }
                if (failed) {
                    Text(
                        "Não foi possível concluir a recuperação. Os dados originais e qualquer cópia já criada foram preservados.",
                        color = theme.textSecondary,
                        fontSize = 13.sp,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                    )
                }
            }
        }
    }

    if (confirm) {
        LiquidGlassDialog(
            onDismissRequest = { confirm = false },
            maxWidth = 420.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Reconstruir a fila local?",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = theme.textPrimary,
                    modifier = Modifier.semantics { heading() }
                )
                Text(
                    text = "Primeiro será guardada uma cópia privada do estado local e do banco de sincronização. Depois, a fila antiga do Firestore será substituída e os registros salvos no app serão reenviados. Operações antigas que existam apenas nessa fila, como exclusões ainda não confirmadas, podem precisar de revisão. Sua conta não será desconectada.",
                    color = theme.textSecondary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    LiquidGlassPillButton(
                        text = "Criar cópia e reconstruir",
                        onClick = {
                            confirm = false; busy = true; failed = false
                            scope.launch {
                                try {
                                    CloudCacheRecovery.repair(context)
                                    repaired = true
                                } catch (cancel: kotlinx.coroutines.CancellationException) {
                                    throw cancel
                                } catch (_: Exception) {
                                    failed = true
                                } finally {
                                    busy = false
                                }
                            }
                        },
                        emphasized = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    LiquidGlassPillButton(
                        text = "Cancelar",
                        onClick = { confirm = false },
                        contentTint = rememberAdaptiveLiquidGlassInk(theme.textSecondary, theme.bgSecondary, 4.5f),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
