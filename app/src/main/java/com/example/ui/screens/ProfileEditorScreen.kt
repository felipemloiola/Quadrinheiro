package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ReaderProfile
import com.example.model.AppSettings
import com.example.ui.components.*
import com.example.ui.theme.LocalIOS27Theme
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
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
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    val isScrolled by remember(listState) {
        androidx.compose.runtime.derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 24
        }
    }

    val editorBackdrop = rememberLayerBackdrop()
    val bannerBackdrop = rememberLayerBackdrop()

    Box(Modifier.fillMaxSize().background(theme.bgPrimary)) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(editorBackdrop)
                .testTag("profile_editor"),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 76.dp, bottom = 200.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                Text(
                    text = "Editar perfil",
                    color = theme.textPrimary,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp)
                )
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
                        Box(
                            Modifier
                                .fillMaxSize()
                                .layerBackdrop(bannerBackdrop)
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
                        }
                        if (banner.isNotBlank()) {
                            Row(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                LiquidGlassPillButton(
                                    text = "Ajustar",
                                    onClick = {
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
                                    },
                                    enabled = !imageBusy,
                                    leadingIcon = Icons.Default.Crop,
                                    onDarkScrim = true,
                                    minHeight = 40.dp,
                                    horizontalPadding = 14.dp,
                                    verticalPadding = 8.dp,
                                    backdropOverride = bannerBackdrop,
                                    modifier = Modifier.testTag("reframe_profile_banner")
                                )
                                LiquidGlassCircleButton(
                                    icon = Icons.Default.Close,
                                    contentDescription = "Remover banner",
                                    onClick = { banner = "" },
                                    size = 40.dp,
                                    iconSize = 18.dp,
                                    onDarkScrim = true,
                                    enabled = !imageBusy,
                                    tag = "remove_profile_banner",
                                    backdropOverride = bannerBackdrop
                                )
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
                            .heightIn(min = halfAvatar)
                            .padding(start = 104.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LiquidGlassPillButton(
                            text = "Buscar arte",
                            onClick = { bannerSearchOpen = true },
                            enabled = !imageBusy,
                            leadingIcon = Icons.Default.Search,
                            minHeight = 40.dp,
                            horizontalPadding = 12.dp,
                            verticalPadding = 8.dp,
                            modifier = Modifier.testTag("search_profile_banner")
                        )
                        LiquidGlassPillButton(
                            text = "Galeria",
                            onClick = {
                                bannerPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                            enabled = !imageBusy,
                            leadingIcon = Icons.Default.PhotoLibrary,
                            minHeight = 40.dp,
                            horizontalPadding = 12.dp,
                            verticalPadding = 8.dp,
                            modifier = Modifier.testTag("gallery_profile_banner")
                        )
                    }
                }
                if (avatar.isBlank()) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    com.example.model.AppleAvatarPreset.entries.forEach { choice ->
                        Box(
                            Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .clickable(
                                    role = androidx.compose.ui.semantics.Role.RadioButton,
                                    onClickLabel = "Selecionar avatar ${choice.id}"
                                ) { preset = choice.id },
                            contentAlignment = Alignment.Center
                        ) {
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
        CompositionLocalProvider(LocalLiquidGlassBackdrop provides editorBackdrop) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SettingsBackButton(onBack, label = "Perfil", collapsed = isScrolled)
                AnimatedVisibility(
                    visible = isScrolled,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Text(
                        "Editar perfil",
                        color = theme.textPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                LiquidGlassPillButton(
                    text = "Salvar",
                    enabled = name.isNotBlank() && !imageBusy,
                    emphasized = true,
                    horizontalPadding = 18.dp,
                    verticalPadding = 10.dp,
                    onClick = {
                        onSave(
                            profile.copy(
                                name = name.trim().take(80),
                                bio = bio.trim().take(400),
                                avatarUrl = avatar,
                                favoriteUniverse = universe,
                                avatarPreset = preset,
                                yearlyGoal = yearly.toIntOrNull()?.coerceIn(1, 1000) ?: profile.yearlyGoal,
                                dailyPagesGoal = daily.toIntOrNull()?.coerceIn(1, 2000) ?: profile.dailyPagesGoal
                            ),
                            banner
                        )
                        onBack()
                    },
                    modifier = Modifier.testTag("save_profile")
                )
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
