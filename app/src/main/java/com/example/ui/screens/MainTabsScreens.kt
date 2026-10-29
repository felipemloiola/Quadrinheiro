package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.data.CuratedCatalog
import com.example.data.SeriesSequenceEngine
import com.example.model.AppSettings
import com.example.model.ComicEntry
import com.example.model.ComicMoment
import com.example.model.ReaderProfile
import com.example.model.ReadingStatus
import com.example.ui.components.AppleActivityRingsGauge
import com.example.ui.components.AppleCapsuleButton
import com.example.ui.components.AppleMenuPicker
import com.example.ui.components.AppleSegmentedPicker
import com.example.ui.components.AppleStepper
import com.example.ui.components.AppleTextField
import com.example.ui.components.AppleToggle
import com.example.ui.components.BookCover
import com.example.ui.components.LiquidGlassBackdropScope
import com.example.ui.components.LocalLiquidGlassAmbientBackdropLayer
import com.example.ui.components.LocalLiquidGlassBackdropLayer
import com.example.ui.components.LocalLiquidGlassBackdropOrigin
import com.example.ui.components.LocalLiquidGlassScrollTick
import com.example.ui.components.LucideIcons
import com.example.ui.components.SegmentOption
import com.example.ui.components.StarRating
import com.example.ui.components.UserAvatar
import com.example.ui.components.contentMaterialSurface
import com.example.ui.components.inputWellSurface
import com.example.ui.components.liquidGlass
import com.example.ui.components.liquidGlassNavSurface
import com.example.ui.theme.AppTheme
import com.example.ui.theme.LocalAppleMaterials
import com.example.ui.theme.LocalIOS27Theme
import com.example.ui.theme.LocalIsDarkTheme
import com.example.ui.theme.PlusJakartaSansFamily
import com.example.ui.theme.SpaceGroteskTabularFamily
import kotlin.math.roundToInt

@Composable
private fun TabScrollBackdropContainer(
    listState: LazyListState,
    hasComics: Boolean,
    showFloatingMenu: Boolean,
    onOpenSidebar: () -> Unit,
    onQuickRegister: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        content()
    }
}

/**
 * Top Floating Toolbar (Sidebar Button on left) com `Modifier.liquidGlass` conectado ao `realLiquidGlassBackdrop`.
 */
