package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import com.example.data.CharacterResult
import com.example.model.ComicEntry
import com.example.ui.components.*
import com.example.ui.theme.AppMotion
import com.example.ui.theme.LocalAppMotion
import com.example.ui.theme.LocalAppSettings
import com.example.ui.theme.LocalIOS27Theme
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.kyant.shapes.ContinuousCapsule
import com.kyant.shapes.ContinuousRoundedRectangle
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.min

/**
 * Floating Apple iOS 26 Liquid Glass sheet container for curation, artwork, banner, edition,
 * and community sheets.
 *
 * Uses `LiquidGlassOverlayPortal` to render inside the main Activity window directly above
 * `mainContentBackdrop` (or an active modal screen's live backdrop) so the sheet genuinely refracts
 * the live profile, covers, and banners behind it — matching native iOS 26 sheets.
 */
@Composable
internal fun CurationSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    maxHeightFraction: Float = 0.88f,
    content: @Composable ColumnScope.(dismissWith: (() -> Unit) -> Unit) -> Unit
) {
    val theme = LocalIOS27Theme.current
    val settings = LocalAppSettings.current
    val motion = LocalAppMotion.current
    val haptics = rememberAppHaptics()
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val isLight = !theme.isDark
    val sheetShape = remember { ContinuousRoundedRectangle(32.dp) }
    val entranceProgress = remember { Animatable(if (motion.reduced) 1f else 0f) }
    val dragOffsetY = remember { Animatable(0f) }
    var sheetHeightPx by remember { mutableFloatStateOf(with(density) { 480.dp.toPx() }) }
    var isDismissing by remember { mutableStateOf(false) }

    fun dismissAnimated(velocityY: Float = 0f, afterExit: () -> Unit = onDismiss) {
        if (isDismissing) return
        isDismissing = true
        scope.launch {
            if (!motion.reduced) {
                if (velocityY > 1f || dragOffsetY.value > 4f) {
                    dragOffsetY.animateTo(
                        targetValue = sheetHeightPx.coerceAtLeast(320f),
                        animationSpec = motion.ios26ModalExitSpring(),
                        initialVelocity = velocityY
                    )
                } else {
                    entranceProgress.animateTo(
                        targetValue = 0f,
                        animationSpec = motion.ios26ModalExitSpring()
                    )
                }
            }
            afterExit()
        }
    }

    LaunchedEffect(motion.reduced) {
        if (motion.reduced) {
            entranceProgress.snapTo(1f)
        } else {
            entranceProgress.animateTo(
                targetValue = 1f,
                animationSpec = motion.ios26ModalSpring()
            )
        }
    }

    LiquidGlassOverlayPortal(onDismissRequest = { dismissAnimated() }) { liveBackdrop ->
        val p = entranceProgress.value
        val clampedP = p.coerceIn(0f, 1f)
        val dragDismissFraction = (1f - (dragOffsetY.value.coerceAtLeast(0f) / sheetHeightPx.coerceAtLeast(1f))).coerceIn(0f, 1f)
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Color.Black.copy(
                            alpha = (if (settings.reduceTransparency) {
                                if (isLight) 0.38f else 0.66f
                            } else {
                                if (isLight) 0.16f else 0.34f
                            }) * clampedP * dragDismissFraction
                        )
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { dismissAnimated() }
                    )
            )
            val edgeRefraction = !settings.reduceTransparency && settings.enableLensRefraction
            val chromaticAberration = !settings.reduceTransparency && settings.enableChromaticAberration
            val refractionMult = settings.glassIntensity.coerceIn(0.6f, 2.2f)

            val sheetSurfaceModifier = if (settings.reduceTransparency) {
                Modifier
                    .clip(sheetShape)
                    .background(theme.bgSecondary, sheetShape)
                    .border(1.dp, theme.divider, sheetShape)
            } else {
                Modifier
                    .drawBackdrop(
                        backdrop = liveBackdrop,
                        shape = { sheetShape },
                        highlight = { adaptiveGlassHighlight(isLight = isLight, reduceTransparency = false) },
                        shadow = {
                            Shadow(
                                radius = 28.dp,
                                offset = DpOffset(0.dp, 10.dp),
                                color = Color.Black.copy(alpha = if (isLight) 0.16f else 0.42f)
                            )
                        },
                        innerShadow = {
                            InnerShadow(
                                radius = 7.dp,
                                alpha = if (isLight) 0.14f else 0.24f
                            )
                        },
                        effects = {
                            vibrancy()
                            val blurPx = (20f * settings.glassBlurIntensity).coerceIn(10f, 28f).dp.toPx()
                            if (blurPx >= 1f) blur(blurPx)
                            if (edgeRefraction && size.isSpecified && size.width > 4f && size.height > 4f) {
                                val rHeight = ((20f * refractionMult).dp.toPx())
                                    .coerceAtMost(min(size.width, size.height) * 0.38f)
                                val rAmount = (34f * refractionMult).dp.toPx()
                                if (rHeight >= 1f && rAmount >= 1f) {
                                    lens(
                                        refractionHeight = rHeight,
                                        refractionAmount = rAmount,
                                        depthEffect = true,
                                        chromaticAberration = chromaticAberration
                                    )
                                }
                            }
                        },
                        onDrawSurface = {
                            // Authentic iOS 26 translucent glass: lets real refracted background breathe through
                            val base = if (isLight) Color.White else Color(0xFF14151C)
                            val alpha = ((if (isLight) 0.50f else 0.44f) * settings.glassOpacity).coerceIn(0.28f, 0.74f)
                            drawRect(base.copy(alpha = alpha))
                            drawOpticalGlassMeniscus(
                                cornerRadiusPx = 32.dp.toPx(),
                                isLight = isLight,
                                edgeRefraction = edgeRefraction,
                                cyanDispersionStrength = 0.26f
                            )
                        }
                    )
                    .clip(sheetShape)
            }

            val dragHandleModifier = Modifier.pointerInput(Unit) {
                val tracker = VelocityTracker()
                var rawDrag = 0f
                detectVerticalDragGestures(
                    onDragStart = {
                        rawDrag = dragOffsetY.value
                        tracker.resetTracking()
                    },
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        rawDrag += dragAmount
                        tracker.addPosition(change.uptimeMillis, Offset(0f, rawDrag))
                        val trackedY = if (rawDrag >= 0f) rawDrag else AppMotion.rubberband(rawDrag, sheetHeightPx, 0.55f)
                        scope.launch { dragOffsetY.snapTo(trackedY) }
                    },
                    onDragEnd = {
                        val velocityY = tracker.calculateVelocity().y
                        val projected = AppMotion.projectMomentum(rawDrag, velocityY, decelerationRate = 0.99f)
                        val threshold = (sheetHeightPx * 0.34f).coerceAtLeast(with(density) { 110.dp.toPx() })
                        if (projected > threshold) {
                            if (abs(velocityY) > 650f) haptics.impactMedium() else haptics.impactLight()
                            dismissAnimated(velocityY = velocityY)
                        } else {
                            haptics.impactLight()
                            scope.launch {
                                dragOffsetY.animateTo(
                                    targetValue = 0f,
                                    animationSpec = motion.ios26DragReleaseSpring(carriedMomentum = abs(velocityY) > 40f),
                                    initialVelocity = velocityY
                                )
                            }
                        }
                    },
                    onDragCancel = {
                        scope.launch {
                            dragOffsetY.animateTo(0f, motion.ios26DragReleaseSpring(carriedMomentum = false))
                        }
                    }
                )
            }

            Box(
                modifier = modifier
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .imePadding()
                    .padding(horizontal = 10.dp, vertical = 12.dp)
                    .widthIn(max = 560.dp)
                    .fillMaxWidth()
                    .heightIn(max = 760.dp)
                    .then(
                        if (maxHeightFraction < 1f) {
                            Modifier
                                .fillMaxHeight(maxHeightFraction)
                                .wrapContentHeight(Alignment.Bottom, unbounded = false)
                        } else Modifier
                    )
                    .onSizeChanged { if (it.height > 0) sheetHeightPx = it.height.toFloat() }
                    .graphicsLayer {
                        val slideOffset = (1f - p) * 160f
                        val scale = if (motion.reduced) 1f else lerp(0.95f, 1f, p)
                        translationY = (if (motion.reduced) 0f else slideOffset) + dragOffsetY.value
                        scaleX = scale
                        scaleY = scale
                        alpha = clampedP
                    }
                    .then(sheetSurfaceModifier)
                    .pointerInput(Unit) { detectTapGestures {} }
            ) {
                CompositionLocalProvider(LocalLiquidGlassBackdrop provides liveBackdrop) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Interactive iOS 26 grabber zone (1:1 drag-to-dismiss with momentum projection)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(dragHandleModifier)
                                .padding(top = 10.dp, bottom = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(38.dp)
                                    .height(4.5.dp)
                                    .clip(ContinuousCapsule)
                                    .background(
                                        if (isLight) Color.Black.copy(alpha = 0.22f)
                                        else Color.White.copy(alpha = 0.28f)
                                    )
                            )
                        }
                        content { afterExit -> dismissAnimated(afterExit = afterExit) }
                    }
                }
            }
        }
    }
}

