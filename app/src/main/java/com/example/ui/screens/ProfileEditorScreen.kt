package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ReaderProfile
import com.example.model.AppSettings
import com.example.ui.components.*
import com.example.ui.theme.LocalIOS27Theme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun ProfileEditorScreen(profile: ReaderProfile, settings: AppSettings,
    onBack: () -> Unit, onSave: (ReaderProfile, String) -> Unit) {
    val theme = LocalIOS27Theme.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var name by rememberSaveable { mutableStateOf(profile.name) }
    var bio by rememberSaveable { mutableStateOf(profile.bio) }
    var universe by rememberSaveable { mutableStateOf(profile.favoriteUniverse) }
    var avatar by rememberSaveable { mutableStateOf(profile.avatarUrl) }
    var banner by rememberSaveable { mutableStateOf(settings.profileBannerUrl) }
    var imageBusy by remember { mutableStateOf(false) }
    var imageError by remember { mutableStateOf<String?>(null) }
    var pendingAvatar by remember { mutableStateOf<Bitmap?>(null) }
    val bannerPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch {
            imageBusy = true; imageError = null
            try { banner = prepareProfileBanner(context, uri) }
            catch (cancel: kotlinx.coroutines.CancellationException) { throw cancel }
            catch (e: Exception) { imageError = "Não foi possível preparar essa imagem. Escolha outra." }
            finally { imageBusy = false }
        }
    }
    val avatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch {
            imageBusy = true; imageError = null
            try { pendingAvatar = withContext(Dispatchers.IO) {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                context.contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, bounds) }
                var sample = 1; while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 1400) sample *= 2
                context.contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample }) }
            } }
            catch (e: Exception) { imageError = "Não foi possível abrir a foto." }
            finally { imageBusy = false }
        }
    }
    BackHandler(onBack = onBack)
    LazyColumn(Modifier.fillMaxSize().testTag("profile_editor"), contentPadding = PaddingValues(20.dp, 12.dp, 20.dp, 200.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                SettingsBackButton(onBack, label = "Perfil")
                Text("Editar perfil", color = theme.textPrimary, fontSize = 18.sp)
                TextButton(enabled = name.isNotBlank() && !imageBusy, onClick = {
                    onSave(profile.copy(name = name.trim().take(80), bio = bio.trim().take(400), avatarUrl = avatar, favoriteUniverse = universe), banner)
                    onBack()
                }, modifier = Modifier.testTag("save_profile")) { Text("Salvar", color = theme.accent) }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionHeading("Aparência do perfil")
                Box(Modifier.fillMaxWidth().height(154.dp).clip(RoundedCornerShape(20.dp)).background(theme.bgSecondary)) {
                    if (banner.isNotBlank()) ProfileImage(banner, "Banner do perfil", Modifier.fillMaxSize())
                    Box(Modifier.align(Alignment.TopEnd).padding(10.dp).size(44.dp).clip(CircleShape).background(theme.bgSecondary.copy(alpha = .9f))
                        .clickable(enabled = !imageBusy) { bannerPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.AddPhotoAlternate, "Escolher banner", tint = theme.textPrimary)
                    }
                    Box(Modifier.align(Alignment.BottomStart).padding(12.dp).clip(CircleShape)
                        .clickable(enabled = !imageBusy) { avatarPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                        UserAvatar(profile.copy(avatarUrl = avatar), sizeDp = 68)
                    }
                    if (imageBusy) CircularProgressIndicator(Modifier.align(Alignment.Center).size(24.dp), color = theme.accent)
                }
                if (banner.isNotBlank()) TextButton(onClick = { banner = "" }) { Text("Remover banner", color = theme.accent) }
                if (imageError != null) Text(imageError!!, color = theme.textSecondary, fontSize = 13.sp)
            }
        }
        item { SectionHeading("Informações") }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Nome", color = theme.textSecondary, fontSize = 13.sp)
                AppleTextField(name, { name = it.take(80) }, "Nome de exibição", useContentMaterial = true)
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Biografia", color = theme.textSecondary, fontSize = 13.sp)
                AppleTextEditor(bio, { bio = it.take(400) }, "Conte um pouco sobre você")
            }
        }
        item {
            Column(Modifier.fillMaxWidth().contentMaterialSurface(RoundedCornerShape(20.dp)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Editora ou universo favorito", color = theme.textPrimary, fontSize = 15.sp)
                AppleMenuPicker(value = universe, onChange = { universe = it }, options = listOf("DC / Vertigo", "Marvel", "Image", "Dark Horse", "Mangás", "Quadrinhos nacionais", "Independentes").map { SegmentOption(it, it) })
                Text("Também ajuda nas suas recomendações.", color = theme.textSecondary, fontSize = 12.sp)
            }
        }
    }
    pendingAvatar?.let { bitmap -> ProfilePhotoCropModal(bitmap, onDismiss = { pendingAvatar = null }, onConfirmCrop = { avatar = it; pendingAvatar = null }) }
}
