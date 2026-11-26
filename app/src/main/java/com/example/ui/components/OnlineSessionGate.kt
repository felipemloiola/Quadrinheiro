package com.example.ui.components

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.OnlineSession

@Composable
internal fun OnlineSessionGate(content: @Composable () -> Unit) {
    val context = LocalContext.current.applicationContext
    var connected by remember { mutableStateOf(OnlineSession.isConnected(context)) }
    var started by remember { mutableStateOf(connected) }
    DisposableEffect(context) {
        val manager = context.getSystemService(ConnectivityManager::class.java)
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                connected = OnlineSession.isConnected(context)
            }
            override fun onLost(network: Network) { connected = OnlineSession.isConnected(context) }
        }
        manager.registerDefaultNetworkCallback(callback)
        connected = OnlineSession.isConnected(context)
        onDispose { manager.unregisterNetworkCallback(callback) }
    }
    val focus = LocalFocusManager.current
    LaunchedEffect(connected) {
        if (connected) started = true else focus.clearFocus(force = true)
    }
    Box(Modifier.fillMaxSize()) {
        if (started) Box(if (connected) Modifier else Modifier.clearAndSetSemantics {}) { content() }
        if (!connected) {
            OnlineGateOfflineOverlay(
                onRetry = { connected = OnlineSession.isConnected(context) }
            )
        }
    }
}

@Composable
internal fun OnlineGateOfflineOverlay(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dark = isSystemInDarkTheme()
    val bg = if (dark) Color(0xFF0A0A0D) else Color(0xFFF2F2F7)
    val cardBg = if (dark) Color(0xFF1A1B22) else Color.White
    val primaryText = if (dark) Color.White else Color(0xFF111318)
    val secondaryText = if (dark) Color(0xFFD1D5DB) else Color(0xFF374151)
    val accent = if (dark) Color(0xFF60A5FA) else Color(0xFF0066CC)

    Column(
        modifier
            .fillMaxSize()
            .background(bg)
            .pointerInput(Unit) { awaitPointerEventScope { while (true) awaitPointerEvent().changes.forEach { it.consume() } } }
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(28.dp)
            .testTag("online_session_gate_offline"),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(cardBg)
                .border(1.dp, accent.copy(alpha = 0.35f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.CloudOff,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(36.dp)
            )
        }
        Spacer(Modifier.height(20.dp))
        Text(
            text = "Sem conexão",
            color = primaryText,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics {
                heading()
                liveRegion = LiveRegionMode.Polite
            }
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = "Conecte-se à internet para usar o Quadrinheiro.",
            color = secondaryText,
            fontSize = 15.sp,
            lineHeight = 22.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onRetry,
            shape = RoundedCornerShape(999.dp),
            colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color.White),
            modifier = Modifier
                .widthIn(min = 200.dp)
                .heightIn(min = 48.dp)
                .testTag("online_gate_retry_button")
        ) {
            Text("Tentar novamente", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        }
    }
}