@Composable
fun TopFloatingToolbar(
    hasComics: Boolean,
    onOpenSidebar: () -> Unit,
    onQuickRegister: () -> Unit,
    visible: Boolean = true
) {
    if (!visible) return
    val mat = LocalAppleMaterials.current
    Row(
        modifier = Modifier.padding(bottom = 4.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .shadow(
                    elevation = 8.dp,
                    shape = CircleShape,
                    spotColor = mat.scheme.accentColor.copy(alpha = if (mat.scheme.isLight) 0.14f else 0.28f)
                )
                .liquidGlass(shape = CircleShape, elevation = 8.dp)
                .clickable { onOpenSidebar() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = LucideIcons.PanelLeft,
                contentDescription = "Abrir Menu Lateral",
                tint = mat.scheme.textPrimary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/**
 * ABA 1: LENDO AGORA (HOME / APPLE BOOKS READING NOW)
 */
@Composable
fun HomeReadingNowTab(
    comics: List<ComicEntry>,
    profile: ReaderProfile,
    appSettings: AppSettings,
    listState: LazyListState,
    onOpenSidebar: () -> Unit,
    onQuickRegister: () -> Unit,
    onOpenProfileSettings: () -> Unit,
    onGoToSearch: () -> Unit,
    onGoToShelf: () -> Unit,
    onSelectComic: (ComicEntry) -> Unit,
    onQuickUpdateReadingPage: (ComicEntry, Int) -> Unit,
    onToggleBottomAccessory: (Boolean) -> Unit = {}
) {
    val mat = LocalAppleMaterials.current
    val s = mat.scheme
    val themeColors = LocalIOS27Theme.current

    val readingNowList = remember(comics) { comics.filter { it.status == ReadingStatus.READING } }
    val readComicsList = remember(comics) { comics.filter { it.status == ReadingStatus.READ } }
    val totalPagesRead = remember(comics) {
        comics.sumOf {
            when (it.status) {
                ReadingStatus.READ -> it.pageCount
                ReadingStatus.READING -> it.currentPage
                ReadingStatus.WANT_TO_READ -> 0
            }
        }
    }

    val recentCols = appSettings.shelfColumns.coerceIn(2, 4)
    val recentRows = remember(comics, recentCols) { comics.take(6).chunked(recentCols) }

    TabScrollBackdropContainer(
        listState = listState,
        hasComics = true,
        showFloatingMenu = appSettings.showFloatingMenu,
        onOpenSidebar = onOpenSidebar,
        onQuickRegister = onQuickRegister
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = if (appSettings.showFloatingMenu) 62.dp else 12.dp,
                bottom = 88.dp
            ),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                // Header (Apenas um botão de registrar no topo, sem duplicidade)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    UserAvatar(
                        profile = profile,
                        sizeDp = 42,
                        modifier = Modifier.clickable { onOpenProfileSettings() }
                    )
                    Column {
                        Text(
                            text = "Quadrinheiro • ${profile.name}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = s.textSecondary
                        )
                        Text(
                            text = "Lendo Agora",
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Bold,
                            color = s.textPrimary,
                            letterSpacing = (-0.5).sp
                        )
                    }
                }
            }
            HorizontalDivider(color = mat.dividerColor, thickness = 0.5.dp)
        }

        // Resumo da Meta de Leitura (Apple HIG Activity Rings + Gauge)
        if (profile.showGoalOnHome) {
            item {
                val isDark = LocalIsDarkTheme.current
                val outerRingColor = themeColors.accent
                val innerRingColor = when (themeColors.appTheme) {
                    AppTheme.BLUE -> if (isDark) Color(0xFF30D5C8) else Color(0xFF00B4D8)   // Azul + Ciano/Turquesa
                    AppTheme.RED -> if (isDark) Color(0xFFFF9F0A) else Color(0xFFF59E0B)    // Vermelho + Âmbar/Laranja
                    AppTheme.GREEN -> if (isDark) Color(0xFF00C7BE) else Color(0xFF0D9488)  // Verde + Menta/Teal
                    AppTheme.PURPLE -> if (isDark) Color(0xFFFF375F) else Color(0xFFEC4899) // Roxo + Magenta/Rosa
                    AppTheme.ORANGE -> if (isDark) Color(0xFFFF453A) else Color(0xFFEF4444) // Laranja + Coral/Vermelho
                    AppTheme.WHITE -> if (isDark) Color(0xFF8E8E93) else Color(0xFFA1A1AA)  // Branco/Grafite + Prata
                    AppTheme.BLACK -> if (isDark) Color(0xFF636366) else Color(0xFF71717A)  // Titânio/Preto + Cinza Ardósia
                    else -> themeColors.accentSecondary
                }

                val outerRatio = (readComicsList.size.toFloat() / profile.yearlyGoal.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
                val targetPagesYear = (profile.dailyPagesGoal.coerceAtLeast(5) * 30).toFloat()
                val innerRatio = (totalPagesRead.toFloat() / targetPagesYear).coerceIn(0f, 1f)
                val pct = (outerRatio * 100f).roundToInt().coerceIn(0, 100)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .contentMaterialSurface(RoundedCornerShape(26.dp))
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f).padding(end = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(
                            text = "META DE 2026 • ${(profile.favoriteUniverse.ifBlank { "ACERVO" }).uppercase()}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = s.textSecondary
                        )
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "${readComicsList.size}",
                                fontSize = 24.sp,
                                fontFamily = SpaceGroteskTabularFamily,
                                fontWeight = FontWeight.Bold,
                                color = s.textPrimary
                            )
                            Text(
                                text = "de ${profile.yearlyGoal} quadrinhos lidos",
                                fontSize = 12.sp,
                                color = s.textSecondary,
                                modifier = Modifier.padding(bottom = 3.dp)
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(outerRingColor)
                                )
                                Text(
                                    text = "Obras ($pct%)",
                                    fontSize = 10.5.sp,
                                    color = s.textSecondary
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(innerRingColor)
                                )
                                Text(
                                    text = "$totalPagesRead págs",
                                    fontSize = 10.5.sp,
                                    color = s.textSecondary
                                )
                            }
                        }
                        Text(
                            text = "Ajustar meta anual",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = themeColors.accent,
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .clickable { onOpenProfileSettings() }
                        )
                    }

                    AppleActivityRingsGauge(
                        outerProgress = outerRatio,
                        innerProgress = innerRatio,
                        centerLabel = "$pct%",
                        sizeDp = 68.dp,
                        outerColor = outerRingColor,
                        innerColor = innerRingColor,
                        modifier = Modifier.clickable { onOpenProfileSettings() }
                    )
                }
            }
        }

        if (comics.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .contentMaterialSurface(RoundedCornerShape(28.dp))
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .liquidGlassNavSurface(CircleShape, elevation = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = null,
                            tint = s.textPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Sua estante está pronta",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = s.textPrimary
                        )
                        Text(
                            text = "Toque na lupa na barra inferior para pesquisar HQs e mangás online em tempo real ou cadastre manualmente.",
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            color = s.textSecondary
                        )
                    }

                    Row(
                        modifier = Modifier
                            .shadow(6.dp, RoundedCornerShape(999.dp))
                            .clip(RoundedCornerShape(999.dp))
                            .background(s.accentBg)
                            .clickable { onGoToSearch() }
                            .padding(horizontal = 24.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = s.accentText,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Buscar HQs Online",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = s.accentText
                        )
                    }
                }
            }
        } else {
            // Seção: Continuar Lendo com Stepper Nativo
            if (readingNowList.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Em leitura (${readingNowList.size})",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = s.textSecondary
                        )
                    }
                }

                items(readingNowList, key = { "reading-${it.id}" }) { item ->
                    val pct = ((item.currentPage.toFloat() / item.pageCount.coerceAtLeast(1).toFloat()) * 100f)
                        .roundToInt()
                        .coerceIn(0, 100)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .contentMaterialSurface(RoundedCornerShape(26.dp))
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BookCover(
                            src = item.coverUrl,
                            title = item.title,
                            authors = item.authors,
                            publisher = item.publisher,
                            year = item.releaseYear,
                            spine3d = appSettings.spine3dEffect,
                            modifier = Modifier
                                .width(80.dp)
                                .aspectRatio(2f / 3f)
                                .shadow(6.dp, RoundedCornerShape(10.dp))
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onSelectComic(item) }
                        )

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectComic(item) }
                            ) {
                                Text(
                                    text = "${item.publisher.uppercase()} • $pct% CONCLUÍDO",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = s.textSecondary
                                )
                                Text(
                                    text = item.title,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = s.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = item.authors,
                                    fontSize = 12.sp,
                                    color = s.textSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(99.dp))
                                        .background(
                                            if (s.isLight) Color.Black.copy(alpha = 0.10f)
                                            else Color.White.copy(alpha = 0.12f)
                                        )
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth(pct / 100f)
                                            .clip(RoundedCornerShape(99.dp))
                                            .background(s.accentColor)
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Pág. ${item.currentPage} de ${item.pageCount}",
                                        fontSize = 11.sp,
                                        fontFamily = SpaceGroteskTabularFamily,
                                        fontWeight = FontWeight.Medium,
                                        color = s.textSecondary
                                    )

                                    AppleStepper(
                                        value = item.currentPage,
                                        min = 0,
                                        max = item.pageCount.coerceAtLeast(1),
                                        step = 5,
                                        onChange = { nextPage ->
                                            onQuickUpdateReadingPage(item, nextPage)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Seção: Recentes na Biblioteca
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recentes na Biblioteca",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = s.textSecondary
                    )
                    Text(
                        text = "Ver todos (${comics.size})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = s.accentColor,
                        modifier = Modifier.clickable { onGoToShelf() }
                    )
                }
            }

            items(recentRows, key = { row -> row.firstOrNull()?.id ?: row.hashCode() }) { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(if (recentCols >= 4) 8.dp else 12.dp)
                ) {
                    rowItems.forEach { comic ->
                        ComicGridCard(
                            comic = comic,
                            appSettings = appSettings,
                            onClick = { onSelectComic(comic) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    repeat(recentCols - rowItems.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
    }
}

@Composable
private fun ComicGridCard(
    comic: ComicEntry,
    appSettings: AppSettings,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val mat = LocalAppleMaterials.current
    val s = mat.scheme
    val isCompact4Col = appSettings.shelfColumns >= 4
    val cardCorner = if (isCompact4Col) 10.dp else 12.dp
    val pct = ((comic.currentPage.toFloat() / comic.pageCount.coerceAtLeast(1).toFloat()) * 100f)
        .roundToInt()
        .coerceIn(0, 100)

    Column(
        modifier = modifier.clickable { onClick() },
        verticalArrangement = Arrangement.spacedBy(if (isCompact4Col) 4.dp else 6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .shadow(6.dp, RoundedCornerShape(cardCorner))
                .clip(RoundedCornerShape(cardCorner))
        ) {
            BookCover(
                src = comic.coverUrl,
                title = comic.title,
                authors = comic.authors,
                publisher = comic.publisher,
                year = comic.releaseYear,
                spine3d = appSettings.spine3dEffect,
                modifier = Modifier.fillMaxSize()
            )

            if (comic.favorite) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(if (isCompact4Col) 4.dp else 6.dp)
                        .size(if (isCompact4Col) 20.dp else 24.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.55f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        tint = Color(0xFFF43F5E),
                        modifier = Modifier.size(if (isCompact4Col) 10.dp else 12.dp)
                    )
                }
            }

            val issueNum = remember(comic.id, comic.title, comic.issueNumber) {
                SeriesSequenceEngine.inferIssueNumber(comic)
            }
            if (issueNum > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(if (isCompact4Col) 4.dp else 6.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.72f))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "#$issueNum",
                        fontSize = if (isCompact4Col) 8.5.sp else 9.5.sp,
                        fontFamily = SpaceGroteskTabularFamily,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            if (appSettings.showProgressOnCovers && comic.status == ReadingStatus.READING) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = if (isCompact4Col) 6.dp else 8.dp, vertical = if (isCompact4Col) 6.dp else 8.dp)
                        .fillMaxWidth()
                        .height(if (isCompact4Col) 5.dp else 6.dp)
                        .clip(RoundedCornerShape(99.dp))
                        .background(Color.Black.copy(alpha = 0.60f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(pct / 100f)
                            .clip(RoundedCornerShape(99.dp))
                            .background(s.accentColor)
                    )
                }
            }
        }

        Text(
            text = comic.title,
            fontSize = if (isCompact4Col) 11.sp else 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = s.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        if (appSettings.showRatingsOnGrid) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(15.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (comic.status == ReadingStatus.READ) {
                    StarRating(
                        rating = comic.rating,
                        readOnly = true,
                        size = if (isCompact4Col) "2xs" else "xs",
                        starColor = s.starColor
                    )
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }
                Text(
                    text = comic.status.shortLabel,
                    fontSize = if (isCompact4Col) 9.sp else 10.sp,
                    color = s.textTertiary,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Clip
                )
            }
        }
    }
}

