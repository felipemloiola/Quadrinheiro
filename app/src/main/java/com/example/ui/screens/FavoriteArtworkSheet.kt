package com.example.ui.screens

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.model.ComicEntry
import com.example.ui.components.*
import com.example.ui.theme.LocalIOS27Theme
import com.kyant.shapes.ContinuousRoundedRectangle
import kotlinx.coroutines.CancellationException

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
    val current = comic?.coverUrl ?: ProfileShowcase.characterImage(portrait)
    var draft by remember(current) { mutableStateOf(current) }
    var removalPending by remember(current) { mutableStateOf(false) }
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

    CurationSheet(onDismiss = onClose, maxHeightFraction = 0.84f) { dismissWith ->
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .testTag("favorite_artwork_sheet"),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            CurationSheetHeader(
                title = if (comic?.let { it.externalId.ifBlank { it.id }.startsWith("cv-volume-") } == true) "Capas da coleção"
                else if (comic != null) "Capa em destaque"
                else "Imagem do personagem",
                onClose = { dismissWith(onClose) },
                closeTag = "favorite_artwork_close",
                closeLabel = "Cancelar"
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
                        .selectableGroup(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 2.dp)
                ) {
                    itemsIndexed(images, key = { _, image -> image }) { index, image ->
                        val isSelected = !removalPending && image == draft
                        val interaction = remember { MutableInteractionSource() }
                        FavoriteChoiceArt(
                            modifier = Modifier
                                .width(if (comic != null) 118.dp else 132.dp)
                                .aspectRatio(if (comic != null) 2f / 3f else 1f)
                                .pressScaleFeedback(interaction, targetScale = 0.94f)
                                .selectable(
                                    selected = isSelected,
                                    interactionSource = interaction,
                                    indication = null,
                                    role = Role.RadioButton
                                ) {
                                    draft = image
                                    removalPending = false
                                }
                                .semantics {
                                    val item = if (comic != null) "Capa" else "Retrato"
                                    contentDescription = if (image == current) "$item em uso" else "$item ${index + 1}"
                                },
                            shape = artShape,
                            selected = isSelected
                        ) {
                            if (comic != null) {
                                BookCover(
                                    image,
                                    comic.displayTitle,
                                    spine3d = false,
                                    modifier = Modifier.fillMaxSize().clearAndSetSemantics { }
                                )
                            } else {
                                ProfileImage(
                                    image,
                                    null,
                                    Modifier.fillMaxSize(),
                                    face = true
                                )
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

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp, bottom = 6.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (removalPending || (draft.isNotBlank() && draft != current)) {
                    LiquidGlassPillButton(
                        text = if (removalPending) "Confirmar remoção" else "Confirmar imagem",
                        onClick = {
                            val selectedImage = draft
                            if (removalPending) dismissWith(onRemove)
                            else dismissWith { onChoose(selectedImage) }
                        },
                        emphasized = !removalPending,
                        destructive = removalPending,
                        modifier = Modifier.fillMaxWidth().testTag("favorite_artwork_confirm")
                    )
                }
                LiquidGlassPillButton(
                    text = if (comic != null) "Trocar HQ" else "Trocar personagem",
                    onClick = { dismissWith(onReplace) },
                    emphasized = true,
                    leadingIcon = Icons.Default.SwapHoriz,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("favorite_artwork_replace")
                )
                LiquidGlassPillButton(
                    text = if (removalPending) "Manter favorito" else "Remover dos favoritos",
                    onClick = { removalPending = !removalPending },
                    destructive = !removalPending,
                    leadingIcon = if (removalPending) null else Icons.Default.DeleteOutline,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("favorite_artwork_remove")
                )
            }
        }
    }
}
