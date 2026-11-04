package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.data.*
import com.example.model.ComicEntry
import com.example.ui.components.*
import com.example.ui.theme.LocalIOS27Theme
import kotlinx.coroutines.CancellationException

@OptIn(ExperimentalMaterial3Api::class)
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

    ModalBottomSheet(onDismissRequest = onClose, containerColor = t.bgSecondary) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            SectionHeading(
                if (comic?.let { it.externalId.ifBlank { it.id }.startsWith("cv-volume-") } == true) "Capas da coleção"
                else if (comic != null) "Capa em destaque"
                else "Imagem do personagem"
            )
            if (loading) {
                LinearProgressIndicator(Modifier.fillMaxWidth(), color = t.accent)
            }
            if (images.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    items(images, key = { it }) { image ->
                        Column(Modifier.width(112.dp).clickable(role = Role.Button) { onChoose(image) }) {
                            if (comic != null) {
                                BookCover(image, comic.displayTitle, spine3d = false, modifier = Modifier.fillMaxWidth().aspectRatio(2f / 3f))
                            } else {
                                ProfileImage(image, character, Modifier.size(112.dp).clip(RoundedCornerShape(18.dp)), face = true)
                            }
                            Text(
                                text = if (image == current) "Selecionada" else "Escolher",
                                color = t.accent,
                                modifier = Modifier.heightIn(min = 44.dp).padding(vertical = 12.dp),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
            if (!loading && images.size <= 1) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (failed) "Não foi possível buscar outras imagens agora."
                        else "Não encontramos outras imagens nesta busca.",
                        color = t.textSecondary,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )
                    if (failed) {
                        TextButton(
                            onClick = { retryToken++ },
                            modifier = Modifier.heightIn(min = 44.dp).testTag("retry_favorite_artwork")
                        ) {
                            Text("Tentar novamente", color = t.accent)
                        }
                    }
                }
            }
            TextButton(onClick = onReplace, modifier = Modifier.heightIn(min = 44.dp)) {
                Text(if (comic != null) "Trocar HQ" else "Trocar personagem", color = t.accent)
            }
            TextButton(onClick = onRemove, modifier = Modifier.heightIn(min = 44.dp)) {
                Text("Remover dos favoritos", color = t.textSecondary)
            }
        }
    }
}

