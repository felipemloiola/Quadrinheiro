package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.contentMaterialSurface
import com.example.ui.theme.LocalIOS27Theme

@Composable
internal fun GuestProfilePanel(
    onBack: () -> Unit,
    onLogin: () -> Unit,
    onRegister: () -> Unit,
    onLogout: (() -> Unit)? = null
) {
    val theme = LocalIOS27Theme.current
    BackHandler(onBack = onBack)
    Column(
        modifier = Modifier.fillMaxSize().statusBarsPadding().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        TextButton(onClick = onBack) { Text("Voltar", color = theme.accent) }
        Column(
            modifier = Modifier.fillMaxWidth().contentMaterialSurface(RoundedCornerShape(26.dp)).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(Icons.Outlined.PersonOutline, contentDescription = null, tint = theme.textPrimary, modifier = Modifier.size(48.dp))
            Text("Convidado", color = theme.textPrimary, fontSize = 24.sp)
            Text("Entre ou crie uma conta para personalizar seu perfil.", color = theme.textSecondary)
            Button(
                onClick = onLogin,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = theme.accent,
                    contentColor = if (theme.accent.luminance() > 0.179f) Color.Black else Color.White
                )
            ) { Text("Entrar") }
            TextButton(onClick = onRegister) { Text("Criar conta", color = theme.accent) }
            if (onLogout != null) {
                TextButton(onClick = onLogout) {
                    Text("Sair do Modo Convidado", color = Color(0xFFFF453A))
                }
            }
        }
    }
}
