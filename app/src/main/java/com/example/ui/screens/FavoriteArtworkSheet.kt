package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.*
import com.example.model.ComicEntry
import com.example.ui.components.*
import com.example.ui.theme.LocalIOS27Theme
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
    var images by remember(current) { mutableStateOf(listOf(current).filter { it.isNotBlank() }) }
    var loading by remember { mutableStateOf(true) }
    var failed by remember { mutableStateOf(false) }
    var retryToken by remember { mutableIntStateOf(0) }

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

    val title = when {
        comic?.externalId?.ifBlank { comic.id }?.startsWith("cv-volume-") == true -> "Capas da coleção"
        comic != null -> "Capa em destaque"
        else -> "Imagem do personagem"
    }
    val haptics = LocalHapticFeedback.current
    val panel = rememberCurationPanelState()
    CurationSheet(title, onClose, compact = true, glass = true, panel = panel) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (loading) {
                LoadingLine(Modifier.fillMaxWidth(), color = t.accent)
            }
            if (images.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    items(images, key = { it }) { image ->
                        Box(Modifier.width(112.dp)) {
                            FavoritePickerTile(
                                title = if (image == current) "Selecionada" else "Escolher",
                                subtitle = "",
                                selected = image == current,
                                onSelect = {
                                    if (panel.request == 0) {
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onChoose(image)
                                    }
                                }
                            ) {
                                if (comic != null) {
                                    BookCover(image, comic.displayTitle, spine3d = false, modifier = Modifier.fillMaxWidth().aspectRatio(2f / 3f))
                                } else {
                                    ProfileImage(image, character, Modifier.fillMaxWidth().aspectRatio(1f), face = true)
                                }
                            }
                        }
                    }
                }
            }
            if (!loading && images.size <= 1) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (failed) "Não foi possível buscar outras imagens agora."
                        else "Não encontramos outras imagens nesta busca.",
                        color = t.textSecondary,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    if (failed) {
                        LiquidGlassPillButton(
                            label = "Tentar novamente",
                            onClick = { retryToken++ },
                            modifier = Modifier.testTag("retry_favorite_artwork")
                        )
                    }
                }
            }
            LiquidGlassPillButton(if (comic != null) "Trocar HQ" else "Trocar personagem", onReplace, Modifier.fillMaxWidth(), prominent = true)
            LiquidGlassPillButton("Remover dos favoritos", onRemove, Modifier.fillMaxWidth())
        }
    }
}