/**
 * Shared accessible header bar for curation sheets (favorites, artwork, banner, edition chooser).
 * Follows Apple HIG with refined typography and circular Liquid Glass close control.
 */
@Composable
internal fun CurationSheetHeader(
    title: String,
    subtitle: String? = null,
    onClose: () -> Unit,
    closeTag: String = "curation_sheet_close",
    modifier: Modifier = Modifier,
    closeLabel: String = "Fechar"
) {
    val theme = LocalIOS27Theme.current
    val settings = LocalAppSettings.current
    val closeTint = rememberAdaptiveLiquidGlassInk(theme.textPrimary, theme.bgSecondary, 4.5f)
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = title,
                color = theme.textPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.3).sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics { heading() }
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    color = theme.textSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }
        }
        SheetToolbarActionButton(
            icon = Icons.Default.Close,
            label = closeLabel,
            tint = closeTint,
            tag = closeTag,
            reduceTransparency = settings.reduceTransparency,
            onClick = onClose
        )
    }
}

internal data class FavoriteChoice(val value: String, val title: String, val snapshot: String? = null)

@Composable
internal fun FavoriteSelectionSheet(
    characters: Boolean,
    query: String,
    onQueryChange: (String) -> Unit,
    currentSelection: FavoriteChoice?,
    characterLoading: Boolean,
    characterError: Boolean,
    characterResults: List<CharacterResult>,
    comicLoading: Boolean,
    shownComics: List<ComicEntry>,
    onConfirm: (FavoriteChoice?) -> Unit,
    onClose: () -> Unit
) {
    val theme = LocalIOS27Theme.current
    val innerArtShape = remember { ContinuousRoundedRectangle(18.dp) }
    var draft by remember(currentSelection?.value) { mutableStateOf(currentSelection) }
    val selectionChanged = draft?.value != currentSelection?.value
    val removalPending = selectionChanged && draft == null
    val selectedChoice = if (selectionChanged) draft else currentSelection

    CurationSheet(onDismiss = onClose, maxHeightFraction = 0.88f) { dismissWith ->
        val focusManager = LocalFocusManager.current
        Column(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(horizontal = 20.dp, vertical = 10.dp)
                .testTag("favorite_selection_sheet"),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CurationSheetHeader(
                title = if (characters) "Personagem favorito" else "HQ favorita",
                onClose = {
                    focusManager.clearFocus()
                    dismissWith(onClose)
                },
                closeTag = "favorite_selection_close",
                closeLabel = "Cancelar"
            )
            AppleTextField(
                value = query,
                onChange = { onQueryChange(it.take(100)) },
                placeholder = if (characters) "Buscar personagem" else "Buscar HQ",
                shape = ContinuousCapsule,
                leadingIcon = Icons.Default.Search,
                useContentMaterial = false
            )
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 102.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .selectableGroup()
                    .testTag("favorite_selection_grid"),
                contentPadding = PaddingValues(top = 2.dp, bottom = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (currentSelection != null && !removalPending) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        LiquidGlassPillButton(
                            text = "Remover dos favoritos",
                            onClick = { draft = null },
                            destructive = true,
                            leadingIcon = Icons.Default.DeleteOutline,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("favorite_remove_button")
                        )
                    }
                }
                if (if (characters) characterLoading else comicLoading) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Row(
                            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IOSLoadingIndicator(Modifier.size(18.dp), color = theme.accent)
                            Text(
                                text = if (characters) "Buscando personagens…" else "Buscando quadrinhos…",
                                color = theme.textSecondary,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
                if (characters) {
                    if (characterError) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Text(
                                "Não foi possível carregar os personagens. Confira sua conexão e tente outra busca.",
                                color = theme.textSecondary,
                                fontSize = 13.sp,
                                modifier = Modifier
                                    .padding(vertical = 8.dp)
                                    .semantics { liveRegion = LiveRegionMode.Polite }
                            )
                        }
                    }
                    items(characterResults, key = { it.id }) { result ->
                        val value = result.name.take(60)
                        val selected = selectedChoice?.value == value
                        val interaction = remember { MutableInteractionSource() }
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .tactileCardPress(interaction, targetScale = 0.97f)
                                .selectable(
                                    selected = selected,
                                    interactionSource = interaction,
                                    indication = null,
                                    role = Role.RadioButton
                                ) {
                                    draft = FavoriteChoice(
                                        value,
                                        result.name,
                                        com.example.data.ProfileShowcase.characterSnapshot(result)
                                    )
                                }
                                .testTag("favorite_character_option_${result.id}"),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FavoriteChoiceArt(Modifier.fillMaxWidth().aspectRatio(1f), innerArtShape, selected) {
                                ProfileImage(result.image, null, Modifier.fillMaxSize(), face = true)
                            }
                            Text(
                                text = result.name,
                                color = theme.textPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (!result.name.contains(result.originalName, true) && result.originalName.isNotBlank()) {
                                Text(
                                    text = result.originalName,
                                    color = theme.textSecondary,
                                    fontSize = 11.sp,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                    if (!characterLoading && !characterError && characterResults.isEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Text(
                                "Nenhum personagem encontrado. Tente o nome em português ou inglês.",
                                color = theme.textSecondary,
                                fontSize = 13.sp,
                                modifier = Modifier
                                    .padding(vertical = 8.dp)
                                    .semantics { liveRegion = LiveRegionMode.Polite }
                            )
                        }
                    }
                } else {
                    items(shownComics, key = { it.id }) { comic ->
                        val selected = selectedChoice?.value == comic.id
                        val interaction = remember { MutableInteractionSource() }
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .tactileCardPress(interaction, targetScale = 0.97f)
                                .selectable(
                                    selected = selected,
                                    interactionSource = interaction,
                                    indication = null,
                                    role = Role.RadioButton
                                ) {
                                    draft = FavoriteChoice(
                                        comic.id,
                                        comic.displayTitle,
                                        com.example.data.ProfileShowcase.comicSnapshot(comic)
                                    )
                                }
                                .testTag("favorite_comic_option_${comic.id}"),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FavoriteChoiceArt(Modifier.fillMaxWidth().aspectRatio(2f / 3f), innerArtShape, selected) {
                                BookCover(
                                    comic.coverUrl,
                                    comic.displayTitle,
                                    spine3d = false,
                                    modifier = Modifier.fillMaxSize().clearAndSetSemantics { }
                                )
                            }
                            Text(
                                text = comic.displayTitle,
                                color = theme.textPrimary,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            val sub = comic.publisher.ifBlank { comic.authors }
                            if (sub.isNotBlank()) {
                                Text(
                                    text = sub,
                                    color = theme.textSecondary,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                    if (!comicLoading && shownComics.isEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Text(
                                "Nenhuma HQ encontrada para essa busca.",
                                color = theme.textSecondary,
                                fontSize = 13.sp,
                                modifier = Modifier
                                    .padding(vertical = 8.dp)
                                    .semantics { liveRegion = LiveRegionMode.Polite }
                            )
                        }
                    }
                }
            }
            val selectionStatus = when {
                removalPending -> "Remover ${currentSelection?.title.orEmpty()}"
                selectedChoice != null -> "Seleção: ${selectedChoice.title}"
                else -> null
            }
            if (selectionStatus != null) {
                Text(
                    text = selectionStatus,
                    color = theme.textPrimary,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { liveRegion = LiveRegionMode.Polite }
                        .testTag("favorite_selection_status")
                )
            }
            LiquidGlassPillButton(
                text = if (removalPending) "Confirmar remoção" else "Confirmar",
                onClick = {
                    focusManager.clearFocus()
                    dismissWith { onConfirm(selectedChoice) }
                },
                enabled = selectionChanged,
                emphasized = !removalPending,
                destructive = removalPending,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("favorite_selection_confirm")
            )
        }
    }
}

@Composable
internal fun FavoriteChoiceArt(
    modifier: Modifier,
    shape: Shape,
    selected: Boolean,
    content: @Composable BoxScope.() -> Unit
) {
    val theme = LocalIOS27Theme.current
    val reduceTransparency = LocalAppSettings.current.reduceTransparency
    val border = when {
        selected -> theme.accent
        reduceTransparency -> theme.divider
        theme.isDark -> Color.White.copy(alpha = 0.14f)
        else -> Color.Black.copy(alpha = 0.08f)
    }
    Box(
        modifier
            .clip(shape)
            .border(if (selected) 2.dp else 1.dp, border, shape)
    ) {
        content()
        if (selected) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(7.dp)
                    .size(26.dp)
                    .background(theme.accent, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = rememberAdaptiveLiquidGlassInk(Color.White, theme.accent),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
