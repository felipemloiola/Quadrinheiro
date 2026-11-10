package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.pressScaleFeedback
import com.example.ui.components.tintedLiquidBlueButton
import com.example.ui.theme.LocalIOS27Theme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun OfflineRequiredScreen(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val theme = LocalIOS27Theme.current
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
            modifier = Modifier.fillMaxWidth()
        ) {
            // Optical Glass Badge
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                theme.accent.copy(alpha = 0.22f),
                                theme.accent.copy(alpha = 0.04f),
                                Color.Transparent
                            )
                        )
                    )
                    .padding(8.dp)
                    .clip(CircleShape)
                    .background(theme.bgSecondary)
                    .padding(18.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.CloudOff,
                    contentDescription = "Sem conexão",
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
                textAlign = TextAlign.Center
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
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.68f)
                    .height(50.dp)
                    .pressScaleFeedback(retryInteraction, targetScale = 0.96f)
                    .tintedLiquidBlueButton()
                    .clickable(
                        interactionSource = retryInteraction,
                        indication = null,
                        role = Role.Button,
                        enabled = !isChecking
                    ) {
                        scope.launch {
                            isChecking = true
                            onRetry()
                            delay(600)
                            isChecking = false
                        }
                    }
                    .testTag("retry_connection_button"),
                contentAlignment = Alignment.Center
            ) {
                if (isChecking) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Text(
                        text = "Tentar novamente",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }
        }
    }
}
