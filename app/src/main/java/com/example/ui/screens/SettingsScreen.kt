package com.example.ui.screens

import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.AccessibilityNew

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import coil.compose.AsyncImage
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ApiDiagnosticsReport
import com.example.model.AppSettings
import com.example.model.AppleAvatarPreset
import com.example.model.AuthMode
import com.example.model.AuthUiState
import com.example.model.ComicEntry
import com.example.model.IOS27ThemeDef
import com.example.model.ReaderProfile
import com.example.model.ReadingStatus
import com.example.model.UsernameAvailabilityStatus
import com.example.model.UsernameValidator
import com.example.ui.theme.AppTheme
import com.example.ui.theme.LocalIOS27Theme
import com.example.ui.components.AppleMenuPicker
import com.example.ui.components.AppleSegmentedPicker
import com.example.ui.components.AppleSlider
import com.example.ui.components.AppleStepper
import com.example.ui.components.AppleTextField
import com.example.ui.components.AppleToggle
import com.example.ui.components.IOS27LiquidGlassController
import com.example.ui.components.LiquidGlassModeSwitcher
import com.example.ui.components.LucideIcons
import com.example.ui.components.QuadrinheiroLogo
import com.example.ui.components.SegmentOption
import com.example.ui.components.SettingsSquircleIcon
import com.example.ui.components.UserAvatar
import com.example.ui.components.contentMaterialSurface
import com.example.ui.components.inputWellSurface
import com.example.ui.components.liquidGlass
import com.example.ui.components.liquidGlassNavSurface
import com.example.ui.theme.LocalAppleMaterials
import com.example.ui.theme.SpaceGroteskTabularFamily
import com.example.viewmodel.QuadrinheiroViewModel
import java.io.ByteArrayOutputStream
import kotlin.math.min
import kotlin.math.roundToInt

