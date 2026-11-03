package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalIOS27Theme

/** Mirrors the auth header, form and provider actions while the surface is prepared. */
@Composable
fun AuthLoadingSkeleton() {
    val t = LocalIOS27Theme.current
    Column(Modifier.fillMaxSize().background(t.bgPrimary).windowInsetsPadding(WindowInsets.systemBars)
        .padding(horizontal = 18.dp, vertical = 8.dp).testTag("auth_loading_skeleton"), verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally) {
        Column(Modifier.widthIn(max = 420.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            AuthSkeletonBlock(Modifier.size(76.dp)); Spacer(Modifier.height(10.dp))
            AuthSkeletonBlock(Modifier.width(210.dp).height(34.dp)); Spacer(Modifier.height(10.dp))
            AuthSkeletonBlock(Modifier.width(250.dp).height(14.dp)); Spacer(Modifier.height(16.dp))
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(t.textPrimary.copy(alpha = .025f)).padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                AuthSkeletonBlock(Modifier.fillMaxWidth().height(44.dp))
                repeat(2) { AuthSkeletonBlock(Modifier.fillMaxWidth().height(56.dp)) }
                AuthSkeletonBlock(Modifier.align(Alignment.End).width(130.dp).height(20.dp))
                AuthSkeletonBlock(Modifier.fillMaxWidth().height(48.dp)); Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    repeat(2) { AuthSkeletonBlock(Modifier.size(48.dp)) }
                }
            }
        }
    }
}

@Composable
private fun AuthSkeletonBlock(modifier: Modifier) {
    Box(modifier.clip(RoundedCornerShape(14.dp)).background(LocalIOS27Theme.current.textPrimary.copy(alpha = .07f)))
}
