package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.BannerArtwork
import com.example.data.BannerErrorKind
import com.example.data.BannerSearchException
import com.example.data.CatalogBanners
import com.example.ui.components.AppleSegmentedPicker
import com.example.ui.components.AppleTextField
import com.example.ui.components.IOSLoadingIndicator
import com.example.ui.components.LiquidGlassPillButton
import com.example.ui.components.contentMaterialSurface
import com.example.ui.components.pressScaleFeedback
import com.example.ui.components.rememberAdaptiveLiquidGlassInk
import com.example.ui.theme.LocalAppSettings
import com.example.ui.theme.LocalIOS27Theme
import com.kyant.shapes.ContinuousCapsule
import com.kyant.shapes.ContinuousRoundedRectangle
import kotlinx.coroutines.CancellationException

@Composable
internal fun BannerArtworkPicker(
    initialQuery: String,
    initialKind: String,
    onClose: () -> Unit,
    onGallery: () -> Unit,
    onChoose: (BannerArtwork) -> Unit
) {
    val t = LocalIOS27Theme.current
    val settings = LocalAppSettings.current
    val context = LocalContext.current
    var query by rememberSaveable { mutableStateOf(initialQuery.take(100)) }
    var kind by rememberSaveable { mutableIntStateOf(if (initialKind == "character") 1 else 0) }
    var submitted by remember { mutableStateOf<Pair<String, String>?>(null) }
    var generation by remember { mutableIntStateOf(0) }
    var items by remember { mutableStateOf(emptyList<BannerArtwork>()) }
    var loading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val cardShape = remember { ContinuousRoundedRectangle(18.dp) }
    val artShape = remember { ContinuousRoundedRectangle(12.dp) }

    fun submit() {
        val clean = query.trim().ifBlank { "Batman" }
        if (clean.length >= 2) {
            submitted = clean to if (kind == 1) "character" else "volume"
            generation++
        }
    }
    LaunchedEffect(query, kind) {
        val trimmed = query.trim()
        if (trimmed.length >= 2) {
            kotlinx.coroutines.delay(320)
            submitted = trimmed to if (kind == 1) "character" else "volume"
            generation++
        } else if (trimmed.isEmpty() && submitted == null) {
            submitted = "Batman" to if (kind == 1) "character" else "volume"
            generation++
        }
    }
    LaunchedEffect(submitted, generation) {
        items = emptyList()
        errorMessage = null
        loading = false
        val request = submitted ?: return@LaunchedEffect
        val token = generation
        loading = true
        try {
            val next = CatalogBanners.search(request.first, request.second, context)
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

    CurationSheet(onDismiss = onClose, maxHeightFraction = 0.88f) {
        LazyColumn(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .testTag("banner_artwork_picker"),
            contentPadding = PaddingValues(20.dp, 8.dp, 20.dp, 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                CurationSheetHeader(
                    title = "Escolher banner",
                    subtitle = "Busque uma arte horizontal ou abra sua galeria.",
                    onClose = onClose,
                    closeTag = "banner_picker_close"
                )
            }
            item {
                AppleSegmentedPicker(listOf("HQs", "Personagens"), kind, { next ->
                    if (next != kind) {
                        kind = next; submitted = null; errorMessage = null
                    }
                }, Modifier.fillMaxWidth())
            }
            item {
                AppleTextField(
                    value = query,
                    onChange = { query = it.take(100) },
                    placeholder = if (kind == 1) "Nome do personagem" else "Título da HQ",
                    shape = ContinuousCapsule,
                    leadingIcon = Icons.Default.Search,
                    useContentMaterial = false
                )
            }
            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LiquidGlassPillButton(
                        text = "Buscar artes",
                        onClick = { submit() },
                        enabled = query.trim().length >= 2 && !loading,
                        emphasized = true,
                        leadingIcon = Icons.Default.Search,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("search_banner_artwork")
                    )
                    LiquidGlassPillButton(
                        text = "Abrir galeria",
                        onClick = onGallery,
                        leadingIcon = Icons.Default.PhotoLibrary,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("gallery_banner_artwork")
                    )
                }
            }
            if (loading) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .semantics(mergeDescendants = true) {
                                contentDescription = "Buscando artes de banner"
                                liveRegion = LiveRegionMode.Polite
                            },
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IOSLoadingIndicator(modifier = Modifier.size(18.dp), color = t.accent)
                        Text("Buscando artes horizontais…", color = t.textSecondary, fontSize = 13.sp)
                    }
                }
            }
            if (errorMessage != null) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            errorMessage!!,
                            color = t.textSecondary,
                            fontSize = 13.sp,
                            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                        )
                        LiquidGlassPillButton(
                            text = "Tentar novamente",
                            onClick = { submit() },
                            enabled = !loading && query.trim().length >= 2,
                            leadingIcon = Icons.Default.Refresh,
                            modifier = Modifier.testTag("retry_banner_artwork")
                        )
                    }
                }
            } else if (!loading && submitted != null && items.isEmpty()) {
                item {
                    Text(
                        "Nenhuma arte horizontal encontrada. Tente outra HQ ou personagem.",
                        color = t.textSecondary,
                        fontSize = 13.sp,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                    )
                }
            } else if (submitted == null) {
                item {
                    Text(
                        "Busque uma HQ ou personagem para escolher uma arte horizontal.",
                        color = t.textSecondary,
                        fontSize = 13.sp
                    )
                }
            }
            items(items, key = { it.url }) { art ->
                val interaction = remember { MutableInteractionSource() }
                val cardModifier = if (settings.reduceTransparency) {
                    Modifier
                        .clip(cardShape)
                        .background(t.bgPrimary, cardShape)
                        .border(0.85.dp, t.divider, cardShape)
                } else {
                    Modifier.contentMaterialSurface(shape = cardShape)
                }
                Column(
                    Modifier
                        .fillMaxWidth()
                        .pressScaleFeedback(interaction, targetScale = 0.98f)
                        .then(cardModifier)
                        .focusable()
                        .clickable(
                            interactionSource = interaction,
                            indication = null,
                            role = Role.Button,
                            onClickLabel = "Usar arte de ${art.title}"
                        ) { onChoose(art) }
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AsyncImage(
                        ImageRequest.Builder(context).data(art.url).size(600, 200).allowHardware(true).build(),
                        "Arte de ${art.title}",
                        Modifier
                            .fillMaxWidth()
                            .aspectRatio(3f)
                            .clip(artShape),
                        contentScale = ContentScale.Crop
                    )
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 34.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            art.title,
                            Modifier.weight(1f),
                            color = t.textPrimary,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            "Usar arte",
                            color = rememberAdaptiveLiquidGlassInk(t.accent, t.bgPrimary, 4.5f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
