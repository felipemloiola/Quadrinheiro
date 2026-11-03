package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.LauncherAppearance
import com.example.LauncherIconStyle
import com.example.R
import com.example.ui.theme.LocalIOS27Theme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LauncherIconPicker() {
    val context = LocalContext.current
    val theme = LocalIOS27Theme.current
    val scope = rememberCoroutineScope()
    var selected by remember { mutableStateOf(LauncherAppearance.selected(context)) }
    var error by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().contentMaterialSurface(RoundedCornerShape(24.dp)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Ícone do app", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = theme.textPrimary)
        Text("Escolha o fundo. A marca mantém suas cores.", fontSize = 13.sp, color = theme.textSecondary)
        FlowRow(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            LauncherIconStyle.entries.forEach { style ->
                Column(Modifier.width(68.dp).testTag("launcher-icon-${style.id}")
                    .selectable(selected == style, role = Role.RadioButton, onClick = {
                        val previous = selected
                        selected = style
                        error = false
                        scope.launch {
                            try { withContext(Dispatchers.IO) { LauncherAppearance.select(context, style) } }
                            catch (e: Exception) { selected = previous; error = true }
                        }
                    }).padding(vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(Modifier.size(58.dp).border(if (selected == style) 2.dp else 0.dp,
                        if (selected == style) theme.accent else Color.Transparent, RoundedCornerShape(16.dp)).padding(4.dp)) {
                        LauncherIconPreview(style, Modifier.fillMaxSize())
                    }
                    Text(style.label, fontSize = 12.sp, maxLines = 1, color = theme.textPrimary)
                }
            }
        }
        if (error) Text("Não foi possível alterar o ícone. Tente novamente.", fontSize = 12.sp, color = theme.textSecondary)
    }
}

@Composable
internal fun LauncherIconPreview(style: LauncherIconStyle, modifier: Modifier = Modifier) {
    val night = isSystemInDarkTheme()
    val background = if (style == LauncherIconStyle.DEFAULT && night) Color.Black else Color(style.background)
    Box(modifier.clip(RoundedCornerShape(13.dp)).background(background)) {
        Image(painterResource(R.drawable.ic_launcher_foreground), null, Modifier.fillMaxSize().graphicsLayer { scaleX = 1.5f; scaleY = 1.5f })
    }
}