/**
 * ABA 2: BIBLIOTECA (COM SWIFTUI PICKER .segmented E .menu ANCORADO)
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ShelfLibraryTab(
    comics: List<ComicEntry>,
    appSettings: AppSettings,
    shelfSection: String = "shelf",
    shelfStatus: String,
    shelfGenre: String,
    shelfLayout: String,
    shelfSort: String,
    shelfFilterQuery: String,
    listState: LazyListState,
    onOpenSidebar: () -> Unit,
    onQuickRegister: () -> Unit,
    onChangeShelfSection: (String) -> Unit = {},
    onChangeShelfStatus: (String) -> Unit,
    onChangeShelfGenre: (String) -> Unit,
    onToggleShelfLayout: () -> Unit,
    onChangeShelfLayout: (String) -> Unit = {},
    onChangeShelfSort: (String) -> Unit,
    onChangeShelfFilterQuery: (String) -> Unit,
    onGoToSearch: () -> Unit,
    onSelectComic: (ComicEntry) -> Unit
) {
    val mat = LocalAppleMaterials.current
    val s = mat.scheme

    val revealedSpoilers = remember { mutableStateMapOf<String, Boolean>() }

    val diaryGroups = remember(comics) {
        val logged = comics.filter { it.status != ReadingStatus.WANT_TO_READ }
            .sortedByDescending { it.readDate.ifBlank { it.addedAt.take(10) } }
        val map = LinkedHashMap<String, MutableList<Pair<ComicEntry, String>>>()
        val monthNames = listOf(
            "Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho",
            "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro"
        )
        logged.forEach { item ->
            val dateStr = item.readDate.ifBlank { item.addedAt.take(10).ifBlank { "2026-10-14" } }
            val parts = dateStr.split("-")
            val y = parts.getOrNull(0) ?: "2026"
            val mIdx = ((parts.getOrNull(1)?.toIntOrNull() ?: 10) - 1).coerceIn(0, 11)
            val day = parts.getOrNull(2)?.take(2) ?: "01"
            val label = "${monthNames[mIdx]} de $y"
            map.getOrPut(label) { mutableListOf() }.add(item to day)
        }
        map.entries.toList()
    }

    val filteredShelf = remember(comics, shelfStatus, shelfGenre, shelfFilterQuery, shelfSort) {
        comics.filter { c ->
            if (shelfStatus != "ALL" && c.status.key != shelfStatus) return@filter false
            if (shelfGenre != "Todos" && c.genre != shelfGenre) return@filter false
            val q = shelfFilterQuery.trim().lowercase()
            if (q.isNotEmpty()) {
                val matchT = c.title.lowercase().contains(q)
                val matchA = c.authors.lowercase().contains(q)
                val matchP = c.publisher.lowercase().contains(q)
                if (!matchT && !matchA && !matchP) return@filter false
            }
            true
        }.let { list ->
            when (shelfSort) {
                "RATING" -> list.sortedByDescending { it.rating }
                "TITLE" -> list.sortedBy { it.title.lowercase() }
                "YEAR" -> list.sortedByDescending { it.releaseYear }
                else -> list.sortedByDescending { it.addedAt }
            }
        }
    }

    val gridCols = appSettings.shelfColumns.coerceIn(2, 4)
    val gridRows = remember(filteredShelf, gridCols) { filteredShelf.chunked(gridCols) }
    val sequenceGroups = remember(filteredShelf) {
        SeriesSequenceEngine.groupLibraryBySequences(
            userComics = filteredShelf,
            catalogComics = CuratedCatalog.curatedComicsCatalog
        )
    }

    TabScrollBackdropContainer(
        listState = listState,
        hasComics = comics.isNotEmpty(),
        showFloatingMenu = appSettings.showFloatingMenu,
        onOpenSidebar = onOpenSidebar,
        onQuickRegister = onQuickRegister
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = if (appSettings.showFloatingMenu) 62.dp else 12.dp,
                bottom = 88.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            text = if (shelfSection == "shelf") "Acervo catalogado • ${filteredShelf.size} de ${comics.size}" else "Registro cronológico • ${diaryGroups.sumOf { it.value.size }} leituras",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = s.textSecondary
                        )
                        Text(
                            text = "Biblioteca",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = s.textPrimary,
                            letterSpacing = (-0.5).sp
                        )
                    }

                    if (shelfSection == "shelf") {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AppleMenuPicker(
                                value = shelfSort,
                                onChange = onChangeShelfSort,
                                options = listOf(
                                    SegmentOption("RECENT", "Mais Recentes", Icons.Default.Schedule),
                                    SegmentOption("RATING", "Melhor Avaliação", Icons.Default.Star),
                                    SegmentOption("TITLE", "Título (A–Z)", Icons.AutoMirrored.Filled.MenuBook),
                                    SegmentOption("YEAR", "Ano de Lançamento", Icons.Default.CalendarMonth)
                                )
                            )

                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .liquidGlassNavSurface(CircleShape, elevation = 6.dp)
                                    .clickable { onToggleShelfLayout() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = when (shelfLayout) {
                                        "grid" -> Icons.AutoMirrored.Filled.FormatListBulleted
                                        "list" -> LucideIcons.Layers
                                        else -> Icons.Default.GridView
                                    },
                                    contentDescription = "Alternar Grade, Lista ou Sequências",
                                    tint = s.textPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
                HorizontalDivider(color = mat.dividerColor, thickness = 0.5.dp)
            }

            // Primary Section Switcher: Estante vs Diário
            item {
                AppleSegmentedPicker(
                    value = shelfSection,
                    onChange = onChangeShelfSection,
                    options = listOf(
                        SegmentOption("shelf", "Estante (${comics.size})", LucideIcons.Library),
                        SegmentOption("diary", "Diário", LucideIcons.Calendar)
                    )
                )
            }

            if (shelfSection == "shelf") {
                // Segmented Picker + Filter TextField + Genre Pills
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AppleSegmentedPicker(
                    value = shelfStatus,
                    onChange = onChangeShelfStatus,
                    options = listOf(
                        SegmentOption("ALL", "Todos (${comics.size})"),
                        SegmentOption("READ", "Lidos (${comics.count { it.status == ReadingStatus.READ }})"),
                        SegmentOption("READING", "Lendo (${comics.count { it.status == ReadingStatus.READING }})"),
                        SegmentOption("WANT_TO_READ", "Fila (${comics.count { it.status == ReadingStatus.WANT_TO_READ }})")
                    )
                )

                AppleTextField(
                    value = shelfFilterQuery,
                    onChange = onChangeShelfFilterQuery,
                    placeholder = "Filtrar na sua Biblioteca por título, autor ou editora...",
                    leadingIcon = Icons.Default.Search,
                    useContentMaterial = true
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CuratedCatalog.shelfGenres.forEach { g ->
                        val selected = shelfGenre == g
                        Box(
                            modifier = Modifier
                                .then(
                                    if (selected) {
                                        Modifier
                                            .clip(RoundedCornerShape(999.dp))
                                            .background(s.accentBg)
                                    } else {
                                        Modifier.contentMaterialSurface(RoundedCornerShape(999.dp), elevation = 2.dp)
                                    }
                                )
                                .clickable { onChangeShelfGenre(g) }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = g,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (selected) s.accentText else s.textPrimary
                            )
                        }
                    }
                }

                // Seletor rápido de visualização: Grade / Lista / Sequências (#1, #2, #3...)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf(
                        Triple("grid", "Grade", Icons.Default.GridView),
                        Triple("list", "Lista", Icons.AutoMirrored.Filled.FormatListBulleted),
                        Triple("sequences", "Sequências (#1, #2...)", LucideIcons.Layers)
                    ).forEach { (layoutKey, label, icon) ->
                        val active = shelfLayout == layoutKey
                        Row(
                            modifier = Modifier
                                .then(
                                    if (active) {
                                        Modifier
                                            .clip(RoundedCornerShape(999.dp))
                                            .background(s.accentColor.copy(alpha = 0.16f))
                                            .border(1.dp, s.accentColor.copy(alpha = 0.45f), RoundedCornerShape(999.dp))
                                    } else {
                                        Modifier.contentMaterialSurface(RoundedCornerShape(999.dp), elevation = 2.dp)
                                    }
                                )
                                .clickable { onChangeShelfLayout(layoutKey) }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = if (active) s.accentColor else s.textSecondary,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = label,
                                fontSize = 11.5.sp,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                                color = if (active) s.accentColor else s.textPrimary
                            )
                        }
                    }
                }
            }
        }

        if (filteredShelf.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .contentMaterialSurface(RoundedCornerShape(26.dp))
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Nenhuma edição encontrada neste filtro",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = s.textPrimary
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(s.accentBg)
                            .clickable { onGoToSearch() }
                            .padding(horizontal = 20.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = "+ Buscar e Adicionar HQs",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = s.accentText
                        )
                    }
                }
            }
        } else if (shelfLayout == "grid") {
            items(
                gridRows,
                key = { row -> row.firstOrNull()?.id ?: row.hashCode() },
                contentType = { "grid_row" }
            ) { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(if (gridCols >= 4) 8.dp else 12.dp)
                ) {
                    rowItems.forEach { comic ->
                        ComicGridCard(
                            comic = comic,
                            appSettings = appSettings,
                            onClick = { onSelectComic(comic) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    repeat(gridCols - rowItems.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        } else if (shelfLayout == "sequences") {
            items(
                sequenceGroups,
                key = { group -> "seq-group-${group.sequenceKey}" },
                contentType = { "sequence_group_card" }
            ) { group ->
                val inShelfCount = group.savedCount
                val totalKnown = group.totalCount
                val seedComic = group.issues.firstOrNull { it.isSavedInLibrary }?.comic
                    ?: group.issues.firstOrNull()?.comic
                val publisherLabel = seedComic?.publisher.orEmpty()

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
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = LucideIcons.Layers,
                                    contentDescription = null,
                                    tint = s.accentColor,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = if (totalKnown > 1) "SEQUÊNCIA DE HQS" else "OBRA ÚNICA",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp,
                                    color = s.accentColor
                                )
                            }
                            Text(
                                text = group.baseSeriesTitle,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = s.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${group.primaryAuthor} • $publisherLabel",
                                fontSize = 12.sp,
                                color = s.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(s.accentColor.copy(alpha = 0.14f))
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = "$inShelfCount/$totalKnown na estante",
                                fontSize = 11.sp,
                                fontFamily = SpaceGroteskTabularFamily,
                                fontWeight = FontWeight.Bold,
                                color = s.accentColor
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        group.issues.forEach { seqItem ->
                            val userStatus = seqItem.savedEntry?.status
                            val statusText = when (userStatus) {
                                ReadingStatus.READ -> "✓ Lido"
                                ReadingStatus.READING -> "Lendo"
                                ReadingStatus.WANT_TO_READ -> "Fila"
                                null -> "+ Registrar"
                            }
                            val statusBg = when (userStatus) {
                                ReadingStatus.READ -> Color(0xFF10B981).copy(alpha = 0.20f)
                                ReadingStatus.READING -> s.accentColor.copy(alpha = 0.20f)
                                ReadingStatus.WANT_TO_READ -> Color(0xFFF59E0B).copy(alpha = 0.20f)
                                null -> if (s.isLight) Color.Black.copy(alpha = 0.07f) else Color.White.copy(alpha = 0.10f)
                            }
                            val statusColor = when (userStatus) {
                                ReadingStatus.READ -> if (s.isLight) Color(0xFF047857) else Color(0xFF34D399)
                                ReadingStatus.READING -> s.accentColor
                                ReadingStatus.WANT_TO_READ -> if (s.isLight) Color(0xFFB45309) else Color(0xFFFBBF24)
                                null -> s.textSecondary
                            }

                            Column(
                                modifier = Modifier
                                    .width(86.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (seqItem.isSavedInLibrary) {
                                            if (s.isLight) Color.White.copy(alpha = 0.72f)
                                            else Color.White.copy(alpha = 0.07f)
                                        } else {
                                            if (s.isLight) Color.Black.copy(alpha = 0.03f)
                                            else Color.White.copy(alpha = 0.03f)
                                        }
                                    )
                                    .border(
                                        width = if (seqItem.isSavedInLibrary) 1.dp else 0.6.dp,
                                        color = if (seqItem.isSavedInLibrary) s.accentColor.copy(alpha = 0.35f)
                                        else if (s.isLight) Color.Black.copy(alpha = 0.08f)
                                        else Color.White.copy(alpha = 0.08f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { onSelectComic(seqItem.comic) }
                                    .padding(6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(2f / 3f)
                                        .clip(RoundedCornerShape(8.dp))
                                ) {
                                    BookCover(
                                        src = seqItem.comic.coverUrl,
                                        title = seqItem.comic.title,
                                        authors = seqItem.comic.authors,
                                        publisher = seqItem.comic.publisher,
                                        year = seqItem.comic.releaseYear,
                                        spine3d = appSettings.spine3dEffect,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    if (seqItem.issueNumber > 0) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopStart)
                                                .padding(4.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(
                                                    if (seqItem.isSavedInLibrary) s.accentColor
                                                    else Color.Black.copy(alpha = 0.75f)
                                                )
                                                .padding(horizontal = 5.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "#${seqItem.issueNumber}",
                                                fontSize = 9.5.sp,
                                                fontFamily = SpaceGroteskTabularFamily,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }

                                Text(
                                    text = if (seqItem.issueNumber > 0) "Edição #${seqItem.issueNumber}" else seqItem.comic.title,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = s.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(statusBg)
                                        .padding(vertical = 2.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = statusText,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = statusColor,
                                        maxLines = 1
                                    )
                                }
                            }
                        }

                        // Botão rápido para adicionar a próxima edição (#N+1)
                        if (seedComic != null) {
                            val nextNum = ((group.issues.maxOfOrNull { it.issueNumber } ?: 0) + 1).coerceAtLeast(1)
                            Column(
                                modifier = Modifier
                                    .width(86.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (s.isLight) Color.White.copy(alpha = 0.50f)
                                        else Color.White.copy(alpha = 0.04f)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = s.accentColor.copy(alpha = 0.45f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable {
                                        val nextTemplate = SeriesSequenceEngine.createNextIssueTemplate(
                                            fromComic = seedComic,
                                            nextIssueNumber = nextNum
                                        )
                                        onSelectComic(nextTemplate)
                                    }
                                    .padding(6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(2f / 3f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(s.accentColor.copy(alpha = 0.10f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Text(
                                            text = "+ #$nextNum",
                                            fontSize = 14.sp,
                                            fontFamily = SpaceGroteskTabularFamily,
                                            fontWeight = FontWeight.Bold,
                                            color = s.accentColor
                                        )
                                        Text(
                                            text = "Próxima",
                                            fontSize = 9.5.sp,
                                            color = s.textSecondary
                                        )
                                    }
                                }

                                Text(
                                    text = "Edição #$nextNum",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = s.accentColor,
                                    maxLines = 1
                                )

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(s.accentBg)
                                        .padding(vertical = 2.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "+ Adicionar",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = s.accentText
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            items(
                filteredShelf,
                key = { "shelf-list-${it.id}" },
                contentType = { "shelf_list_item" }
            ) { comic ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .contentMaterialSurface(RoundedCornerShape(22.dp))
                        .clickable { onSelectComic(comic) }
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BookCover(
                        src = comic.coverUrl,
                        title = comic.title,
                        publisher = comic.publisher,
                        year = comic.releaseYear,
                        modifier = Modifier
                            .width(56.dp)
                            .aspectRatio(2f / 3f)
                            .clip(RoundedCornerShape(8.dp))
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = comic.title,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = s.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = comic.authors,
                            fontSize = 12.sp,
                            color = s.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (comic.status == ReadingStatus.READ) {
                                StarRating(
                                    rating = comic.rating,
                                    readOnly = true,
                                    size = "xs",
                                    starColor = s.starColor
                                )
                            }
                            Text(
                                text = "${comic.status.label} • ${comic.releaseYear}",
                                fontSize = 11.sp,
                                color = s.textTertiary
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = s.textTertiary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
        } else {
            // Diário items
            if (diaryGroups.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .contentMaterialSurface(RoundedCornerShape(26.dp))
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Nenhuma leitura registrada no Diário",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = s.textPrimary
                        )
                        Text(
                            text = "Ao adicionar quadrinhos na aba Buscar, eles aparecem organizados por mês aqui.",
                            fontSize = 12.sp,
                            color = s.textSecondary
                        )
                    }
                }
            } else {
                item(key = "shelf-diary-analytics-card") {
                    val ratedComics = remember(comics) { comics.filter { it.status == ReadingStatus.READ && it.rating > 0f } }
                    val avgRating = remember(ratedComics) {
                        if (ratedComics.isEmpty()) 0f
                        else ratedComics.map { it.rating }.average().toFloat()
                    }
                    val totalLoggedPages = remember(comics) {
                        comics.sumOf {
                            when (it.status) {
                                ReadingStatus.READ -> it.pageCount
                                ReadingStatus.READING -> it.currentPage
                                ReadingStatus.WANT_TO_READ -> 0
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .contentMaterialSurface(RoundedCornerShape(24.dp))
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.width(86.dp)
                        ) {
                            Text(
                                text = String.format(java.util.Locale.US, "%.1f", avgRating),
                                fontSize = 28.sp,
                                fontFamily = SpaceGroteskTabularFamily,
                                fontWeight = FontWeight.Bold,
                                color = s.textPrimary
                            )
                            StarRating(
                                rating = avgRating,
                                readOnly = true,
                                size = "xs",
                                starColor = s.starColor
                            )
                            Text(
                                text = "${ratedComics.size} avaliadas",
                                fontSize = 10.sp,
                                color = s.textTertiary
                            )
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val totalRated = ratedComics.size.coerceAtLeast(1)
                            for (star in 5 downTo 1) {
                                val countForStar = ratedComics.count { it.rating.roundToInt() == star }
                                val ratio = (countForStar.toFloat() / totalRated.toFloat()).coerceIn(0f, 1f)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "$star★",
                                        fontSize = 9.5.sp,
                                        fontFamily = SpaceGroteskTabularFamily,
                                        color = s.textSecondary,
                                        modifier = Modifier.width(18.dp)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(5.dp)
                                            .clip(RoundedCornerShape(99.dp))
                                            .background(
                                                if (s.isLight) Color.Black.copy(alpha = 0.08f)
                                                else Color.White.copy(alpha = 0.10f)
                                            )
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .fillMaxWidth(ratio.coerceAtLeast(if (countForStar > 0) 0.08f else 0f))
                                                .clip(RoundedCornerShape(99.dp))
                                                .background(s.starColor)
                                        )
                                    }
                                }
                            }
                        }

                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "PÁGINAS",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = s.textTertiary
                            )
                            Text(
                                text = "$totalLoggedPages",
                                fontSize = 18.sp,
                                fontFamily = SpaceGroteskTabularFamily,
                                fontWeight = FontWeight.Bold,
                                color = s.accentColor
                            )
                            Text(
                                text = "${comics.count { it.status == ReadingStatus.READ }} concluídas",
                                fontSize = 10.sp,
                                color = s.textSecondary
                            )
                        }
                    }
                }

                diaryGroups.forEach { (monthLabel, entries) ->
                    item(key = "shelf-diary-month-$monthLabel") {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = monthLabel,
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
                                entries.forEachIndexed { idx, (entry, displayDay) ->
                                    if (idx > 0) {
                                        HorizontalDivider(color = mat.dividerColor, thickness = 0.5.dp)
                                    }
                                    val isSpoilerHidden = appSettings.spoilerProtection &&
                                        entry.review.isNotBlank() &&
                                        !(revealedSpoilers[entry.id] ?: false)

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onSelectComic(entry) }
                                            .padding(16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Column(
                                            modifier = Modifier.width(36.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = displayDay,
                                                fontSize = 18.sp,
                                                fontFamily = SpaceGroteskTabularFamily,
                                                fontWeight = FontWeight.Bold,
                                                color = s.textPrimary
                                            )
                                            Text(
                                                text = entry.status.shortLabel.uppercase(),
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = s.textTertiary
                                            )
                                        }

                                        BookCover(
                                            src = entry.coverUrl,
                                            title = entry.title,
                                            modifier = Modifier
                                                .width(48.dp)
                                                .aspectRatio(2f / 3f)
                                                .clip(RoundedCornerShape(6.dp))
                                        )

                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = entry.title,
                                                    fontSize = 14.5.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = s.textPrimary,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                if (appSettings.allowRereads && entry.rereadCount > 0) {
                                                    Box(
                                                        modifier = Modifier
                                                            .inputWellSurface(RoundedCornerShape(999.dp))
                                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                                    ) {
                                                        Text(
                                                            text = "↻ ${entry.rereadCount}x",
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.SemiBold,
                                                            color = s.textPrimary
                                                        )
                                                    }
                                                }
                                            }

                                            Text(
                                                text = buildString {
                                                    append(entry.authors)
                                                    if (appSettings.showPagesInDiary) {
                                                        append(" • ${entry.publisher} • ${entry.pageCount} págs")
                                                    }
                                                },
                                                fontSize = 12.sp,
                                                color = s.textSecondary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )

                                            if (entry.status == ReadingStatus.READ && (entry.rating > 0f || entry.favorite)) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    if (entry.rating > 0f) {
                                                        StarRating(
                                                            rating = entry.rating,
                                                            readOnly = true,
                                                            size = "xs",
                                                            starColor = s.starColor
                                                        )
                                                    }
                                                    if (entry.favorite) {
                                                        Icon(
                                                            imageVector = Icons.Default.Favorite,
                                                            contentDescription = null,
                                                            tint = Color(0xFFF43F5E),
                                                            modifier = Modifier.size(12.dp)
                                                        )
                                                    }
                                                }
                                            }

                                            if (entry.tags.isNotEmpty()) {
                                                FlowRow(
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    entry.tags.forEach { t ->
                                                        Box(
                                                            modifier = Modifier
                                                                .inputWellSurface(RoundedCornerShape(999.dp))
                                                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                                        ) {
                                                            Text(
                                                                text = t,
                                                                fontSize = 10.sp,
                                                                fontWeight = FontWeight.Medium,
                                                                color = s.textPrimary
                                                            )
                                                        }
                                                    }
                                                }
                                            }

                                            if (entry.status == ReadingStatus.READ && entry.review.isNotBlank()) {
                                                Column(
                                                    modifier = Modifier
                                                        .padding(top = 4.dp)
                                                        .clickable(enabled = isSpoilerHidden) {
                                                            revealedSpoilers[entry.id] = true
                                                        }
                                                ) {
                                                    Text(
                                                        text = "“${entry.review}”",
                                                        fontSize = 12.sp,
                                                        lineHeight = 17.sp,
                                                        color = s.textSecondary,
                                                        modifier = if (isSpoilerHidden) Modifier.blur(6.dp) else Modifier
                                                    )
                                                    if (isSpoilerHidden) {
                                                        Text(
                                                            text = "Toque para revelar resenha",
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.SemiBold,
                                                            color = s.accentColor,
                                                            modifier = Modifier.padding(top = 2.dp)
                                                        )
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
        }
    }
    }
}

/**
 * ABA 3: DIÁRIO CRONOLÓGICO DE LEITURA
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DiaryChronologicalTab(
    comics: List<ComicEntry>,
    appSettings: AppSettings,
    listState: LazyListState,
    onOpenSidebar: () -> Unit,
    onQuickRegister: () -> Unit,
    onSelectComic: (ComicEntry) -> Unit
) {
    val mat = LocalAppleMaterials.current
    val s = mat.scheme
    val revealedSpoilers = remember { mutableStateMapOf<String, Boolean>() }

    // O Diário exibe apenas leituras reais (Lido ou Lendo), nunca itens de "Quero Ler"
    val diaryGroups = remember(comics) {
        val logged = comics.filter { it.status != ReadingStatus.WANT_TO_READ }
            .sortedByDescending { it.readDate.ifBlank { it.addedAt.take(10) } }
        val map = LinkedHashMap<String, MutableList<Pair<ComicEntry, String>>>()
        val monthNames = listOf(
            "Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho",
            "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro"
        )
        logged.forEach { item ->
            val dateStr = item.readDate.ifBlank { item.addedAt.take(10).ifBlank { "2026-10-14" } }
            val parts = dateStr.split("-")
            val y = parts.getOrNull(0) ?: "2026"
            val mIdx = ((parts.getOrNull(1)?.toIntOrNull() ?: 10) - 1).coerceIn(0, 11)
            val day = parts.getOrNull(2)?.take(2) ?: "01"
            val label = "${monthNames[mIdx]} de $y"
            map.getOrPut(label) { mutableListOf() }.add(item to day)
        }
        map.entries.toList()
    }

    TabScrollBackdropContainer(
        listState = listState,
        hasComics = comics.isNotEmpty(),
        showFloatingMenu = appSettings.showFloatingMenu,
        onOpenSidebar = onOpenSidebar,
        onQuickRegister = onQuickRegister
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = if (appSettings.showFloatingMenu) 62.dp else 12.dp,
                bottom = 88.dp
            ),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                Column(modifier = Modifier.padding(bottom = 14.dp)) {
                Text(
                    text = "Registro cronológico",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = s.textSecondary
                )
                Text(
                    text = "Diário",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = s.textPrimary,
                    letterSpacing = (-0.5).sp
                )
            }
            HorizontalDivider(color = mat.dividerColor, thickness = 0.5.dp)
        }

        if (diaryGroups.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .contentMaterialSurface(RoundedCornerShape(26.dp))
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Nenhuma leitura registrada no Diário",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = s.textPrimary
                    )
                    Text(
                        text = "Ao adicionar quadrinhos na aba Buscar, eles aparecem organizados por mês aqui.",
                        fontSize = 12.sp,
                        color = s.textSecondary
                    )
                }
            }
        } else {
            // Apple HIG Charting Data & Rating Indicators Summary Card
            item(key = "diary-analytics-card") {
                val ratedComics = remember(comics) { comics.filter { it.status == ReadingStatus.READ && it.rating > 0f } }
                val avgRating = remember(ratedComics) {
                    if (ratedComics.isEmpty()) 0f
                    else ratedComics.map { it.rating }.average().toFloat()
                }
                val totalLoggedPages = remember(comics) {
                    comics.sumOf {
                        when (it.status) {
                            ReadingStatus.READ -> it.pageCount
                            ReadingStatus.READING -> it.currentPage
                            ReadingStatus.WANT_TO_READ -> 0
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .contentMaterialSurface(RoundedCornerShape(24.dp))
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: Apple HIG Rating Indicator Summary
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.width(86.dp)
                    ) {
                        Text(
                            text = String.format( java.util.Locale.US, "%.1f", avgRating),
                            fontSize = 28.sp,
                            fontFamily = SpaceGroteskTabularFamily,
                            fontWeight = FontWeight.Bold,
                            color = s.textPrimary
                        )
                        StarRating(
                            rating = avgRating,
                            readOnly = true,
                            size = "xs",
                            starColor = s.starColor
                        )
                        Text(
                            text = "${ratedComics.size} avaliadas",
                            fontSize = 10.sp,
                            color = s.textTertiary
                        )
                    }

                    // Middle: 5-Star Histogram Bars (Apple App Store / Books style)
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val totalRated = ratedComics.size.coerceAtLeast(1)
                        for (star in 5 downTo 1) {
                            val countForStar = ratedComics.count { it.rating.roundToInt() == star }
                            val ratio = (countForStar.toFloat() / totalRated.toFloat()).coerceIn(0f, 1f)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "$star★",
                                    fontSize = 9.5.sp,
                                    fontFamily = SpaceGroteskTabularFamily,
                                    color = s.textSecondary,
                                    modifier = Modifier.width(18.dp)
                                )
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(5.dp)
                                        .clip(RoundedCornerShape(99.dp))
                                        .background(
                                            if (s.isLight) Color.Black.copy(alpha = 0.08f)
                                            else Color.White.copy(alpha = 0.10f)
                                        )
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth(ratio.coerceAtLeast(if (countForStar > 0) 0.08f else 0f))
                                            .clip(RoundedCornerShape(99.dp))
                                            .background(s.starColor)
                                    )
                                }
                            }
                        }
                    }

                    // Right: Total Pages & Logged Count Pill
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "PÁGINAS",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = s.textTertiary
                        )
                        Text(
                            text = "$totalLoggedPages",
                            fontSize = 18.sp,
                            fontFamily = SpaceGroteskTabularFamily,
                            fontWeight = FontWeight.Bold,
                            color = s.accentColor
                        )
                        Text(
                            text = "${comics.count { it.status == ReadingStatus.READ }} concluídas",
                            fontSize = 10.sp,
                            color = s.textSecondary
                        )
                    }
                }
            }

            diaryGroups.forEach { (monthLabel, entries) ->
                item(key = "diary-month-$monthLabel") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = monthLabel,
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
                            entries.forEachIndexed { idx, (entry, displayDay) ->
                                if (idx > 0) {
                                    HorizontalDivider(color = mat.dividerColor, thickness = 0.5.dp)
                                }
                                val isSpoilerHidden = appSettings.spoilerProtection &&
                                    entry.review.isNotBlank() &&
                                    !(revealedSpoilers[entry.id] ?: false)

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onSelectComic(entry) }
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column(
                                        modifier = Modifier.width(36.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = displayDay,
                                            fontSize = 18.sp,
                                            fontFamily = SpaceGroteskTabularFamily,
                                            fontWeight = FontWeight.Bold,
                                            color = s.textPrimary
                                        )
                                        Text(
                                            text = entry.status.shortLabel.uppercase(),
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = s.textTertiary
                                        )
                                    }

                                    BookCover(
                                        src = entry.coverUrl,
                                        title = entry.title,
                                        modifier = Modifier
                                            .width(48.dp)
                                            .aspectRatio(2f / 3f)
                                            .clip(RoundedCornerShape(6.dp))
                                    )

                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = entry.title,
                                                fontSize = 14.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = s.textPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f)
                                            )
                                            if (appSettings.allowRereads && entry.rereadCount > 0) {
                                                Box(
                                                    modifier = Modifier
                                                        .inputWellSurface(RoundedCornerShape(999.dp))
                                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = "↻ ${entry.rereadCount}x",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = s.textPrimary
                                                    )
                                                }
                                            }
                                        }

                                        Text(
                                            text = buildString {
                                                append(entry.authors)
                                                if (appSettings.showPagesInDiary) {
                                                    append(" • ${entry.publisher} • ${entry.pageCount} págs")
                                                }
                                            },
                                            fontSize = 12.sp,
                                            color = s.textSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        if (entry.status == ReadingStatus.READ && (entry.rating > 0f || entry.favorite)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                if (entry.rating > 0f) {
                                                    StarRating(
                                                        rating = entry.rating,
                                                        readOnly = true,
                                                        size = "xs",
                                                        starColor = s.starColor
                                                    )
                                                }
                                                if (entry.favorite) {
                                                    Icon(
                                                        imageVector = Icons.Default.Favorite,
                                                        contentDescription = null,
                                                        tint = Color(0xFFF43F5E),
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                }
                                            }
                                        }

                                        if (entry.tags.isNotEmpty()) {
                                            FlowRow(
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                verticalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                entry.tags.forEach { t ->
                                                    Box(
                                                        modifier = Modifier
                                                            .inputWellSurface(RoundedCornerShape(999.dp))
                                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                                    ) {
                                                        Text(
                                                            text = t,
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Medium,
                                                            color = s.textPrimary
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        if (entry.status == ReadingStatus.READ && entry.review.isNotBlank()) {
                                            Column(
                                                modifier = Modifier
                                                    .padding(top = 4.dp)
                                                    .clickable(enabled = isSpoilerHidden) {
                                                        revealedSpoilers[entry.id] = true
                                                    }
                                            ) {
                                                Text(
                                                    text = "“${entry.review}”",
                                                    fontSize = 12.sp,
                                                    lineHeight = 17.sp,
                                                    color = s.textSecondary,
                                                    modifier = if (isSpoilerHidden) Modifier.blur(6.dp) else Modifier
                                                )
                                                if (isSpoilerHidden) {
                                                    Text(
                                                        text = "Toque para revelar resenha",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = s.accentColor,
                                                        modifier = Modifier.padding(top = 2.dp)
                                                    )
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
    }
    }
}

/**
 * ABA 5: BUSCA INDEPENDENTE ONLINE MULTI-API (BOTÃO DA LUPA)
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchMultiApiTab(
    comics: List<ComicEntry>,
    appSettings: AppSettings,
    searchQuery: String,
    remoteResults: List<ComicEntry>,
    isSearchingApi: Boolean,
    searchSourceFilter: String,
    listState: LazyListState,
    onOpenSidebar: () -> Unit,
    onQuickRegister: () -> Unit,
    onChangeSearchQuery: (String) -> Unit,
    onTriggerImmediateSearch: () -> Unit = {},
    onChangeSearchSourceFilter: (String) -> Unit,
    onOpenManualModal: (String) -> Unit,
    onOpenCatalogSheetForComic: (ComicEntry, ReadingStatus) -> Unit,
    onSelectSavedComic: (ComicEntry) -> Unit,
    onOpenComicInfoModal: (ComicEntry) -> Unit = {}
) {
    val mat = LocalAppleMaterials.current
    val s = mat.scheme
    val focusManager = LocalFocusManager.current

    val savedExternalIds = remember(comics) {
        comics.mapNotNull { it.externalId.trim().lowercase().takeIf { id -> id.isNotEmpty() } }.toSet()
    }
    val savedIds = remember(comics) {
        comics.mapNotNull { it.id.trim().lowercase().takeIf { id -> id.isNotEmpty() } }.toSet()
    }
    val savedTitles = remember(comics) {
        comics.mapNotNull { it.title.trim().lowercase().takeIf { t -> t.isNotEmpty() } }.toSet()
    }

    val instantCuratedMatches = remember(searchQuery, savedExternalIds, savedIds, savedTitles) {
        val q = searchQuery.trim().lowercase()
        val base = CuratedCatalog.curatedComicsCatalog
        if (q.isEmpty()) {
            // Do NOT show comics already in the library unless the user explicitly searches!
            base.filterNot { item ->
                val extId = item.externalId.trim().lowercase()
                val id = item.id.trim().lowercase()
                val title = item.title.trim().lowercase()
                (extId.isNotEmpty() && savedExternalIds.contains(extId)) ||
                    (id.isNotEmpty() && savedIds.contains(id)) ||
                    (title.isNotEmpty() && savedTitles.contains(title))
            }
        } else {
            base.filter {
                it.title.lowercase().contains(q) ||
                    it.authors.lowercase().contains(q) ||
                    it.publisher.lowercase().contains(q) ||
                    it.genre.lowercase().contains(q)
            }
        }
    }

    val combinedSearchResults = remember(instantCuratedMatches, remoteResults, searchSourceFilter) {
        val seenKeys = HashSet<String>()
        val finalItems = mutableListOf<ComicEntry>()
        fun addUnique(item: ComicEntry) {
            val normTitle = item.title.lowercase().trim()
            val canonicalKey = SeriesSequenceEngine.canonicalDeduplicationKey(item)
            val extId = item.externalId.trim().lowercase()
            if (normTitle.isEmpty() ||
                seenKeys.contains(canonicalKey) ||
                seenKeys.contains(normTitle) ||
                (extId.isNotEmpty() && seenKeys.contains(extId))
            ) return
            seenKeys.add(canonicalKey)
            seenKeys.add(normTitle)
            if (extId.isNotEmpty()) seenKeys.add(extId)
            finalItems.add(item)
        }
        when (searchSourceFilter) {
            "ONLINE" -> {
                instantCuratedMatches.forEach(::addUnique)
                remoteResults.forEach(::addUnique)
            }
            "LOCAL" -> instantCuratedMatches.forEach(::addUnique)
            else -> {
                instantCuratedMatches.forEach(::addUnique)
                remoteResults.forEach(::addUnique)
            }
        }
        finalItems
    }

    TabScrollBackdropContainer(
        listState = listState,
        hasComics = comics.isNotEmpty(),
        showFloatingMenu = appSettings.showFloatingMenu,
        onOpenSidebar = onOpenSidebar,
        onQuickRegister = onQuickRegister
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = if (appSettings.showFloatingMenu) 62.dp else 12.dp,
                bottom = 88.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(
                    modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "Catálogo de Quadrinhos e Mangás",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = s.textSecondary
                    )
                    Text(
                        text = "Buscar",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = s.textPrimary,
                        letterSpacing = (-0.5).sp
                    )
                }

                Row(
                    modifier = Modifier
                        .liquidGlassNavSurface(RoundedCornerShape(999.dp), elevation = 6.dp)
                        .clickable { onOpenManualModal("") }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = s.textPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Manual",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = s.textPrimary
                    )
                }
            }
            HorizontalDivider(color = mat.dividerColor, thickness = 0.5.dp)
        }

        // Barra de Busca em Liquid Glass + Sugestões Rápidas
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlassNavSurface(RoundedCornerShape(999.dp), elevation = 8.dp)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = LucideIcons.Search,
                        contentDescription = null,
                        tint = s.textSecondary,
                        modifier = Modifier.size(16.dp)
                    )

                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "Buscar quadrinhos, mangás, autores ou editoras...",
                                fontSize = 13.5.sp,
                                color = s.textTertiary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = onChangeSearchQuery,
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(
                                onSearch = {
                                    onTriggerImmediateSearch()
                                    focusManager.clearFocus()
                                }
                            ),
                            textStyle = TextStyle(
                                fontFamily = PlusJakartaSansFamily,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = s.textPrimary
                            ),
                            cursorBrush = SolidColor(s.accentColor),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    if (isSearchingApi) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = s.accentColor
                        )
                    }

                    if (searchQuery.isNotEmpty()) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Limpar",
                            tint = s.textSecondary,
                            modifier = Modifier
                                .size(18.dp)
                                .clickable { onChangeSearchQuery("") }
                        )
                    }
                }

                // Quick suggestion pills
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CuratedCatalog.quickSearchSuggestions.forEach { tag ->
                        val active = searchQuery.equals(tag, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .then(
                                    if (active) {
                                        Modifier
                                            .clip(RoundedCornerShape(999.dp))
                                            .background(s.accentBg)
                                    } else {
                                        Modifier.contentMaterialSurface(RoundedCornerShape(999.dp), elevation = 2.dp)
                                    }
                                )
                                .clickable { onChangeSearchQuery(tag) }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = tag,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (active) s.accentText else s.textPrimary
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (searchQuery.trim().isNotEmpty()) {
                            "Resultados para \"$searchQuery\" (${combinedSearchResults.size})"
                        } else {
                            "Destaques do catálogo"
                        },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = s.textSecondary
                    )
                    if (isSearchingApi) {
                        Text(
                            text = "Buscando edições...",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = s.accentColor
                        )
                    }
                }
            }
        }

        if (combinedSearchResults.isEmpty() && !isSearchingApi) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .contentMaterialSurface(RoundedCornerShape(24.dp))
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Nenhum resultado encontrado para “$searchQuery”",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = s.textPrimary
                    )
                    Text(
                        text = "Você pode cadastrar qualquer edição personalizada usando o botão Manual.",
                        fontSize = 12.sp,
                        color = s.textSecondary
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(s.accentBg)
                            .clickable { onOpenManualModal(searchQuery) }
                            .padding(horizontal = 20.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = "+ Cadastrar “$searchQuery” Manualmente",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = s.accentText
                        )
                    }
                }
            }
        } else {
            items(
                combinedSearchResults,
                key = { item -> (item.externalId.ifBlank { item.id } + "_" + item.title).hashCode() },
                contentType = { "search_result_item" }
            ) { item ->
                val savedEntry = comics.firstOrNull {
                    it.externalId == item.externalId || it.id == item.externalId
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .contentMaterialSurface(RoundedCornerShape(22.dp))
                        .clickable { onOpenComicInfoModal(item) }
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    BookCover(
                        src = item.coverUrl,
                        title = item.title,
                        authors = item.authors,
                        publisher = item.publisher,
                        year = item.releaseYear,
                        spine3d = appSettings.spine3dEffect,
                        modifier = Modifier
                            .width(64.dp)
                            .aspectRatio(2f / 3f)
                            .shadow(4.dp, RoundedCornerShape(8.dp))
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onOpenComicInfoModal(item) }
                    )

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        val issueNum = remember(item.id, item.title, item.issueNumber) {
                            SeriesSequenceEngine.inferIssueNumber(item)
                        }
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .inputWellSurface(RoundedCornerShape(999.dp))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = item.format.ifBlank { item.publisher },
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = s.textPrimary,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (issueNum > 0) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(s.accentColor.copy(alpha = 0.14f))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "Sequência • Ed. #$issueNum",
                                        fontSize = 9.5.sp,
                                        fontFamily = SpaceGroteskTabularFamily,
                                        fontWeight = FontWeight.Bold,
                                        color = s.accentColor,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                            Box(
                                modifier = Modifier.padding(vertical = 2.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text(
                                    text = item.genre,
                                    fontSize = 10.sp,
                                    color = s.textTertiary,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Text(
                            text = item.title,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = s.textPrimary,
                            lineHeight = 19.sp
                        )

                        Text(
                            text = item.authors,
                            fontSize = 12.sp,
                            color = s.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            text = "${item.publisher} • ${item.releaseYear} • ${item.pageCount} págs",
                            fontSize = 11.sp,
                            color = s.textTertiary
                        )

                        if (item.synopsis.isNotBlank()) {
                            Text(
                                text = item.synopsis,
                                fontSize = 11.5.sp,
                                lineHeight = 16.sp,
                                color = s.textSecondary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Text(
                            text = "Toque para ver sinopse e informações ›",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = s.accentColor
                        )

                        if (savedEntry != null) {
                            Row(
                                modifier = Modifier
                                    .padding(top = 4.dp)
                                    .inputWellSurface(RoundedCornerShape(999.dp))
                                    .clickable { onSelectSavedComic(savedEntry) }
                                    .padding(horizontal = 14.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color(0xFF34D399),
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Editar na Biblioteca (${savedEntry.status.shortLabel})",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = s.textPrimary
                                )
                            }
                        } else {
                            Row(
                                modifier = Modifier.padding(top = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AppleCapsuleButton(
                                    label = "+ Lido",
                                    prominent = true,
                                    onClick = {
                                        onOpenCatalogSheetForComic(item, ReadingStatus.READ)
                                    }
                                )

                                AppleCapsuleButton(
                                    label = "Lendo",
                                    prominent = false,
                                    onClick = {
                                        onOpenCatalogSheetForComic(item, ReadingStatus.READING)
                                    }
                                )

                                AppleCapsuleButton(
                                    label = "Quero Ler",
                                    prominent = false,
                                    onClick = {
                                        onOpenCatalogSheetForComic(item, ReadingStatus.WANT_TO_READ)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    }
}