import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsTabScreen(
    viewModel: QuadrinheiroViewModel,
    comics: List<ComicEntry>,
    profile: ReaderProfile,
    appSettings: AppSettings,
    settingsSubPage: String?,
    settingsParentPage: String?,
    apiDiagnostics: ApiDiagnosticsReport?,
    isTestingApis: Boolean,
    listState: LazyListState,
    onOpenSidebar: () -> Unit,
    onQuickRegister: () -> Unit
) {
    val mat = LocalAppleMaterials.current
    val s = mat.scheme
    val themeColors = LocalIOS27Theme.current
    // GUEST_SETTINGS_V7: no personal profile or goal editors for guests.
    val sessionState by viewModel.authUiState.collectAsStateWithLifecycle()
    val isGuest = sessionState == AuthUiState.Guest
    val context = LocalContext.current
    if (isGuest && settingsSubPage == "profile") {
        GuestProfilePanel(
            onBack = viewModel::goBackSettings,
            onLogin = { viewModel.openAuthModalForLinking(AuthMode.LOGIN) },
            onRegister = { viewModel.openAuthModalForLinking(AuthMode.REGISTER) },
            onLogout = { viewModel.logout(context) }
        )
        return
    }
    val config = LocalConfiguration.current

    var newTagInput by remember { mutableStateOf("") }
    var confirmClearAll by remember { mutableStateOf(false) }
    val usernameVerification by viewModel.usernameVerification.collectAsStateWithLifecycle()
    var nameInputDraft by remember(settingsSubPage) { mutableStateOf(profile.name) }
    var bioInputDraft by remember(settingsSubPage) { mutableStateOf(profile.bio) }
    var handleInputDraft by remember(settingsSubPage) { mutableStateOf(profile.handle) }
    LaunchedEffect(profile.handle, usernameVerification.status) {
        if (usernameVerification.status == UsernameAvailabilityStatus.IDLE) {
            handleInputDraft = profile.handle
        }
    }

    val readComicsCount = remember(comics) { comics.count { it.status == ReadingStatus.READ } }
    val totalPagesRead = remember(comics) {
        comics.sumOf {
            when (it.status) {
                ReadingStatus.READ -> it.pageCount
                ReadingStatus.READING -> it.currentPage
                ReadingStatus.WANT_TO_READ -> 0
            }
        }
    }

    // Back handler inside Settings sub-pages
    BackHandler(enabled = settingsSubPage != null) {
        viewModel.goBackSettings()
    }

    // Route key for current settings view (root or sub-page)
    val routeKey = viewModel.getCurrentRouteKey("settings", settingsSubPage)

    var pendingCropBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Gallery photo picker for Avatar
    val avatarPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val bmp = com.example.ui.components.decodeAvatarBitmapFromUri(context, uri)
            if (bmp != null) {
                pendingCropBitmap = bmp
            }
        }
    }

    // Export / Import JSON Launchers & Android ZIP Launcher
    val exportJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(viewModel.exportBackupJson().toByteArray(Charsets.UTF_8))
                }
                viewModel.showNotice("Backup JSON exportado")
            } catch (_: Throwable) {
            }
        }
    }

    val importJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                if (!text.isNullOrBlank()) {
                    viewModel.importBackupJson(text)
                }
            } catch (_: Throwable) {
                viewModel.showNotice("Arquivo de backup inválido")
            }
        }
    }

    val isSliderActive = com.kyant.backdrop.catalog.components.SliderTouchLock.isAnySliderActive

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            userScrollEnabled = !isSliderActive,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = if (settingsSubPage != null) 64.dp else 12.dp,
                bottom = 88.dp
            ),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            if (settingsSubPage == null && appSettings.showFloatingMenu) {
                item {
                    TopFloatingToolbar(
                        hasComics = comics.isNotEmpty(),
                        onOpenSidebar = onOpenSidebar,
                        onQuickRegister = onQuickRegister
                    )
                }
            }

            if (settingsSubPage == null) {
            // ROOT DE AJUSTES
            item {
                Column(modifier = Modifier.padding(bottom = 14.dp)) {
                    Text(
                        text = "Configurações do sistema",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = themeColors.textSecondary
                    )
                    Text(
                        text = "Ajustes",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = themeColors.textPrimary,
                        letterSpacing = (-0.5).sp
                    )
                }
                HorizontalDivider(color = themeColors.divider, thickness = 0.5.dp)
            }

            // Cartão de Perfil estilo Conta Apple no topo dos Ajustes
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .contentMaterialSurface(RoundedCornerShape(24.dp))
                        .clickable {
                            viewModel.scrollMemory[routeKey] =
                                listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset
                            viewModel.openSettingsCategory("profile")
                        }
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    UserAvatar(profile = profile, sizeDp = 58)
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (isGuest) "Convidado" else profile.name.ifBlank { "Leitor" },
                                fontSize = 15.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = themeColors.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            if (!isGuest) Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(themeColors.accent.copy(alpha = 0.09f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = profile.handle.ifBlank { "@leitor" },
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = themeColors.textSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        if (!isGuest) Text(
                            text = profile.bio.ifBlank { "Meta: $readComicsCount/${profile.yearlyGoal} no ano" },
                            fontSize = 12.sp,
                            color = themeColors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Conta, sincronização na nuvem e perfil",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = themeColors.accent
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = themeColors.textTertiary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // GRUPO 1: Aparência e Acessibilidade
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Aparência e materiais ópticos",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = s.textSecondary,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .contentMaterialSurface(RoundedCornerShape(24.dp))
                    ) {
                        SettingsMenuRow(
                            icon = { Icon(Icons.Outlined.Palette, null, tint = themeColors.accent, modifier = Modifier.size(16.dp)) },
                            title = "Aparência",
                            subtitle = "Modo ${if (themeColors.isDark) "Escuro" else "Claro"} • Liquid Glass (${appSettings.glassLevel}%)",
                            trailingValue = AppTheme.fromId(appSettings.themeId).label,
                            onClick = {
                                viewModel.scrollMemory[routeKey] =
                                    listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset
                                viewModel.openSettingsCategory("appearance")
                            }
                        )
                        HorizontalDivider(color = themeColors.divider, thickness = 0.5.dp)
                        SettingsMenuRow(
                            icon = { Icon(Icons.Outlined.AccessibilityNew, null, tint = themeColors.accent, modifier = Modifier.size(16.dp)) },
                            title = "Acessibilidade e Contraste",
                            subtitle = null,
                            trailingValue = if (appSettings.reduceTransparency || appSettings.increaseContrast || appSettings.boldText) "Personalizado" else "Padrão",
                            onClick = {
                                viewModel.scrollMemory[routeKey] =
                                    listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset
                                viewModel.openSettingsCategory("accessibility")
                            }
                        )
                    }
                }
            }

            // GRUPO 2: Leitura e catalogação
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Leitura e catalogação",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = themeColors.textSecondary,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .contentMaterialSurface(RoundedCornerShape(24.dp))
                    ) {
                        SettingsMenuRow(
                            icon = { Icon(LucideIcons.Layers, null, tint = themeColors.accent, modifier = Modifier.size(16.dp)) },
                            title = "Biblioteca e Exibição",
                            subtitle = null,
                            trailingValue = "${appSettings.shelfColumns} colunas",
                            onClick = {
                                viewModel.scrollMemory[routeKey] =
                                    listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset
                                viewModel.openSettingsCategory("libraryConfig")
                            }
                        )
                        HorizontalDivider(color = themeColors.divider, thickness = 0.5.dp)
                        SettingsMenuRow(
                            icon = { Icon(LucideIcons.Calendar, null, tint = themeColors.accent, modifier = Modifier.size(16.dp)) },
                            title = "Diário e Resenhas",
                            subtitle = null,
                            trailingValue = if (appSettings.spoilerProtection) "Spoilers ocultos" else "Padrão",
                            onClick = {
                                viewModel.scrollMemory[routeKey] =
                                    listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset
                                viewModel.openSettingsCategory("diaryConfig")
                            }
                        )
                        HorizontalDivider(color = themeColors.divider, thickness = 0.5.dp)
                        SettingsMenuRow(
                            icon = { Icon(LucideIcons.Globe, null, tint = themeColors.accent, modifier = Modifier.size(16.dp)) },
                            title = "Catálogo e Busca",
                            subtitle = null,
                            trailingValue = when (appSettings.searchProvider) {
                                "MULTI" -> "Global"
                                "MANGA" -> "Mangás"
                                else -> "Quadrinhos"
                            },
                            onClick = {
                                viewModel.scrollMemory[routeKey] =
                                    listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset
                                viewModel.openSettingsCategory("searchConfig")
                            }
                        )
                    }
                }
            }

            // GRUPO 3: Dados e dispositivo
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Dados e dispositivo",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = themeColors.textSecondary,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .contentMaterialSurface(RoundedCornerShape(24.dp))
                    ) {
                        SettingsMenuRow(
                            icon = { Icon(LucideIcons.Upload, null, tint = themeColors.accent, modifier = Modifier.size(16.dp)) },
                            title = "Backup e Armazenamento",
                            subtitle = null,
                            trailingValue = "${comics.size} ${if (comics.size == 1) "obra" else "obras"}",
                            onClick = {
                                viewModel.scrollMemory[routeKey] =
                                    listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset
                                viewModel.openSettingsCategory("backupConfig")
                            }
                        )
                    }
                }
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuadrinheiroLogo(
                        // SETTINGS_LOGO_ADAPTIVE_V8
                        adaptiveBadgeBackground = true,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(com.kyant.shapes.ContinuousRoundedRectangle(12.dp))
                    )
                    Text(
                        text = "Quadrinheiro",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = themeColors.textPrimary,
                        letterSpacing = (-0.3).sp
                    )
                    Text(
                        text = "Catálogo, Diário Cronológico & Guia de Leitura",
                        fontSize = 11.5.sp,
                        color = themeColors.textTertiary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            when (settingsSubPage) {
                // SUB-TELA 1: LIQUID GLASS
                "liquidGlass" -> {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                            Column {
                                Text(
                                    text = "Acabamento óptico do sistema",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = s.textSecondary
                                )
                                Text(
                                    text = "Liquid Glass",
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = s.textPrimary
                                )
                            }

                            IOS27LiquidGlassController(
                                glassLevel = appSettings.glassLevel,
                                onChangeGlassLevel = { lvl ->
                                    viewModel.updateSettings { it.copy(glassLevel = lvl) }
                                },
                                edgeRefraction = appSettings.edgeRefraction,
                                glassVariant = appSettings.glassVariant
                            )

                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Estilo de transparência do vidro",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = s.textSecondary,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )
                                AppleSegmentedPicker(
                                    value = appSettings.glassVariant,
                                    onChange = { v -> viewModel.updateSettings { it.copy(glassVariant = v) } },
                                    options = listOf(
                                        SegmentOption("regular", "Regular (Adaptativo)"),
                                        SegmentOption("clear", "Cristalino (Mais Claro)")
                                    )
                                )
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Personalização do Liquid Glass",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = s.textSecondary,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .contentMaterialSurface(RoundedCornerShape(24.dp))
                                        .padding(vertical = 4.dp)
                                ) {
                                    // 1. Intensidade da Refração
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 10.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Intensidade da Refração",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = s.textPrimary
                                            )
                                            Text(
                                                text = "${(appSettings.glassIntensity * 100).roundToInt()}%",
                                                fontSize = 12.sp,
                                                fontFamily = SpaceGroteskTabularFamily,
                                                fontWeight = FontWeight.Bold,
                                                color = s.accentColor
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        AppleSlider(
                                            value = appSettings.glassIntensity,
                                            min = 0.0f,
                                            max = 2.0f,
                                            step = 0.1f,
                                            accentColor = s.accentColor,
                                            onChange = { v ->
                                                viewModel.updateSettings { it.copy(glassIntensity = v) }
                                            }
                                        )
                                    }

                                    HorizontalDivider(color = mat.dividerColor, thickness = 0.5.dp)

                                    // 2. Desfoque (Blur)
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 10.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Desfoque / Blur",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = s.textPrimary
                                            )
                                            Text(
                                                text = "${(appSettings.glassBlurIntensity * 100).roundToInt()}%",
                                                fontSize = 12.sp,
                                                fontFamily = SpaceGroteskTabularFamily,
                                                fontWeight = FontWeight.Bold,
                                                color = s.accentColor
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        AppleSlider(
                                            value = appSettings.glassBlurIntensity,
                                            min = 0.2f,
                                            max = 3.0f,
                                            step = 0.1f,
                                            accentColor = s.accentColor,
                                            onChange = { v ->
                                                viewModel.updateSettings { it.copy(glassBlurIntensity = v) }
                                            }
                                        )
                                    }

                                    HorizontalDivider(color = mat.dividerColor, thickness = 0.5.dp)

                                    // 3. Opacidade do Vidro
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 10.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Opacidade da Superfície",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = s.textPrimary
                                            )
                                            Text(
                                                text = "${(appSettings.glassOpacity * 100).roundToInt()}%",
                                                fontSize = 12.sp,
                                                fontFamily = SpaceGroteskTabularFamily,
                                                fontWeight = FontWeight.Bold,
                                                color = s.accentColor
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        AppleSlider(
                                            value = appSettings.glassOpacity,
                                            min = 0.2f,
                                            max = 2.0f,
                                            step = 0.1f,
                                            accentColor = s.accentColor,
                                            onChange = { v ->
                                                viewModel.updateSettings { it.copy(glassOpacity = v) }
                                            }
                                        )
                                    }

                                    HorizontalDivider(color = mat.dividerColor, thickness = 0.5.dp)

                                    // 4. Toggles
                                    SettingsToggleItem(
                                        title = "Aberração Cromática",
                                        subtitle = "Dispersão óptica ciano/magenta realista nas bordas",
                                        checked = appSettings.enableChromaticAberration,
                                        onChange = { v -> viewModel.updateSettings { it.copy(enableChromaticAberration = v) } }
                                    )
                                    HorizontalDivider(color = mat.dividerColor, thickness = 0.5.dp)
                                    SettingsToggleItem(
                                        title = "Refração de Lente (Lens)",
                                        subtitle = "Distorção física convexa sob o dedo e nas bordas",
                                        checked = appSettings.enableLensRefraction,
                                        onChange = { v -> viewModel.updateSettings { it.copy(enableLensRefraction = v) } }
                                    )
                                    HorizontalDivider(color = mat.dividerColor, thickness = 0.5.dp)
                                    SettingsToggleItem(
                                        title = "Reduzir Transparência",
                                        subtitle = "Altera o vidro líquido cristalino para visual fosco leitoso",
                                        checked = appSettings.reduceTransparency,
                                        onChange = { v -> viewModel.updateSettings { it.copy(reduceTransparency = v) } }
                                    )
                                    HorizontalDivider(color = mat.dividerColor, thickness = 0.5.dp)
                                    SettingsToggleItem(
                                        title = "Refração óptica nas bordas",
                                        subtitle = "Simula a curvatura de lente nas extremidades dos botões e barras",
                                        checked = appSettings.edgeRefraction,
                                        onChange = { v -> viewModel.updateSettings { it.copy(edgeRefraction = v) } }
                                    )
                                    HorizontalDivider(color = mat.dividerColor, thickness = 0.5.dp)
                                    SettingsToggleItem(
                                        title = "Iluminação ambiente da capa no topo",
                                        subtitle = "Espelha e desfoca as cores da capa ativa no fundo superior da tela",
                                        checked = appSettings.backgroundExtension,
                                        onChange = { v -> viewModel.updateSettings { it.copy(backgroundExtension = v) } }
                                    )
                                }
                            }
                        }
                    }
                }

                // SUB-TELA 2: APARÊNCIA
                "appearance" -> {
                    item {
                        val themeColors = LocalIOS27Theme.current
                        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                            Column {
                                Text(
                                    text = "Tela, brilho e materiais translúcidos",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = s.textSecondary
                                )
                                Text(
                                    text = "Aparência",
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = s.textPrimary
                                )
                            }

                            // 1. Modo de iluminação (Miniaturas de iPhone estilo iOS — sem switch redundante abaixo)
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Modo",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = themeColors.textSecondary,
                                    letterSpacing = 0.sp,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .contentMaterialSurface(RoundedCornerShape(26.dp))
                                        .padding(vertical = 18.dp, horizontal = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IphoneModePreview(
                                        label = "Claro",
                                        isSelected = appSettings.colorScheme == "light",
                                        swatchStart = mat.themeDef.light.swatchStart,
                                        swatchEnd = mat.themeDef.light.swatchEnd,
                                        isLightMode = true,
                                        accentColor = themeColors.accent,
                                        onClick = { viewModel.updateSettings { it.copy(colorScheme = "light") } },
                                        modifier = Modifier.weight(1f)
                                    )
                                    IphoneModePreview(
                                        label = "Escuro",
                                        isSelected = appSettings.colorScheme != "light",
                                        swatchStart = mat.themeDef.dark.swatchStart,
                                        swatchEnd = mat.themeDef.dark.swatchEnd,
                                        isLightMode = false,
                                        accentColor = themeColors.accent,
                                        onClick = { viewModel.updateSettings { it.copy(colorScheme = "dark") } },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            // 2. Seletor de Cores Nativo do iOS
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Cor",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = themeColors.textSecondary,
                                    letterSpacing = 0.sp,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .contentMaterialSurface(RoundedCornerShape(26.dp))
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    val currentAppTheme = AppTheme.fromId(appSettings.themeId)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Cor de destaque",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = s.textPrimary
                                        )
                                        Text(
                                            text = currentAppTheme.label,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Normal,
                                            color = s.textSecondary
                                        )
                                    }

                                    HorizontalDivider(color = themeColors.divider, thickness = 0.5.dp)

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 12.dp, horizontal = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        AppTheme.entries.forEach { theme ->
                                            val isSelected = currentAppTheme == theme
                                            val swatchColor = if (s.isLight) theme.swatchLight else theme.swatchDark

                                            Column(
                                                modifier = Modifier
                                                    .clickable(
                                                        interactionSource = null,
                                                        indication = null
                                                    ) {
                                                        viewModel.updateSettings { it.copy(themeId = theme.id) }
                                                    },
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Box(
                                                    modifier = Modifier.size(30.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    if (isSelected) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(30.dp)
                                                                .border(2.dp, themeColors.accent, CircleShape)
                                                                .padding(3.dp),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .fillMaxSize()
                                                                    .clip(CircleShape)
                                                                    .background(swatchColor)
                                                                    .border(0.5.dp, Color.Gray.copy(alpha = 0.35f), CircleShape)
                                                            )
                                                        }
                                                    } else {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(24.dp)
                                                                .clip(CircleShape)
                                                                .background(swatchColor)
                                                                .border(0.5.dp, Color.Gray.copy(alpha = 0.35f), CircleShape)
                                                        )
                                                    }
                                                }

                                                Spacer(Modifier.height(5.dp))

                                                Text(
                                                    text = theme.label,
                                                    fontSize = 10.sp,
                                                    maxLines = 1,
                                                    color = if (isSelected) s.textPrimary else s.textSecondary,
                                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // 3. Menu Exclusivo do Liquid Glass dentro de Aparência
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Refração e opacidade do sistema",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = s.textSecondary,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .contentMaterialSurface(RoundedCornerShape(24.dp))
                                ) {
                                    SettingsMenuRow(
                                        icon = { Icon(LucideIcons.Sparkles, null, tint = themeColors.accent, modifier = Modifier.size(16.dp)) },
                                        title = "Liquid Glass",
                                        subtitle = "Regulador Clear vs. Tinted, refração de borda e lente transiente",
                                        trailingValue = "${appSettings.glassLevel}%",
                                        onClick = {
                                            viewModel.scrollMemory[routeKey] =
                                                listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset
                                            viewModel.openSettingsCategory("liquidGlass", "appearance")
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // SUB-TELA 3: ACESSIBILIDADE E CONTRASTE
                "accessibility" -> {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                            Column {
                                Text(
                                    text = "Legibilidade e materiais adaptativos",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = s.textSecondary
                                )
                                Text(
                                    text = "Acessibilidade e Contraste",
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = s.textPrimary
                                )
                            }

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .contentMaterialSurface(RoundedCornerShape(24.dp))
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "PRÉ-VISUALIZAÇÃO EM TEMPO REAL",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = s.accentColor
                                    )
                                    Text(
                                        text = "Escala ${appSettings.dynamicTypeScale}%",
                                        fontSize = 11.sp,
                                        fontFamily = SpaceGroteskTabularFamily,
                                        color = s.textSecondary
                                    )
                                }
                                Text(
                                    text = "Contraste e transparência do Liquid Glass",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = s.textPrimary
                                )
                                Text(
                                    text = "O iOS ajusta automaticamente as bordas especulares, o desfoque de fundo e o peso da fonte conforme suas preferências visuais abaixo.",
                                    fontSize = 12.sp,
                                    lineHeight = 18.sp,
                                    color = s.textSecondary
                                )
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Visão e superfícies",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = s.textSecondary,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .contentMaterialSurface(RoundedCornerShape(24.dp))
                                ) {
                                    SettingsToggleItem(
                                        title = "Reduzir transparência",
                                        subtitle = "Torna os painéis de vidro mais opacos para maximizar a leitura de textos e capas",
                                        checked = appSettings.reduceTransparency,
                                        onChange = { v -> viewModel.updateSettings { it.copy(reduceTransparency = v) } }
                                    )
                                    HorizontalDivider(color = mat.dividerColor, thickness = 0.5.dp)
                                    SettingsToggleItem(
                                        title = "Aumentar contraste",
                                        subtitle = "Reforça os contornos especulares dos cartões, botões e divisores de lista",
                                        checked = appSettings.increaseContrast,
                                        onChange = { v -> viewModel.updateSettings { it.copy(increaseContrast = v) } }
                                    )
                                    HorizontalDivider(color = mat.dividerColor, thickness = 0.5.dp)
                                    SettingsToggleItem(
                                        title = "Texto em negrito",
                                        subtitle = "Aplica peso extra na tipografia SF Pro em todas as abas",
                                        checked = appSettings.boldText,
                                        onChange = { v -> viewModel.updateSettings { it.copy(boldText = v) } }
                                    )
                                    HorizontalDivider(color = mat.dividerColor, thickness = 0.5.dp)
                                    SettingsToggleItem(
                                        title = "Reduzir movimento",
                                        subtitle = "Atenua o efeito de mola das transições e lençóis modais",
                                        checked = appSettings.reduceMotion,
                                        onChange = { v -> viewModel.updateSettings { it.copy(reduceMotion = v) } }
                                    )
                                }
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Tamanho da fonte",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = s.textSecondary,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .contentMaterialSurface(RoundedCornerShape(24.dp))
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Escala de leitura do aplicativo",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = s.textSecondary
                                        )
                                        Text(
                                            text = "${appSettings.dynamicTypeScale}%",
                                            fontSize = 12.sp,
                                            fontFamily = SpaceGroteskTabularFamily,
                                            fontWeight = FontWeight.Bold,
                                            color = s.textPrimary
                                        )
                                    }
                                    AppleSlider(
                                        value = appSettings.dynamicTypeScale.toFloat(),
                                        min = 85f,
                                        max = 115f,
                                        step = 5f,
                                        ticks = listOf(85f, 95f, 100f, 105f, 115f),
                                        accentColor = s.accentColor,
                                        onChange = { v ->
                                            viewModel.updateSettings { it.copy(dynamicTypeScale = v.roundToInt()) }
                                        },
                                        leadingIcon = {
                                            Text("A", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = s.textSecondary)
                                        },
                                        trailingIcon = {
                                            Text("A", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = s.textPrimary)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // SUB-TELA 4: BIBLIOTECA E EXIBIÇÃO
                "libraryConfig" -> {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                            Column {
                                Text(
                                    text = "Grade de capas e organização",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = s.textSecondary
                                )
                                Text(
                                    text = "Biblioteca e Exibição",
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = s.textPrimary
                                )
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Densidade de colunas na estante",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = s.textSecondary,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )
                                AppleSegmentedPicker(
                                    value = appSettings.shelfColumns,
                                    onChange = { cols -> viewModel.updateSettings { it.copy(shelfColumns = cols) } },
                                    options = listOf(
                                        SegmentOption(2, "2 Colunas"),
                                        SegmentOption(3, "3 Colunas"),
                                        SegmentOption(4, "4 Colunas")
                                    )
                                )
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Detalhes visuais nas capas",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = s.textSecondary,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .contentMaterialSurface(RoundedCornerShape(24.dp))
                                ) {
                                    SettingsToggleItem(
                                        title = "Barra de progresso nas capas",
                                        subtitle = "Exibe o indicador visual de páginas lidas sobre as HQs em leitura",
                                        checked = appSettings.showProgressOnCovers,
                                        onChange = { v -> viewModel.updateSettings { it.copy(showProgressOnCovers = v) } }
                                    )
                                    HorizontalDivider(color = mat.dividerColor, thickness = 0.5.dp)
                                    SettingsToggleItem(
                                        title = "Avaliação em estrelas abaixo da capa",
                                        subtitle = "Mostra as estrelas douradas e o status na visualização em grade",
                                        checked = appSettings.showRatingsOnGrid,
                                        onChange = { v -> viewModel.updateSettings { it.copy(showRatingsOnGrid = v) } }
                                    )
                                    HorizontalDivider(color = mat.dividerColor, thickness = 0.5.dp)
                                    SettingsToggleItem(
                                        title = "Acabamento de lombada 3D (Apple Books)",
                                        subtitle = "Adiciona vinco lateral e brilho de encadernação nas capas",
                                        checked = appSettings.spine3dEffect,
                                        onChange = { v -> viewModel.updateSettings { it.copy(spine3dEffect = v) } }
                                    )
                                    HorizontalDivider(color = mat.dividerColor, thickness = 0.5.dp)
                                    SettingsToggleItem(
                                        title = "Exibir 'Lendo agora' junto ao menu",
                                        subtitle = "Mostra ou oculta a barra da HQ em leitura junto ao menu inferior",
                                        checked = appSettings.showBottomAccessory,
                                        onChange = { v -> viewModel.updateSettings { it.copy(showBottomAccessory = v) } }
                                    )
                                    HorizontalDivider(color = mat.dividerColor, thickness = 0.5.dp)
                                    SettingsToggleItem(
                                        title = "Exibir menu flutuante",
                                        subtitle = "Mostra ou oculta o botão circular de menu rápido no topo da tela",
                                        checked = appSettings.showFloatingMenu,
                                        onChange = { v -> viewModel.updateSettings { it.copy(showFloatingMenu = v) } }
                                    )
                                    HorizontalDivider(color = mat.dividerColor, thickness = 0.5.dp)
                                    SettingsToggleItem(
                                        title = "Minimizar barra de abas ao rolar",
                                        subtitle = "Recua a Tab Bar inferior durante a leitura da estante",
                                        checked = appSettings.minimizeTabBarOnScroll,
                                        onChange = { v -> viewModel.updateSettings { it.copy(minimizeTabBarOnScroll = v) } }
                                    )
                                    HorizontalDivider(color = mat.dividerColor, thickness = 0.5.dp)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Ordenação padrão",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = s.textPrimary
                                            )
                                            Text(
                                                text = "Ordem inicial ao abrir a Biblioteca",
                                                fontSize = 12.sp,
                                                color = s.textSecondary
                                            )
                                        }
                                        AppleMenuPicker(
                                            value = appSettings.defaultShelfSort,
                                            onChange = { v ->
                                                viewModel.updateSettings { it.copy(defaultShelfSort = v) }
                                                viewModel.setShelfSort(v)
                                            },
                                            options = listOf(
                                                SegmentOption("RECENT", "Mais Recentes", Icons.Default.Schedule),
                                                SegmentOption("RATING", "Melhor Avaliação", Icons.Default.Star),
                                                SegmentOption("TITLE", "Título (A–Z)", Icons.AutoMirrored.Filled.MenuBook),
                                                SegmentOption("YEAR", "Ano de Lançamento", Icons.Default.CalendarMonth)
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // SUB-TELA 5: DIÁRIO E RESENHAS
                "diaryConfig" -> {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                            Column {
                                Text(
                                    text = "Cronologia, notas e spoilers",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = s.textSecondary
                                )
                                Text(
                                    text = "Diário e Resenhas",
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = s.textPrimary
                                )
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Escala de estrelas",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = s.textSecondary,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )
                                AppleSegmentedPicker(
                                    value = if (appSettings.halfStarRatings) "HALF" else "FULL",
                                    onChange = { v -> viewModel.updateSettings { it.copy(halfStarRatings = v == "HALF") } },
                                    options = listOf(
                                        SegmentOption("HALF", "Meia Estrela (0.5 – 5.0 ★)"),
                                        SegmentOption("FULL", "Inteiras (1 – 5 ★)")
                                    )
                                )
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Privacidade e exibição no Diário",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = s.textSecondary,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .contentMaterialSurface(RoundedCornerShape(24.dp))
                                ) {
                                    SettingsToggleItem(
                                        title = "Proteção contra spoilers",
                                        subtitle = "Desfoca o texto das resenhas no Diário até que você toque para ler",
                                        checked = appSettings.spoilerProtection,
                                        onChange = { v -> viewModel.updateSettings { it.copy(spoilerProtection = v) } }
                                    )
                                    HorizontalDivider(color = mat.dividerColor, thickness = 0.5.dp)
                                    SettingsToggleItem(
                                        title = "Contabilizar releituras (↻)",
                                        subtitle = "Permite registrar quantas vezes cada edição foi relida",
                                        checked = appSettings.allowRereads,
                                        onChange = { v -> viewModel.updateSettings { it.copy(allowRereads = v) } }
                                    )
                                    HorizontalDivider(color = mat.dividerColor, thickness = 0.5.dp)
                                    SettingsToggleItem(
                                        title = "Preencher data automaticamente",
                                        subtitle = "Define a data de hoje ao registrar uma nova leitura",
                                        checked = appSettings.autoFillReadDate,
                                        onChange = { v -> viewModel.updateSettings { it.copy(autoFillReadDate = v) } }
                                    )
                                    HorizontalDivider(color = mat.dividerColor, thickness = 0.5.dp)
                                    SettingsToggleItem(
                                        title = "Exibir editora e páginas no Diário",
                                        subtitle = "Mostra metadados completos da edição em cada linha do tempo",
                                        checked = appSettings.showPagesInDiary,
                                        onChange = { v -> viewModel.updateSettings { it.copy(showPagesInDiary = v) } }
                                    )
                                }
                            }

                            // Tags Rápidas de Resenha
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Tags rápidas para resenhas",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = s.textSecondary,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .contentMaterialSurface(RoundedCornerShape(24.dp))
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        appSettings.quickReviewTags.forEach { tag ->
                                            Row(
                                                modifier = Modifier
                                                    .inputWellSurface(RoundedCornerShape(999.dp))
                                                    .clickable {
                                                        viewModel.updateSettings {
                                                            it.copy(quickReviewTags = it.quickReviewTags - tag)
                                                        }
                                                    }
                                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = tag,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = s.textPrimary
                                                )
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = null,
                                                    tint = s.textSecondary,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                            }
                                        }
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        AppleTextField(
                                            value = newTagInput,
                                            onChange = { newTagInput = it },
                                            placeholder = "Nova tag (ex: Arte pintada, Edição de luxo)...",
                                            leadingIcon = Icons.Default.LocalOffer,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(16.dp))
                                                .background(s.accentBg)
                                                .clickable {
                                                    val clean = newTagInput.trim()
                                                    if (clean.isNotEmpty() && !appSettings.quickReviewTags.contains(clean)) {
                                                        viewModel.updateSettings {
                                                            it.copy(quickReviewTags = it.quickReviewTags + clean)
                                                        }
                                                    }
                                                    newTagInput = ""
                                                }
                                                .padding(horizontal = 16.dp, vertical = 12.dp)
                                        ) {
                                            Text(
                                                text = "Adicionar",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = s.accentText
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // SUB-TELA 6: CATÁLOGO E BUSCA ONLINE
                "searchConfig" -> {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                            Column {
                                Text(
                                    text = "Motores de busca de HQs e mangás na nuvem",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = s.textSecondary
                                )
                                Text(
                                    text = "Catálogo e Busca",
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = s.textPrimary
                                )
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Fontes de pesquisa online",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = s.textSecondary,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )
                                AppleSegmentedPicker(
                                    value = appSettings.searchProvider,
                                    onChange = { v -> viewModel.updateSettings { it.copy(searchProvider = v) } },
                                    options = listOf(
                                        SegmentOption("MULTI", "Multi-API (Todas)"),
                                        SegmentOption("BOOKS", "HQs Ocidentais"),
                                        SegmentOption("MANGA", "Mangás (Jikan)")
                                    )
                                )
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Idioma e metadados padrão",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = s.textSecondary,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .contentMaterialSurface(RoundedCornerShape(24.dp))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Idioma da busca online",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = s.textPrimary
                                            )
                                            Text(
                                                text = "Define a região prioritária nos servidores",
                                                fontSize = 12.sp,
                                                color = s.textSecondary
                                            )
                                        }
                                        AppleMenuPicker(
                                            value = appSettings.searchLanguage,
                                            onChange = { v -> viewModel.updateSettings { it.copy(searchLanguage = v) } },
                                            options = listOf(
                                                SegmentOption("ALL", "Global (PT + EN)", Icons.Default.Public),
                                                SegmentOption("PT", "Português (Brasil)", Icons.AutoMirrored.Filled.MenuBook)
                                            )
                                        )
                                    }
                                    HorizontalDivider(color = mat.dividerColor, thickness = 0.5.dp)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Formato padrão ao adicionar",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = s.textPrimary
                                            )
                                            Text(
                                                text = "Aplicado automaticamente às novas edições",
                                                fontSize = 12.sp,
                                                color = s.textSecondary
                                            )
                                        }
                                        AppleMenuPicker(
                                            value = appSettings.defaultFormat,
                                            onChange = { v -> viewModel.updateSettings { it.copy(defaultFormat = v) } },
                                            options = listOf(
                                                SegmentOption("Encadernado (TPB)", "Encadernado (TPB)"),
                                                SegmentOption("Edição de Luxo (Capa Dura)", "Capa Dura / Omnibus"),
                                                SegmentOption("Edição Mensal (Avulsa)", "Edição Mensal"),
                                                SegmentOption("Mangá (Tankobon)", "Mangá (Tankobon)")
                                            )
                                        )
                                    }
                                    HorizontalDivider(color = mat.dividerColor, thickness = 0.5.dp)
                                    SettingsToggleItem(
                                        title = "Priorizar capas em Alta Definição (HD)",
                                        subtitle = "Converte miniaturas para resolução de 600px automaticamente",
                                        checked = appSettings.hdCovers,
                                        onChange = { v -> viewModel.updateSettings { it.copy(hdCovers = v) } }
                                    )
                                }
                            }

                            // Diagnóstico ao Vivo das 4 APIs Públicas
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Status das APIs de quadrinhos",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = s.textSecondary,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .contentMaterialSurface(RoundedCornerShape(24.dp))
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Wifi,
                                                contentDescription = null,
                                                tint = Color(0xFF34D399),
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Column {
                                                Text(
                                                    text = "Motor Multi-API Paralelo Ativo",
                                                    fontSize = 13.5.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = s.textPrimary
                                                )
                                                Text(
                                                    text = "Apple Books + AniList GraphQL + Kitsu DB + Open Library",
                                                    fontSize = 11.5.sp,
                                                    color = s.textSecondary
                                                )
                                            }
                                        }

                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(999.dp))
                                                .background(s.accentBg)
                                                .clickable(enabled = !isTestingApis) {
                                                    viewModel.runOnlineApiDiagnostics()
                                                }
                                                .padding(horizontal = 14.dp, vertical = 7.dp)
                                        ) {
                                            Text(
                                                text = if (isTestingApis) "Testando..." else "Testar APIs",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = s.accentText
                                            )
                                        }
                                    }

                                    if (apiDiagnostics != null) {
                                        HorizontalDivider(color = mat.dividerColor, thickness = 0.5.dp)
                                        apiDiagnostics.endpoints.forEach { ep ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(8.dp)
                                                            .clip(CircleShape)
                                                            .background(
                                                                if (ep.ok) Color(0xFF34D399) else Color(0xFFFBBF24)
                                                            )
                                                    )
                                                    Text(
                                                        text = ep.name,
                                                        fontSize = 12.sp,
                                                        color = s.textPrimary
                                                    )
                                                }
                                                Text(
                                                    text = if (ep.ok) "Online (${ep.ms} ms)" else "Limite de cota / Fallback ativo",
                                                    fontSize = 12.sp,
                                                    fontFamily = SpaceGroteskTabularFamily,
                                                    fontWeight = FontWeight.Medium,
                                                    color = s.textSecondary
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // SUB-TELA 7: MEU PERFIL
                "profile" -> {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                            Column {
                                Text(
                                    text = "Identidade do colecionador e metas",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = s.textSecondary
                                )
                                Text(
                                    text = "Meu Perfil",
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = s.textPrimary
                                )
                            }

                            // Cartão de Foto de Perfil + Seletor de Avatares Apple
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .contentMaterialSurface(RoundedCornerShape(24.dp))
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Box {
                                    UserAvatar(profile = profile, sizeDp = 88)
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .size(32.dp)
                                            .liquidGlassNavSurface(CircleShape, elevation = 6.dp)
                                            .clickable {
                                                avatarPickerLauncher.launch(
                                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                                )
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CameraAlt,
                                            contentDescription = "Enviar foto do aparelho",
                                            tint = s.textPrimary,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                }

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = if (isGuest) "Convidado" else profile.name.ifBlank { "Leitor" },
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = s.textPrimary
                                    )
                                    Text(
                                        text = "${profile.handle.ifBlank { "@leitor" }} • ${profile.favoriteUniverse.ifBlank { "Colecionador" }}",
                                        fontSize = 12.sp,
                                        color = s.textSecondary
                                    )
                                }

                                if (profile.avatarUrl.isNotBlank()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // PROFILE_SINGLE_PHOTO_ACTION_V11: use the camera beside the avatar.

                                        Row(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(40.dp)
                                                .clip(RoundedCornerShape(999.dp))
                                                .background(
                                                    Color(0xFFF43F5E).copy(
                                                        alpha = if (s.isLight) 0.10f else 0.14f
                                                    )
                                                )
                                                .border(
                                                    width = 0.8.dp,
                                                    color = Color(0xFFF43F5E).copy(alpha = 0.28f),
                                                    shape = RoundedCornerShape(999.dp)
                                                )
                                                .clickable {
                                                    viewModel.updateProfile { it.copy(avatarUrl = "") }
                                                    viewModel.showNotice("Foto removida")
                                                }
                                                .padding(horizontal = 14.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.DeleteOutline,
                                                contentDescription = null,
                                                tint = Color(0xFFF87171),
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Remover foto",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color(0xFFF87171),
                                                maxLines = 1,
                                                softWrap = false,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                } else {
                                    // PROFILE_SINGLE_PHOTO_ACTION_V11: use the camera beside the avatar.
                                }

                                if (profile.avatarUrl.isBlank()) {
                                    HorizontalDivider(color = mat.dividerColor, thickness = 0.5.dp)

                                    Text(
                                        text = "Acabamento do emblema quando sem foto",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = s.textSecondary
                                    )

                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        AppleAvatarPreset.entries.forEach { preset ->
                                            val active = profile.avatarPreset == preset.id
                                            val startC = if (preset == AppleAvatarPreset.THEME) themeColors.accent else preset.startColor
                                            val endC = if (preset == AppleAvatarPreset.THEME) themeColors.accentSecondary else preset.endColor
                                            val avgLum = (
                                                (0.2126f * startC.red + 0.7152f * startC.green + 0.0722f * startC.blue) +
                                                    (0.2126f * endC.red + 0.7152f * endC.green + 0.0722f * endC.blue)
                                                ) * 0.5f
                                            val checkTint = if (avgLum > 0.55f) Color(0xFF0A0A0E) else Color.White
                                            Box(
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        Brush.linearGradient(listOf(startC, endC))
                                                    )
                                                    .border(
                                                        width = if (active) 2.dp else 0.8.dp,
                                                        color = if (active) s.textPrimary else s.textSecondary.copy(alpha = 0.25f),
                                                        shape = CircleShape
                                                    )
                                                    .clickable {
                                                        viewModel.updateProfile { it.copy(avatarPreset = preset.id) }
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (active) {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = null,
                                                        tint = checkTint,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Informações do leitor
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Informações do leitor",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = s.textSecondary,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .contentMaterialSurface(RoundedCornerShape(24.dp))
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text("Nome de exibição", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = s.textSecondary)
                                        AppleTextField(
                                            value = nameInputDraft,
                                            onChange = { v ->
                                                nameInputDraft = v
                                                viewModel.updateProfile { it.copy(name = v) }
                                            },
                                            placeholder = "Seu nome",
                                            leadingIcon = Icons.Default.Person
                                        )
                                    }
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        val showVerificationFeedback =
                                            usernameVerification.status != UsernameAvailabilityStatus.IDLE
                                        val positiveColor = Color(0xFF34C759)
                                        val negativeColor = Color(0xFFFF3B30)
                                        val statusColor = when {
                                            usernameVerification.isPositive -> positiveColor
                                            usernameVerification.isNegative -> negativeColor
                                            else -> themeColors.accent
                                        }
                                        val statusBadgeLabel = when (usernameVerification.status) {
                                            UsernameAvailabilityStatus.AVAILABLE_OWNED,
                                            UsernameAvailabilityStatus.AVAILABLE_NEW -> "Disponível"
                                            UsernameAvailabilityStatus.TAKEN -> "Em uso"
                                            UsernameAvailabilityStatus.INVALID -> "Inválido"
                                            UsernameAvailabilityStatus.CHECKING -> "Verificando"
                                            UsernameAvailabilityStatus.IDLE -> ""
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Identificador (@usuario)",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = s.textSecondary
                                            )
                                            if (showVerificationFeedback) {
                                                Box(
                                                    modifier = Modifier
                                                        .testTag("username_verification_badge")
                                                        .clip(RoundedCornerShape(999.dp))
                                                        .background(statusColor.copy(alpha = 0.16f))
                                                        .border(
                                                            width = 0.8.dp,
                                                            color = statusColor.copy(alpha = 0.42f),
                                                            shape = RoundedCornerShape(999.dp)
                                                        )
                                                        .padding(horizontal = 8.dp, vertical = 3.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        when {
                                                            usernameVerification.isChecking -> {
                                                                CircularProgressIndicator(
                                                                    modifier = Modifier.size(10.dp),
                                                                    strokeWidth = 1.5.dp,
                                                                    color = statusColor
                                                                )
                                                            }
                                                            usernameVerification.isPositive -> {
                                                                Icon(
                                                                    imageVector = Icons.Default.Check,
                                                                    contentDescription = "Positivo: nome de usuário disponível",
                                                                    tint = statusColor,
                                                                    modifier = Modifier.size(11.dp)
                                                                )
                                                            }
                                                            usernameVerification.isNegative -> {
                                                                Icon(
                                                                    imageVector = Icons.Default.Close,
                                                                    contentDescription = "Negativo: nome de usuário indisponível",
                                                                    tint = statusColor,
                                                                    modifier = Modifier.size(11.dp)
                                                                )
                                                            }
                                                        }
                                                        Text(
                                                            text = statusBadgeLabel,
                                                            fontSize = 10.5.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = statusColor
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        AppleTextField(
                                            value = handleInputDraft,
                                            onChange = { v ->
                                                val cleaned = v.lowercase().replace(Regex("[^a-z0-9_.-@]"), "")
                                                val formatted = if (cleaned.startsWith("@")) {
                                                    "@" + cleaned.removePrefix("@").replace("@", "").take(30)
                                                } else {
                                                    "@" + cleaned.replace("@", "").take(30)
                                                }
                                                handleInputDraft = formatted
                                                viewModel.onHandleInputChanged(formatted)
                                            },
                                            placeholder = "@seu_usuario"
                                        )

                                        if (showVerificationFeedback && usernameVerification.message.isNotBlank()) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .testTag("username_verification_message")
                                                    .padding(horizontal = 2.dp, vertical = 2.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                                            ) {
                                                when {
                                                    usernameVerification.isPositive -> {
                                                        Icon(
                                                            imageVector = Icons.Default.Check,
                                                            contentDescription = null,
                                                            tint = positiveColor,
                                                            modifier = Modifier.size(13.dp)
                                                        )
                                                    }
                                                    usernameVerification.isNegative -> {
                                                        Icon(
                                                            imageVector = Icons.Default.Close,
                                                            contentDescription = null,
                                                            tint = negativeColor,
                                                            modifier = Modifier.size(13.dp)
                                                        )
                                                    }
                                                }
                                                Text(
                                                    text = usernameVerification.message,
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = statusColor
                                                )
                                            }
                                        }
                                    }
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text("Biografia / Frase do leitor", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = s.textSecondary)
                                        AppleTextField(
                                            value = bioInputDraft,
                                            onChange = { v ->
                                                bioInputDraft = v
                                                viewModel.updateProfile { it.copy(bio = v) }
                                            },
                                            placeholder = "Ex: Fã de Vertigo, mangás seinen e edições definitivas"
                                        )
                                    }

                                    HorizontalDivider(color = mat.dividerColor, thickness = 0.5.dp)

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Editora ou universo favorito", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = s.textPrimary)
                                            Text("Destacado no cabeçalho do seu perfil", fontSize = 12.sp, color = s.textSecondary)
                                        }
                                        AppleMenuPicker(
                                            value = profile.favoriteUniverse,
                                            onChange = { v -> viewModel.updateProfile { it.copy(favoriteUniverse = v) } },
                                            options = listOf(
                                                SegmentOption("DC / Vertigo", "DC / Vertigo"),
                                                SegmentOption("Marvel Comics", "Marvel Comics"),
                                                SegmentOption("Mangá & Seinen", "Mangá & Seinen"),
                                                SegmentOption("Image & Indie", "Image & Indie"),
                                                SegmentOption("Quadrinhos Nacionais", "HQs Nacionais"),
                                                SegmentOption("Franco-Belga / Europeu", "Europeu")
                                            )
                                        )
                                    }
                                }
                            }

                            // Metas de Leitura
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Metas de leitura",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = s.textSecondary,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .contentMaterialSurface(RoundedCornerShape(24.dp))
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Meta anual (${profile.yearlyGoal} HQs)", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = s.textPrimary)
                                            Text("$readComicsCount concluídos em 2026", fontSize = 12.sp, color = s.textSecondary)
                                        }
                                        AppleStepper(
                                            value = profile.yearlyGoal,
                                            min = 1,
                                            max = 365,
                                            step = 1,
                                            onChange = { v -> viewModel.updateProfile { it.copy(yearlyGoal = v) } }
                                        )
                                    }

                                    HorizontalDivider(color = mat.dividerColor, thickness = 0.5.dp)

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Meta diária (${profile.dailyPagesGoal} págs)", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = s.textPrimary)
                                            Text("Pressione e segure o Stepper para ajuste rápido", fontSize = 12.sp, color = s.textSecondary)
                                        }
                                        AppleStepper(
                                            value = profile.dailyPagesGoal,
                                            min = 5,
                                            max = 300,
                                            step = 5,
                                            onChange = { v -> viewModel.updateProfile { it.copy(dailyPagesGoal = v) } }
                                        )
                                    }

                                    HorizontalDivider(color = mat.dividerColor, thickness = 0.5.dp)

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                            Text("Exibir progresso da meta em Lendo Agora", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = s.textPrimary)
                                            Text("Mostra o cartão de acompanhamento anual na tela inicial", fontSize = 12.sp, color = s.textSecondary)
                                        }
                                        AppleToggle(
                                            checked = profile.showGoalOnHome,
                                            onChange = { v -> viewModel.updateProfile { it.copy(showGoalOnHome = v) } }
                                        )
                                    }
                                }
                            }

                            // Sincronização na Nuvem (Google & Firebase)
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Conta e nuvem",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = s.textSecondary,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )
                                CloudSyncCard(viewModel = viewModel)
                            }
                        }
                    }
                }

                // SUB-TELA 8: BACKUP E ARMAZENAMENTO
                "backupConfig" -> {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                            Column {
                                Text(
                                    text = "Persistência e sincronização",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = s.textSecondary
                                )
                                Text(
                                    text = "Backup e Armazenamento",
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = s.textPrimary
                                )
                            }

                            CloudSyncCard(viewModel = viewModel)

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .contentMaterialSurface(RoundedCornerShape(24.dp))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { exportJsonLauncher.launch("quadrinheiro-backup.json") }
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Icon(Icons.Default.Download, null, tint = s.textSecondary, modifier = Modifier.size(18.dp))
                                        Column {
                                            Text("Exportar backup (.JSON)", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = s.textPrimary)
                                            Text("${comics.size} obras e $totalPagesRead páginas registradas", fontSize = 12.sp, color = s.textSecondary)
                                        }
                                    }
                                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = s.textTertiary, modifier = Modifier.size(18.dp))
                                }

                                HorizontalDivider(color = mat.dividerColor, thickness = 0.5.dp)

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { importJsonLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) }
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Icon(Icons.Default.Upload, null, tint = s.textSecondary, modifier = Modifier.size(18.dp))
                                        Column {
                                            Text("Restaurar backup (.JSON)", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = s.textPrimary)
                                            Text("Importar arquivo salvo anteriormente", fontSize = 12.sp, color = s.textSecondary)
                                        }
                                    }
                                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = s.textTertiary, modifier = Modifier.size(18.dp))
                                }

                                HorizontalDivider(color = mat.dividerColor, thickness = 0.5.dp)

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.resetAppearanceDefaults() }
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(Icons.Default.Replay, null, tint = s.textSecondary, modifier = Modifier.size(18.dp))
                                    Column {
                                        Text("Restaurar aparência padrão", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = s.textPrimary)
                                        Text("Redefine o Liquid Glass para 26% sem apagar seus quadrinhos", fontSize = 12.sp, color = s.textSecondary)
                                    }
                                }

                                HorizontalDivider(color = mat.dividerColor, thickness = 0.5.dp)

                                if (!confirmClearAll) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { confirmClearAll = true }
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Icon(Icons.Default.DeleteOutline, null, tint = Color(0xFFF43F5E), modifier = Modifier.size(18.dp))
                                        Column {
                                            Text("Limpar todo o acervo", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFF43F5E))
                                            Text("Remove todas as HQs salvas neste aparelho", fontSize = 12.sp, color = Color(0xFFF43F5E).copy(alpha = 0.75f))
                                        }
                                    }
                                } else {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color(0xFFF43F5E).copy(alpha = 0.10f))
                                            .padding(16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Confirmar exclusão de ${comics.size} obras?",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFFFB7185),
                                            modifier = Modifier.weight(1f)
                                        )
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Box(
                                                modifier = Modifier
                                                    .inputWellSurface(RoundedCornerShape(999.dp))
                                                    .clickable { confirmClearAll = false }
                                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                                            ) {
                                                Text("Cancelar", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = s.textPrimary)
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(999.dp))
                                                    .background(Color(0xFFF43F5E))
                                                    .clickable {
                                                        viewModel.clearAllComics()
                                                        confirmClearAll = false
                                                    }
                                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                                            ) {
                                                Text("Apagar tudo", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            }
        }

        // Botão Voltar fixo na parte superior estilo Ajustes do iOS:
        // - No topo da tela: exibe o ícone "<" junto com o nome da página anterior (ex: "Ajustes" ou "Aparência").
        // - Ao rolar a tela: desaparece suavemente enquanto o usuário rola e reaparece quando a rolagem para.
        // - Fora do topo: a escrita recolhe com animação suave, mantendo apenas o ícone em uma bolinha Liquid Glass.
        if (settingsSubPage != null) {
            val isAtTop by remember {
                derivedStateOf {
                    listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset < 28
                }
            }
            val isScrolling = listState.isScrollInProgress

            val previousPageLabel = remember(settingsParentPage) {
                when (settingsParentPage) {
                    "appearance" -> "Aparência"
                    "liquidGlass" -> "Liquid Glass"
                    "accessibility" -> "Acessibilidade"
                    "libraryConfig" -> "Biblioteca"
                    "diaryConfig" -> "Diário"
                    "searchConfig" -> "Catálogo"
                    "profile" -> "Meu Perfil"
                    "backupConfig" -> "Backup"
                    else -> "Ajustes"
                }
            }

            val backBtnAlpha by animateFloatAsState(
                targetValue = if (isScrolling) 0f else 1f,
                animationSpec = spring(
                    dampingRatio = 0.86f,
                    stiffness = if (isScrolling) 420f else 260f
                ),
                label = "iosBackBtnAlpha"
            )
            val backBtnScale by animateFloatAsState(
                targetValue = if (isScrolling) 0.86f else 1f,
                animationSpec = spring(
                    dampingRatio = 0.82f,
                    stiffness = 320f
                ),
                label = "iosBackBtnScale"
            )
            val startPadDp by animateDpAsState(
                targetValue = if (isAtTop) 10.dp else 10.dp,
                animationSpec = spring(dampingRatio = 0.85f, stiffness = 320f),
                label = "iosBackBtnStartPad"
            )
            val endPadDp by animateDpAsState(
                targetValue = if (isAtTop) 15.dp else 10.dp,
                animationSpec = spring(dampingRatio = 0.85f, stiffness = 320f),
                label = "iosBackBtnEndPad"
            )

            val pillShape = RoundedCornerShape(999.dp)

            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 16.dp, top = 12.dp)
                    .height(42.dp)
                    .graphicsLayer {
                        alpha = backBtnAlpha
                        scaleX = backBtnScale
                        scaleY = backBtnScale
                        translationY = (1f - backBtnAlpha) * -10f
                    }
                    .shadow(
                        elevation = 10.dp * backBtnAlpha,
                        shape = pillShape,
                        spotColor = s.accentColor.copy(alpha = if (s.isLight) 0.14f else 0.28f)
                    )
                    .clip(pillShape)
                    .border(
                        width = 1.2.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.30f),
                                Color.Transparent
                            ),
                            start = Offset(0f, 0f),
                            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                        ),
                        shape = pillShape
                    )
                    .clickable(enabled = !isScrolling && backBtnAlpha > 0.15f) {
                        viewModel.goBackSettings()
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .liquidGlass(shape = pillShape, elevation = 10.dp)
                )
                Row(
                    modifier = Modifier.padding(start = startPadDp, end = endPadDp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Voltar para $previousPageLabel",
                        tint = s.textPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                    AnimatedVisibility(
                        visible = isAtTop,
                        enter = fadeIn(
                            animationSpec = spring(stiffness = Spring.StiffnessMedium)
                        ) + expandHorizontally(
                            expandFrom = Alignment.Start,
                            animationSpec = spring(dampingRatio = 0.85f, stiffness = 320f)
                        ),
                        exit = fadeOut(
                            animationSpec = spring(stiffness = Spring.StiffnessMedium)
                        ) + shrinkHorizontally(
                            shrinkTowards = Alignment.Start,
                            animationSpec = spring(dampingRatio = 0.85f, stiffness = 340f)
                        )
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = previousPageLabel,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = s.textPrimary,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        }

        pendingCropBitmap?.let { bmpToCrop ->
            com.example.ui.components.ProfilePhotoCropModal(
                bitmap = bmpToCrop,
                onDismiss = { pendingCropBitmap = null },
                onConfirmCrop = { croppedB64 ->
                    pendingCropBitmap = null
                    viewModel.updateProfile { it.copy(avatarUrl = croppedB64) }
                    viewModel.showNotice("Foto de perfil salva na sua conta")
                }
            )
        }
    }
}

