package com.example.ui.components

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.clearAndSetSemantics
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
        if(connected) started=true else focus.clearFocus(force=true)
    }
    Box(Modifier.fillMaxSize()) {
      if(started) Box(if(connected) Modifier else Modifier.clearAndSetSemantics {}) { content() }
      if (!connected) {
        val dark = isSystemInDarkTheme()
        Column(Modifier.fillMaxSize().background(if(dark) Color.Black else Color(0xFFF2F2F7))
            .pointerInput(Unit) { awaitPointerEventScope { while(true) awaitPointerEvent().changes.forEach { it.consume() } } }
            .safeDrawingPadding().padding(28.dp), verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Sem conexão", color = if(dark) Color.White else Color.Black, fontSize = 24.sp)
            Spacer(Modifier.height(12.dp))
            Text("Conecte-se à internet para usar o Quadrinheiro.", color = if(dark) Color.LightGray else Color.DarkGray)
        }
    }
    }
}
