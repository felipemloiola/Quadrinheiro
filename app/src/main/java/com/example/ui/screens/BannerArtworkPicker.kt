package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.BannerArtwork
import com.example.data.BannerErrorKind
import com.example.data.BannerSearchException
import com.example.data.CatalogBanners
import com.example.ui.components.AppleSegmentedPicker
import com.example.ui.components.AppleTextField
import com.example.ui.theme.LocalIOS27Theme
import kotlinx.coroutines.CancellationException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BannerArtworkPicker(
    initialQuery: String,
    initialKind: String,
    onClose: () -> Unit,
    onGallery: () -> Unit,
    onChoose: (BannerArtwork) -> Unit
) {
    val t = LocalIOS27Theme.current
    val context = LocalContext.current
    var query by rememberSaveable { mutableStateOf(initialQuery.take(100)) }
    var kind by rememberSaveable { mutableIntStateOf(if (initialKind == "character") 1 else 0) }
    var submitted by remember { mutableStateOf<Pair<String, String>?>(null) }
    var generation by remember { mutableIntStateOf(0) }
    var items by remember { mutableStateOf(emptyList<BannerArtwork>()) }
    var loading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun submit() {
        if (query.trim().length >= 2) {
            submitted = query.trim() to if (kind == 1) "character" else "volume"
            generation++
        }
    }
    LaunchedEffect(Unit) { if (query.trim().length >= 2) submit() }
    LaunchedEffect(submitted, generation) {
        items = emptyList()
        errorMessage = null
        loading = false
        val request = submitted ?: return@LaunchedEffect
        val token = generation
        loading = true
        try {
            val next = CatalogBanners.search(request.first, request.second)
            if (submitted == request && generation == token) items = next
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (e: BannerSearchException) {
            if (submitted == request && generation == token) {
                errorMessage = when (e.kind) {
                    BannerErrorKind.RATE_LIMITED -> "Limite temporário da API de artes atingido. Aguarde alguns instantes e tente novamente ou escolha da galeria."
                    BannerErrorKind.MISSING_CONFIG -> "Configuração do provedor de artes ausente no servidor. Tente novamente ou use a galeria."
                    BannerErrorKind.NETWORK_ERROR -> "Sem conexão com o serviço de artes. Verifique sua internet e tente novamente."
                    BannerErrorKind.SERVICE_UNAVAILABLE -> "O serviço de busca de artes está temporariamente indisponível. Tente novamente ou escolha da galeria."
                }
            }
        } catch (_: Exception) {
            if (submitted == request && generation == token) {
                errorMessage = "A busca de artes está indisponível agora. Tente novamente ou use a galeria."
            }
        } finally {
            if (submitted == request && generation == token) loading = false
        }
    }
    ModalBottomSheet(onDismissRequest = onClose, containerColor = t.bgSecondary) {
        LazyColumn(
            Modifier.fillMaxWidth().heightIn(max = 650.dp).imePadding().testTag("banner_artwork_picker"),
            contentPadding = PaddingValues(20.dp, 8.dp, 20.dp, 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Escolher banner", color = t.textPrimary, style = MaterialTheme.typography.titleLarge)
                    TextButton(onClick = onClose) { Text("Fechar", color = t.accent) }
                }
            }
            item {
                AppleSegmentedPicker(listOf("HQs", "Personagens"), kind, { next ->
                    if (next != kind) { kind = next; submitted = null; errorMessage = null }
                }, Modifier.fillMaxWidth())
            }
            item {
                AppleTextField(
                    query,
                    { query = it.take(100) },
                    if (kind == 1) "Nome do personagem" else "Título da HQ",
                    leadingIcon = Icons.Default.Search
                )
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(
                        onClick = { submit() },
                        enabled = query.trim().length >= 2 && !loading,
                        modifier = Modifier.heightIn(min = 44.dp).testTag("search_banner_artwork")
                    ) {
                        Text("Buscar artes", color = t.accent)
                    }
                    TextButton(
                        onClick = onGallery,
                        modifier = Modifier.heightIn(min = 44.dp)
                    ) {
                        Text("Abrir galeria", color = t.accent)
                    }
                }
            }
            if (loading) item { LinearProgressIndicator(Modifier.fillMaxWidth(), color = t.accent) }
            if (errorMessage != null) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(errorMessage!!, color = t.textSecondary)
                        TextButton(
                            onClick = { submit() },
                            enabled = !loading && query.trim().length >= 2,
                            modifier = Modifier.heightIn(min = 44.dp).testTag("retry_banner_artwork")
                        ) {
                            Text("Tentar novamente", color = t.accent)
                        }
                    }
                }
            } else if (!loading && submitted != null && items.isEmpty()) {
                item { Text("Nenhuma arte horizontal encontrada. Tente outra HQ ou personagem.", color = t.textSecondary) }
            } else if (submitted == null) {
                item { Text("Busque uma HQ ou personagem para escolher uma arte horizontal.", color = t.textSecondary) }
            }
            items(items, key = { it.url }) { art ->
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(t.bgPrimary)
                        .clickable(role = Role.Button) { onChoose(art) }
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AsyncImage(
                        ImageRequest.Builder(context).data(art.url).size(600, 200).allowHardware(true).build(),
                        "Arte de ${art.title}",
                        Modifier.fillMaxWidth().aspectRatio(3f).clip(RoundedCornerShape(10.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(art.title, Modifier.weight(1f), color = t.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("Usar arte", color = t.accent)
                    }
                }
            }
        }
    }
}

