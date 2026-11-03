package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import com.example.model.*
import com.example.ui.screens.HomeReadingNowTab
import com.example.ui.theme.LocalAppMotion
import com.example.ui.theme.LocalIOS27Theme

internal val LocalHomeSkeleton = staticCompositionLocalOf<State<Float>?> { null }

@Composable
internal fun Modifier.homeSkeleton(verticalInset: Float = 0f): Modifier {
    val pulse = LocalHomeSkeleton.current ?: return this
    val color = LocalIOS27Theme.current.textSecondary
    return this.clearAndSetSemantics {}.drawWithContent {
        drawRoundRect(color.copy(alpha = 0.18f * pulse.value),
            topLeft = androidx.compose.ui.geometry.Offset(0f, size.height * verticalInset),
            size = androidx.compose.ui.geometry.Size(size.width, size.height * (1f - 2f * verticalInset)),
            cornerRadius = CornerRadius(6.dp.toPx()))
    }
}

@Composable
internal fun HomeSkeletonBlock(modifier: Modifier) { Box(modifier.homeSkeleton()) }

/** Measures the real Home components; only their foreground drawing is replaced. */
@Composable
fun HomeLoadingSkeleton(comics: List<ComicEntry>, profile: ReaderProfile, appSettings: AppSettings) {
    val pulse = if (LocalAppMotion.current.reduced) remember { mutableFloatStateOf(0.7f) } else {
        rememberInfiniteTransition(label = "homeLoading").animateFloat(0.45f, 0.9f,
            infiniteRepeatable(tween(850), RepeatMode.Reverse), label = "homePlaceholder")
    }
    val placeholders = remember {
        List(6) { i -> ComicEntry("loading-$i", "", "Título do quadrinho", authors = "Autor da obra", publisher = "Editora",
            status = if (i == 0) ReadingStatus.READING else ReadingStatus.WANT_TO_READ, pageCount = 160) }
    }
    CompositionLocalProvider(LocalHomeSkeleton provides pulse) {
        HomeReadingNowTab(comics.ifEmpty { placeholders }, profile, appSettings, rememberLazyListState(),
            onOpenSidebar = {}, onQuickRegister = {}, onOpenProfileSettings = {}, onGoToSearch = {},
            onGoToShelf = {}, onSelectComic = {}, onQuickUpdateReadingPage = { _, _ -> })
    }
}

@Composable
internal fun HomeSkeletonText(
    text: String, modifier: Modifier = Modifier, color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified, fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null, fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified, textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null, lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip, softWrap: Boolean = true, maxLines: Int = Int.MAX_VALUE,
    style: TextStyle = LocalTextStyle.current
) {
    Text(text, modifier.homeSkeleton(0.2f), color, fontSize, fontStyle, fontWeight, fontFamily,
        letterSpacing, textDecoration, textAlign, lineHeight, overflow, softWrap, maxLines, style = style)
}
