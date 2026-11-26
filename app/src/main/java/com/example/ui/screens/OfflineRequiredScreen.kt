package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.pressScaleFeedback
import com.example.ui.components.tintedLiquidBlueButton
import com.example.ui.theme.LocalAppSettings
import com.example.ui.theme.LocalIOS27Theme
import com.example.ui.theme.contrastRatio
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun OfflineRequiredScreen(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val theme = LocalIOS27Theme.current
    val settings = LocalAppSettings.current
    val scope = rememberCoroutineScope()
    var isChecking by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(theme.bgPrimary)
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(24.dp)
            .testTag("offline_required_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 460.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(
                        if (settings.reduceTransparency) {
                            Brush.linearGradient(listOf(theme.bgSecondary, theme.bgSecondary))
                        } else {
                            Brush.radialGradient(
                                listOf(
                                    theme.accent.copy(alpha = 0.22f),
                                    theme.accent.copy(alpha = 0.04f),
                                    Color.Transparent
                                )
                            )
                        }
                    )
                    .padding(8.dp)
                    .clip(CircleShape)
                    .background(theme.bgSecondary)
                    .border(1.dp, theme.divider, CircleShape)
                    .padding(18.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.CloudOff,
                    contentDescription = null,
                    tint = theme.accent,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(Modifier.height(28.dp))

            Text(
                text = "Sem conexão com a internet",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = theme.textPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics {
                    heading()
                    liveRegion = LiveRegionMode.Polite
                }
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text = "O Quadrinheiro opera 100% online para manter seu acervo, histórico e resenhas seguros na nuvem sem consumir o armazenamento do seu aparelho.",
                fontSize = 14.5.sp,
                lineHeight = 21.sp,
                color = theme.textSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(Modifier.height(36.dp))

            val retryInteraction = remember { MutableInteractionSource() }
            val solidBtnColor = theme.accent
            val solidBtnFg = if (contrastRatio(Color.White, solidBtnColor) >= contrastRatio(Color.Black, solidBtnColor)) Color.White else Color.Black
            val buttonSurface = if (settings.reduceTransparency) {
                Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(solidBtnColor)
            } else {
                Modifier.tintedLiquidBlueButton()
            }
            val buttonFg = if (settings.reduceTransparency) solidBtnFg else Color.White

            Box(
                modifier = Modifier
                    .fillMaxWidth(0.72f)
                    .heightIn(min = 50.dp)
                    .pressScaleFeedback(retryInteraction, targetScale = 0.96f)
                    .then(buttonSurface)
                    .focusable(enabled = !isChecking)
                    .semantics(mergeDescendants = true) {
                        if (isChecking) {
                            contentDescription = "Verificando conexão"
                            liveRegion = LiveRegionMode.Polite
                        }
                    }
                    .clickable(
                        interactionSource = retryInteraction,
                        indication = null,
                        role = Role.Button,
                        onClickLabel = "Tentar conectar novamente",
                        enabled = !isChecking
                    ) {
                        scope.launch {
                            isChecking = true
                            onRetry()
                            delay(600)
                            isChecking = false
                        }
                    }
                    .padding(horizontal = 18.dp, vertical = 12.dp)
                    .testTag("retry_connection_button"),
                contentAlignment = Alignment.Center
            ) {
                if (isChecking) {
                    CircularProgressIndicator(
                        color = buttonFg,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Text(
                        text = "Tentar novamente",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = buttonFg
                    )
                }
            }
        }
    }
}