@Composable
private fun SettingsMenuRow(
    squircleColors: List<Color> = emptyList(),
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String?,
    trailingValue: String,
    onClick: () -> Unit
) {
    val themeColors = LocalIOS27Theme.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SettingsSquircleIcon(colors = squircleColors, content = icon)

        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = themeColors.textPrimary
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        fontSize = 11.5.sp,
                        color = themeColors.textTertiary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Text(
                text = trailingValue,
                fontSize = 13.sp,
                color = themeColors.textSecondary,
                maxLines = 1
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = themeColors.textTertiary,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun SettingsToggleItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit
) {
    val mat = LocalAppleMaterials.current
    val s = mat.scheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = s.textPrimary
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                color = s.textSecondary
            )
        }
        AppleToggle(checked = checked, onChange = onChange)
    }
}

@Composable
private fun IphoneModePreview(
    label: String,
    isSelected: Boolean,
    swatchStart: Color,
    swatchEnd: Color,
    isLightMode: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val mat = LocalAppleMaterials.current
    val s = mat.scheme
    val themeColors = LocalIOS27Theme.current
    val cardBg = if (isLightMode) Color.White.copy(alpha = 0.78f) else Color.White.copy(alpha = 0.14f)
    val dockBg = if (isLightMode) Color.White.copy(alpha = 0.86f) else Color.White.copy(alpha = 0.22f)
    val textCol = if (isLightMode) Color(0xFF1C1B18) else Color.White

    val phoneBg = if (isLightMode) {
        lerp(Color(0xFFE5E5EA), themeColors.accent, 0.18f)
    } else {
        if (themeColors.isAllBlack) Color(0xFF141416) else lerp(Color(0xFF1C1C1E), themeColors.accent, 0.28f)
    }

    Column(
        modifier = modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null
        ) { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Miniatura de iPhone
        Column(
            modifier = Modifier
                .width(104.dp)
                .height(148.dp)
                .shadow(if (isSelected) 12.dp else 6.dp, RoundedCornerShape(22.dp))
                .clip(RoundedCornerShape(22.dp))
                .background(phoneBg)
                .border(
                    width = if (isSelected) 2.5.dp else 1.dp,
                    color = if (isSelected) themeColors.accent else if (themeColors.isAllWhite) Color.Black.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.22f),
                    shape = RoundedCornerShape(22.dp)
                )
                .padding(10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Relógio estilo iOS ("9:41") no topo da miniatura de tela
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(24.dp)
                        .height(4.5.dp)
                        .clip(RoundedCornerShape(99.dp))
                        .background(Color.Black.copy(alpha = 0.42f))
                )
                Text(
                    text = "9:41",
                    fontSize = 14.sp,
                    fontFamily = SpaceGroteskTabularFamily,
                    fontWeight = FontWeight.Bold,
                    color = textCol.copy(alpha = 0.90f)
                )
            }

            // Mini Cartões de Conteúdo
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(cardBg)
                        .border(0.5.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                        .padding(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(14.dp)
                            .height(22.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(textCol.copy(alpha = 0.30f))
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(
                            modifier = Modifier
                                .width(36.dp)
                                .height(5.dp)
                                .clip(RoundedCornerShape(99.dp))
                                .background(textCol.copy(alpha = 0.75f))
                        )
                        Box(
                            modifier = Modifier
                                .width(24.dp)
                                .height(4.dp)
                                .clip(RoundedCornerShape(99.dp))
                                .background(textCol.copy(alpha = 0.40f))
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    repeat(3) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(2f / 3f)
                                .clip(RoundedCornerShape(5.dp))
                                .background(cardBg)
                                .border(0.5.dp, Color.White.copy(alpha = 0.28f), RoundedCornerShape(5.dp))
                        )
                    }
                }
            }

            // Mini Barra Liquid Glass Inferior
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(14.dp)
                        .clip(RoundedCornerShape(99.dp))
                        .background(dockBg)
                        .border(0.5.dp, Color.White.copy(alpha = 0.50f), RoundedCornerShape(99.dp))
                )
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(dockBg)
                        .border(0.5.dp, Color.White.copy(alpha = 0.50f), CircleShape)
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = label,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = s.textPrimary
            )
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) themeColors.accent else Color.Transparent)
                    .border(
                        1.5.dp,
                        if (isSelected) themeColors.accent
                        else if (s.isLight) Color.Black.copy(alpha = 0.28f)
                        else Color.White.copy(alpha = 0.35f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    val checkmarkColor = if (themeColors.isAllBlack || (!isLightMode && themeColors.appTheme == AppTheme.WHITE)) Color.Black else Color.White
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = checkmarkColor,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CloudSyncCard(viewModel: QuadrinheiroViewModel) {
    val mat = LocalAppleMaterials.current
    val s = mat.scheme
    val themeColors = LocalIOS27Theme.current
    val context = LocalContext.current
    val authUiState by viewModel.authUiState.collectAsStateWithLifecycle()
    val currentUserProfile by viewModel.currentUserProfile.collectAsStateWithLifecycle()
    val cloudUser by viewModel.cloudUser.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncingCloud.collectAsStateWithLifecycle()
    val isAuthLoading by viewModel.isAuthLoading.collectAsStateWithLifecycle()

    val readerProfile by viewModel.profile.collectAsStateWithLifecycle()
    val isAuthenticated = authUiState == AuthUiState.Authenticated && currentUserProfile?.isGuest == false
    val displayName = readerProfile.name.takeIf { it.isNotBlank() && !it.equals("Leitor Convidado", ignoreCase = true) }
        ?: currentUserProfile?.displayName?.takeIf { it.isNotBlank() }
        ?: cloudUser?.displayName?.takeIf { it.isNotBlank() }
        ?: if (isAuthenticated) "Leitor Conectado" else "Modo Convidado"
    val emailText = currentUserProfile?.email?.takeIf { it.isNotBlank() }
        ?: cloudUser?.email?.takeIf { it.isNotBlank() }
    val photoUrl = readerProfile.avatarUrl.takeIf { it.isNotBlank() }
        ?: currentUserProfile?.photoUrl?.takeIf { it.isNotBlank() }
        ?: cloudUser?.photoUrl?.takeIf { it.isNotBlank() }
    val provider = currentUserProfile?.provider ?: if (cloudUser != null) "google" else "guest"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .contentMaterialSurface(RoundedCornerShape(24.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                if (isAuthenticated) {
                    UserAvatar(
                        profile = readerProfile.copy(
                            name = displayName,
                            avatarUrl = photoUrl.orEmpty()
                        ),
                        sizeDp = 46
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(s.textTertiary.copy(alpha = 0.14f))
                            .border(
                                width = 1.dp,
                                color = s.textSecondary.copy(alpha = 0.20f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = LucideIcons.UserCheck,
                            contentDescription = null,
                            tint = themeColors.accent,
                            modifier = Modifier.size(21.dp)
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = displayName,
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = s.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = when {
                            isAuthenticated && !emailText.isNullOrBlank() -> emailText
                            isAuthenticated && provider == "google" -> "Sincronizado via Google & Firestore"
                            isAuthenticated -> "Conta Quadrinheiro ativa"
                            else -> "Acervo salvo localmente neste aparelho"
                        },
                        fontSize = 12.sp,
                        color = s.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (isSyncing || isAuthLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = themeColors.accent
                )
            } else if (isAuthenticated) {
                val badgeColor = if (provider == "google") Color(0xFF10B981) else themeColors.accent
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(badgeColor.copy(alpha = 0.16f))
                        .border(
                            width = 0.8.dp,
                            color = badgeColor.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(999.dp)
                        )
                        .padding(horizontal = 9.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(badgeColor)
                        )
                        Text(
                            text = if (provider == "google") "Google Sync" else "Conta E-mail",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = badgeColor
                        )
                    }
                }
            } else {
                // Badge Modo Convidado
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color(0xFFF59E0B).copy(alpha = 0.16f))
                        .border(
                            width = 0.8.dp,
                            color = Color(0xFFF59E0B).copy(alpha = 0.38f),
                            shape = RoundedCornerShape(999.dp)
                        )
                        .padding(horizontal = 9.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Modo Convidado (Apenas Local)",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFBBF24)
                    )
                }
            }
        }

        HorizontalDivider(color = mat.dividerColor, thickness = 0.5.dp)

        if (!isAuthenticated) {
            Text(
                text = "Seus quadrinhos, notas do diário cronológico e sequências estão salvos localmente. Ao vincular uma conta Google ou E-mail, todo o seu acervo atual será mesclado automaticamente sem perder nenhum item.",
                fontSize = 12.sp,
                lineHeight = 16.5.sp,
                color = s.textSecondary
            )

            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                // Botão 1: Vincular com Google
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .liquidGlassNavSurface(shape = RoundedCornerShape(14.dp), elevation = 3.dp)
                        .clickable(enabled = !isAuthLoading && !isSyncing) {
                            viewModel.loginWithGoogle(context)
                        }
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = LucideIcons.GoogleIcon,
                            contentDescription = null,
                            tint = Color.Unspecified,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Vincular com Google",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = themeColors.textPrimary
                        )
                    }
                }

                // Botão 2: Criar Conta / Entrar com E-mail + Sair
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .inputWellSurface(RoundedCornerShape(14.dp))
                            .clickable(enabled = !isAuthLoading) {
                                viewModel.openAuthModalForLinking(AuthMode.REGISTER)
                            }
                            .padding(horizontal = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = LucideIcons.Mail,
                                contentDescription = null,
                                tint = themeColors.accent,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "Criar Conta / E-mail",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = s.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .inputWellSurface(RoundedCornerShape(14.dp))
                            .clickable(enabled = !isAuthLoading) {
                                viewModel.logout(context)
                            }
                            .padding(horizontal = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = LucideIcons.LogOut,
                                contentDescription = "Sair",
                                tint = Color(0xFFF43F5E),
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "Sair",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFF43F5E)
                            )
                        }
                    }
                }
            }
        } else {
            // Modo Autenticado: Sincronizar Agora / Vincular Google (se entrou por e-mail) / Sair da Conta (Logout)
            if (provider == "email" && cloudUser == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .liquidGlassNavSurface(shape = RoundedCornerShape(14.dp), elevation = 3.dp)
                        .clickable(enabled = !isSyncing && !isAuthLoading) {
                            viewModel.loginWithGoogle(context)
                        }
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = LucideIcons.GoogleIcon,
                            contentDescription = null,
                            tint = Color.Unspecified,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Vincular Google para Backup na Nuvem",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = themeColors.textPrimary
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .inputWellSurface(RoundedCornerShape(14.dp))
                        .clickable(enabled = !isSyncing) { viewModel.syncNowWithCloud() }
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Upload,
                            contentDescription = null,
                            tint = s.textPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (isSyncing) "Sincronizando..." else "Sincronizar",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = s.textPrimary
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .inputWellSurface(RoundedCornerShape(14.dp))
                        .clickable { viewModel.logout(context) }
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = LucideIcons.LogOut,
                            contentDescription = null,
                            tint = Color(0xFFF43F5E),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Sair da Conta",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFF43F5E)
                        )
                    }
                }
            }
        }
    }
}

