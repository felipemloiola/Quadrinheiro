package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.model.ComicEntry
import com.example.ui.components.*
import com.example.ui.theme.LocalAppSettings
import com.example.ui.theme.LocalIOS27Theme
import com.example.ui.theme.contrastRatio
import com.kyant.shapes.ContinuousRoundedRectangle
import kotlinx.coroutines.CancellationException

/**
 * Apple iOS 26 HIG sheet for choosing a favorite character portrait or favorite comic cover.
 * Uses clean edge-to-edge squircle artwork tiles with floating iOS selection checkmarks
 * (no clunky nested gray cards or repetitive flat pills) and full-width non-truncating
 * Liquid Glass action capsules.
 */
@Composable
internal fun FavoriteArtworkSheet(
    comic: ComicEntry?,
    character: String?,
    portrait: String,
    onClose: () -> Unit,
    onChoose: (String) -> Unit,
    onReplace: () -> Unit,
    onRemove: () -> Unit
) {
    val t = LocalIOS27Theme.current
    val settings = LocalAppSettings.current
    val isLight = !t.isDark
    val current = comic?.coverUrl ?: ProfileShowcase.characterImage(portrait)
    var images by remember(current) { mutableStateOf(listOf(current).filter { it.isNotBlank() }) }
    var loading by remember { mutableStateOf(true) }
    var failed by remember { mutableStateOf(false) }
    var retryToken by remember { mutableIntStateOf(0) }
    val artShape = remember { ContinuousRoundedRectangle(20.dp) }

    LaunchedEffect(comic?.id, comic?.externalId, character, portrait, retryToken) {
        loading = true
        failed = false
        images = listOf(current).filter { it.isNotBlank() }
        try {
            if (comic != null) {
                val primary = runCatching { ComicVineCatalog.coverChoices(comic) }.getOrDefault(emptyList())
                val extras = if (primary.size < 3) {
                    val bySearch = runCatching {
                        ComicVineCatalog.search(comic.displayTitle, 0).items.map { it.coverUrl }
                    }.getOrDefault(emptyList())
                    val byApple = runCatching {
                        PortugueseBookSources.appleBrazil(comic.displayTitle, 0).map { it.coverUrl }
                    }.getOrDefault(emptyList())
                    bySearch + byApple
                } else emptyList()
                images = FavoritePortraits.merge(current, primary + extras)
            } else {
                FavoritePortraits.observeChoices(character.orEmpty(), current, portrait).collect { next ->
                    images = next
                }
            }
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (_: Exception) {
            if (images.isEmpty()) failed = true
        } finally {
            loading = false
        }
    }

    val checkInk = remember(t.accent) {
        if (contrastRatio(Color.White, t.accent) >= contrastRatio(Color.Black, t.accent)) Color.White else Color.Black
    }

    CurationSheet(onDismiss = onClose, maxHeightFraction = 0.84f) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .testTag("favorite_artwork_sheet"),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            CurationSheetHeader(
                title = if (comic?.let { it.externalId.ifBlank { it.id }.startsWith("cv-volume-") } == true) "Capas da coleção"
                else if (comic != null) "Capa em destaque"
                else "Imagem do personagem",
                subtitle = if (comic != null) "Toque na capa que deseja exibir no perfil." else "Toque no retrato que deseja exibir no perfil.",
                onClose = onClose,
                closeTag = "favorite_artwork_close"
            )
            if (loading) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics(mergeDescendants = true) {
                            contentDescription = "Carregando opções de imagem"
                            liveRegion = LiveRegionMode.Polite
                        },
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IOSLoadingIndicator(modifier = Modifier.size(18.dp), color = t.accent)
                    Text(
                        text = "Carregando opções de imagem…",
                        color = t.textSecondary,
                        fontSize = 13.sp
                    )
                }
            }
            if (images.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { selectableGroup() },
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 2.dp)
                ) {
                    items(images, key = { it }) { image ->
                        val isSelected = image == current
                        val interaction = remember { MutableInteractionSource() }
                        val borderColor = when {
                            isSelected -> t.accent
                            settings.reduceTransparency -> t.divider
                            isLight -> Color.Black.copy(alpha = 0.10f)
                            else -> Color.White.copy(alpha = 0.18f)
                        }
                        Box(
                            modifier = Modifier
                                .width(if (comic != null) 118.dp else 132.dp)
                                .aspectRatio(if (comic != null) 2f / 3f else 1f)
                                .pressScaleFeedback(interaction, targetScale = 0.94f)
                                .clip(artShape)
                                .border(
                                    width = if (isSelected) 2.5.dp else 0.75.dp,
                                    color = borderColor,
                                    shape = artShape
                                )
                                .focusable()
                                .semantics(mergeDescendants = true) {
                                    selected = isSelected
                                    role = Role.RadioButton
                                    contentDescription = if (isSelected) "Imagem selecionada" else "Escolher esta imagem"
                                }
                                .clickable(
                                    interactionSource = interaction,
                                    indication = null,
                                    role = Role.Button,
                                    onClickLabel = if (isSelected) "Imagem atual selecionada" else "Escolher esta imagem"
                                ) { onChoose(image) }
                        ) {
                            if (comic != null) {
                                BookCover(
                                    image,
                                    comic.displayTitle,
                                    spine3d = false,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                ProfileImage(
                                    image,
                                    character,
                                    Modifier.fillMaxSize(),
                                    face = true
                                )
                            }

                            // Native iOS Photos / Contact Poster selection badge in top-right corner
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) t.accent
                                        else Color.Black.copy(alpha = 0.34f)
                                    )
                                    .border(
                                        width = 1.25.dp,
                                        color = Color.White.copy(alpha = if (isSelected) 0.92f else 0.65f),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = checkInk,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            if (!loading && images.size <= 1) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (failed) "Não foi possível buscar outras imagens agora."
                        else "Não encontramos outras imagens nesta busca.",
                        color = t.textSecondary,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier
                            .weight(1f)
                            .semantics { liveRegion = LiveRegionMode.Polite }
                    )
                    if (failed) {
                        LiquidGlassPillButton(
                            text = "Tentar novamente",
                            onClick = { retryToken++ },
                            leadingIcon = Icons.Default.Refresh,
                            modifier = Modifier.testTag("retry_favorite_artwork")
                        )
                    }
                }
            }

            // Stacked full-width iOS 26 Liquid Glass action capsules — zero text truncation
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp, bottom = 6.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                LiquidGlassPillButton(
                    text = if (comic != null) "Trocar HQ" else "Trocar personagem",
                    onClick = onReplace,
                    emphasized = true,
                    leadingIcon = Icons.Default.SwapHoriz,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("favorite_artwork_replace")
                )
                LiquidGlassPillButton(
                    text = "Remover dos favoritos",
                    onClick = onRemove,
                    destructive = true,
                    leadingIcon = Icons.Default.DeleteOutline,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("favorite_artwork_remove")
                )
            }
        }
    }
}
