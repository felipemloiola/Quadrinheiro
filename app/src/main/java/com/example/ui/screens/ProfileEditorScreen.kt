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
import androidx.compose.ui.graphics.Color
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
    onBack: () -> Unit, onSave: (ReaderProfile, String) -> Unit,
    onHandleChange: (String) -> Unit = {}, handleFeedback: String = "",
    onEditFavorite: (Boolean, Int) -> Unit = { _, _ -> }) {
    val theme = LocalIOS27Theme.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var name by rememberSaveable { mutableStateOf(profile.name) }
    var bio by rememberSaveable { mutableStateOf(profile.bio) }
    var universe by rememberSaveable { mutableStateOf(profile.favoriteUniverse) }
    var preset by rememberSaveable { mutableStateOf(profile.avatarPreset) }
    var handle by rememberSaveable { mutableStateOf(profile.handle) }
    var yearly by rememberSaveable { mutableStateOf(profile.yearlyGoal.toString()) }
    var daily by rememberSaveable { mutableStateOf(profile.dailyPagesGoal.toString()) }
    var avatar by rememberSaveable { mutableStateOf(profile.avatarUrl) }
    var banner by rememberSaveable { mutableStateOf(settings.profileBannerUrl) }
    var bannerSearchOpen by remember { mutableStateOf(false) }
    var imageBusy by remember { mutableStateOf(false) }
    var imageError by remember { mutableStateOf<String?>(null) }
    var pendingBanner by remember { mutableStateOf<Bitmap?>(null) }
    var pendingAvatar by remember { mutableStateOf<Bitmap?>(null) }
    val bannerPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch {
            imageBusy = true; imageError = null
            try { pendingBanner = withContext(Dispatchers.IO) { decodeAvatarBitmapFromUri(context, uri, 2400) } }
            catch (cancel: kotlinx.coroutines.CancellationException) { throw cancel }
            catch (_: Exception) { imageError = "Não foi possível preparar essa imagem. Escolha outra." }
            finally { imageBusy = false }
        }
    }
    val avatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch {
            imageBusy = true; imageError = null
            try { pendingAvatar = withContext(Dispatchers.IO) {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                context.contentResolver.openInputStream(uri)?.use { com.example.data.SafeImageFormats.decodeStreamSafely(it, bounds) }
                if (bounds.outWidth <= 0 || bounds.outHeight <= 0) error("Imagem inválida")
                var sample = 1; while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 1400) sample *= 2
                context.contentResolver.openInputStream(uri)?.use { com.example.data.SafeImageFormats.decodeStreamSafely(it, BitmapFactory.Options().apply { inSampleSize = sample }) }
            } }
            catch (cancel: kotlinx.coroutines.CancellationException) { throw cancel }
            catch (_: Exception) { imageError = "Não foi possível abrir a foto." }
            finally { imageBusy = false }
        }
    }
    BackHandler(onBack = onBack)
    LazyColumn(Modifier.fillMaxSize().testTag("profile_editor"), contentPadding = PaddingValues(20.dp, 12.dp, 20.dp, 200.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                SettingsBackButton(onBack, label = "Perfil")
                Text("Editar perfil", color = theme.textPrimary, fontSize = 18.sp)
                TextButton(enabled = name.isNotBlank() && !imageBusy, onClick = {
                    onSave(profile.copy(name = name.trim().take(80), bio = bio.trim().take(400), avatarUrl = avatar,
                        favoriteUniverse = universe, avatarPreset = preset,
                        yearlyGoal = yearly.toIntOrNull()?.coerceIn(1, 1000) ?: profile.yearlyGoal,
                        dailyPagesGoal = daily.toIntOrNull()?.coerceIn(1, 2000) ?: profile.dailyPagesGoal), banner)
                    onBack()
                }, modifier = Modifier.testTag("save_profile")) { Text("Salvar", color = theme.accent) }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionHeading("Aparência do perfil", "Toque na foto ou escolha uma arte para o banner.")
                val bannerHeight = 132.dp
                val avatarSize = 76.dp
                val halfAvatar = 38.dp
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(bannerHeight + halfAvatar)
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(bannerHeight)
                            .align(Alignment.TopCenter)
                            .clip(RoundedCornerShape(18.dp))
                            .background(theme.bgSecondary)
                            .clickable(enabled = !imageBusy) { bannerSearchOpen = true }
                    ) {
                        if (banner.isNotBlank()) {
                            ProfileImage(banner, "Banner do perfil", Modifier.fillMaxSize())
                        } else {
                            Row(
                                modifier = Modifier.align(Alignment.Center),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddPhotoAlternate,
                                    contentDescription = null,
                                    tint = theme.textTertiary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text("Adicionar banner", color = theme.textTertiary, fontSize = 13.sp)
                            }
                        }
                        if (banner.isNotBlank()) {
                            Row(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .height(30.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.52f))
                                        .clickable(enabled = !imageBusy) {
                                            scope.launch {
                                                imageBusy = true
                                                imageError = null
                                                try {
                                                    pendingBanner = com.example.data.CatalogBanners.bitmapFromBannerValue(banner)
                                                } catch (cancel: kotlinx.coroutines.CancellationException) {
                                                    throw cancel
                                                } catch (_: Exception) {
                                                    imageError = "Não foi possível reenquadrar este banner. Escolha outra arte ou foto."
                                                } finally {
                                                    imageBusy = false
                                                }
                                            }
                                        }
                                        .padding(horizontal = 10.dp)
                                        .testTag("reframe_profile_banner"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(Icons.Default.Crop, contentDescription = "Reenquadrar", tint = Color.White, modifier = Modifier.size(14.dp))
                                        Text("Ajustar", color = Color.White, fontSize = 12.sp, maxLines = 1)
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.52f))
                                        .clickable(enabled = !imageBusy) { banner = "" }
                                        .testTag("remove_profile_banner"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Remover banner", tint = Color.White, modifier = Modifier.size(15.dp))
                                }
                            }
                        }
                        if (imageBusy) {
                            CircularProgressIndicator(
                                Modifier.align(Alignment.Center).size(24.dp),
                                color = theme.accent
                            )
                        }
                    }

                    Box(
                        Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 16.dp)
                            .size(avatarSize)
                            .clip(CircleShape)
                            .background(theme.bgPrimary)
                            .padding(2.5.dp)
                            .clip(CircleShape)
                            .clickable(enabled = !imageBusy) {
                                avatarPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            }
                    ) {
                        UserAvatar(profile.copy(avatarUrl = avatar, avatarPreset = preset), sizeDp = 71)
                    }

                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .height(halfAvatar)
                            .padding(start = 104.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val pillBg = theme.textPrimary.copy(alpha = if (theme.isDark) 0.10f else 0.07f)
                        Box(
                            modifier = Modifier
                                .height(32.dp)
                                .clip(CircleShape)
                                .background(pillBg)
                                .clickable(enabled = !imageBusy) { bannerSearchOpen = true }
                                .padding(horizontal = 12.dp)
                                .testTag("search_profile_banner"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                Icon(Icons.Default.Search, contentDescription = null, tint = theme.textPrimary, modifier = Modifier.size(14.dp))
                                Text("Buscar arte", color = theme.textPrimary, fontSize = 12.5.sp, maxLines = 1)
                            }
                        }
                        Box(
                            modifier = Modifier
                                .height(32.dp)
                                .clip(CircleShape)
                                .background(pillBg)
                                .clickable(enabled = !imageBusy) {
                                    bannerPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                }
                                .padding(horizontal = 12.dp)
                                .testTag("gallery_profile_banner"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = theme.textPrimary, modifier = Modifier.size(14.dp))
                                Text("Galeria", color = theme.textPrimary, fontSize = 12.5.sp, maxLines = 1)
                            }
                        }
                    }
                }
                if (avatar.isBlank()) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    com.example.model.AppleAvatarPreset.entries.forEach { choice ->
                        Box(Modifier.size(44.dp).clip(CircleShape).clickable { preset = choice.id }, contentAlignment = Alignment.Center) {
                            UserAvatar(profile.copy(avatarUrl = "", avatarPreset = choice.id), sizeDp = 36)
                        }
                    }
                }
                if (imageError != null) Text(imageError!!, color = theme.textSecondary, fontSize = 13.sp)
            }
        }
        item { SectionHeading("Informações") }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Nome de usuário", color = theme.textSecondary, fontSize = 13.sp)
                AppleTextField(handle, { handle = it.take(60); onHandleChange(handle) }, "@usuário", useContentMaterial = true)
                if (handleFeedback.isNotBlank()) Text(handleFeedback, color = theme.textSecondary, fontSize = 12.sp)
            }
        }
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
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionHeading("Recomendações")
                Row(Modifier.fillMaxWidth().contentMaterialSurface(RoundedCornerShape(20.dp)).padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Editora ou universo", Modifier.weight(1f), color = theme.textPrimary, fontSize = 15.sp)
                    Box(Modifier.widthIn(max = 190.dp).heightIn(min = 44.dp).testTag("favorite_universe_picker"), contentAlignment = Alignment.CenterEnd) {
                        AppleMenuPicker(value = universe, onChange = { universe = it }, options = listOf("DC / Vertigo", "Marvel", "Image", "Dark Horse", "Mangás", "Quadrinhos nacionais", "Independentes").map { SegmentOption(it, it) })
                    }
                }
                Text("Sua preferência ajuda nas sugestões.", Modifier.padding(horizontal = 16.dp), color = theme.textSecondary, fontSize = 12.sp)
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionHeading("Favoritos")
                listOf(false to "HQs favoritas", true to "Personagens favoritos").forEach { (characters, label) ->
                    Text(label, color = theme.textPrimary)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { repeat(4) { slot ->
                        val name = if (characters) settings.profileFavoriteCharacters.getOrNull(slot) else settings.profileFavoriteComicIds.getOrNull(slot)
                        val source = if (characters) settings.profileShowcaseDetails["character:$name"].orEmpty() else settings.profileShowcaseDetails["comic:$name"]?.let(com.example.data.ProfileShowcase::restoreComic)?.coverUrl.orEmpty()
                        Box(Modifier.weight(1f).aspectRatio(if (characters) 1f else 2f / 3f).clip(RoundedCornerShape(12.dp)).background(theme.bgSecondary).clickable { onEditFavorite(characters, slot) }, contentAlignment = Alignment.Center) {
                            if (source.isNotBlank()) ProfileImage(source, name, Modifier.fillMaxSize(), face = characters)
                            else Icon(Icons.Default.Add, "Escolher ${slot + 1}", tint = theme.textSecondary)
                        }
                    } }
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionHeading("Leitura")
                Column(Modifier.fillMaxWidth().contentMaterialSurface(RoundedCornerShape(20.dp)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) { Text("Meta anual", color = theme.textPrimary, fontSize = 15.sp); Text("$yearly HQs", color = theme.textSecondary, fontSize = 13.sp) }
                        AppleStepper(yearly.toIntOrNull() ?: 1, min = 1, max = 1000, onChange = { yearly = it.toString() })
                    }
                    HorizontalDivider(color = theme.divider)
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) { Text("Meta diária", color = theme.textPrimary, fontSize = 15.sp); Text("$daily páginas", color = theme.textSecondary, fontSize = 13.sp) }
                        AppleStepper(daily.toIntOrNull() ?: 1, min = 1, max = 2000, onChange = { daily = it.toString() })
                    }
                }
            }
        }
    }
    if (bannerSearchOpen) {
        val favoriteTitle = settings.profileFavoriteComicIds.firstOrNull()?.let { settings.profileShowcaseDetails["comic:$it"]?.let(com.example.data.ProfileShowcase::restoreComic)?.displayTitle }.orEmpty()
        val character = settings.profileFavoriteCharacters.firstOrNull().orEmpty().substringBefore('(').trim()
        BannerArtworkPicker(initialQuery = favoriteTitle.ifBlank { character }, initialKind = if (favoriteTitle.isBlank() && character.isNotBlank()) "character" else "volume",
            onClose = { bannerSearchOpen = false },
            onGallery = { bannerSearchOpen = false; bannerPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            onChoose = { art ->
                bannerSearchOpen = false
                scope.launch {
                    imageBusy = true; imageError = null
                    try { pendingBanner = com.example.data.CatalogBanners.bitmap(art.url) }
                    catch (cancel: kotlinx.coroutines.CancellationException) { throw cancel }
                    catch (_: Exception) { imageError = "Não foi possível preparar essa arte. Escolha outra ou use a galeria." }
                    finally { imageBusy = false }
                }
            })
    }
    pendingBanner?.let { bitmap -> BannerCropModal(bitmap, { pendingBanner = null }, { banner = it; pendingBanner = null }) }
    pendingAvatar?.let { bitmap -> ProfilePhotoCropModal(bitmap, onDismiss = { pendingAvatar = null }, onConfirmCrop = { avatar = it; pendingAvatar = null }) }
}
