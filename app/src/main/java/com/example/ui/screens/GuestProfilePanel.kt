package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.contentMaterialSurface
import com.example.ui.components.CurationGlassAction
import com.example.ui.theme.LocalIOS27Theme
import com.kyant.shapes.ContinuousRoundedRectangle

@Composable
internal fun GuestProfilePanel(
    onBack: () -> Unit,
    onLogin: () -> Unit,
    onRegister: () -> Unit,
    onLogout: (() -> Unit)? = null
) {
    val theme = LocalIOS27Theme.current
    BackHandler(enabled = !com.example.ui.components.LocalPageBackManaged.current, onBack = onBack)
    val scroll = rememberScrollState()
    val backCollapsed by rememberScrollCollapsed(scroll)
    Box(Modifier.fillMaxSize().statusBarsPadding()) {
        Column(Modifier.fillMaxSize().verticalScroll(scroll).padding(start = 20.dp, end = 20.dp, top = 68.dp, bottom = 24.dp)) {
        Column(
            modifier = Modifier.fillMaxWidth().contentMaterialSurface(ContinuousRoundedRectangle(26.dp)).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(Icons.Outlined.PersonOutline, contentDescription = null, tint = theme.textPrimary, modifier = Modifier.size(48.dp))
            Text("Convidado", color = theme.textPrimary, fontSize = 24.sp)
            Text("Entre ou crie uma conta para personalizar seu perfil.", color = theme.textSecondary)
            CurationGlassAction("Entrar", onLogin, Modifier.fillMaxWidth(), primary = true)
            CurationGlassAction("Criar conta", onRegister, Modifier.fillMaxWidth())
            if (onLogout != null) {
                TextButton(onClick = onLogout) {
                    Text("Sair do Modo Convidado", color = Color(0xFFFF453A))
                }
            }
        }
        }
        SettingsBackButton(onBack, Modifier.align(Alignment.TopStart).padding(start = 20.dp, top = 12.dp),
            collapsed = backCollapsed, label = "Ajustes")
    }
}
