package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.data.*
import com.example.model.ComicEntry
import com.example.ui.components.*
import com.example.ui.theme.LocalIOS27Theme
import kotlinx.coroutines.CancellationException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FavoriteArtworkSheet(comic: ComicEntry?, character: String?, portrait: String, onClose: () -> Unit,
    onChoose: (String) -> Unit, onReplace: () -> Unit, onRemove: () -> Unit) {
    val t = LocalIOS27Theme.current
    val current = comic?.coverUrl ?: ProfileShowcase.characterImage(portrait)
    var images by remember(current) { mutableStateOf(listOf(current).filter { it.isNotBlank() }) }
    var loading by remember { mutableStateOf(true) }
    var failed by remember { mutableStateOf(false) }
    LaunchedEffect(comic?.id, comic?.externalId, character, portrait) {
        loading = true; failed = false
        images = listOf(current).filter { it.isNotBlank() }
        try {
            val alternatives = if (comic != null) ComicVineCatalog.coverChoices(comic)
                else FavoritePortraits.choices(character.orEmpty(), portrait)
            images = FavoritePortraits.merge(current, alternatives)
        } catch (cancel: CancellationException) { throw cancel }
        catch (_: Exception) { failed = true }
        finally { loading = false }
    }
    ModalBottomSheet(onDismissRequest = onClose, containerColor = t.bgSecondary) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            SectionHeading(if (comic?.let { it.externalId.ifBlank { it.id }.startsWith("cv-volume-") } == true) "Capas da coleção"
                else if (comic != null) "Capa em destaque" else "Imagem do personagem")
            if (loading) LinearProgressIndicator(Modifier.fillMaxWidth(), color = t.accent)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                items(images, key = { it }) { image ->
                    Column(Modifier.width(112.dp).clickable(role = Role.Button) { onChoose(image) }) {
                        if (comic != null) BookCover(image, comic.displayTitle, spine3d = false, modifier = Modifier.fillMaxWidth().aspectRatio(2f / 3f))
                        else ProfileImage(image, character, Modifier.size(112.dp).clip(RoundedCornerShape(18.dp)), face = true)
                        Text(if (image == current) "Selecionada" else "Escolher", color = t.accent,
                            modifier = Modifier.heightIn(min = 44.dp).padding(vertical = 12.dp), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            if (!loading && images.size <= 1) Text(if (failed) "Não foi possível buscar outras imagens agora."
                else "Não encontramos outras imagens nesta busca.", color = t.textSecondary, style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = onReplace) { Text(if (comic != null) "Trocar HQ" else "Trocar personagem", color = t.accent) }
            TextButton(onClick = onRemove) { Text("Remover dos favoritos", color = t.textSecondary) }
        }
    }
}
