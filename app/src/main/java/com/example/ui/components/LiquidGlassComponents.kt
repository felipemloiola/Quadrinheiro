package com.example.ui.components

// ... (all previous imports and declarations unchanged above)

/**
 * Sculptural Wave Wallpaper + SwiftUI `.backgroundExtensionEffect()` backdrop.
 * Optimized to reduce GPU load when `backgroundExtensionEnabled` is true by dialing back blur and light radius
 * while keeping the same visual identity.
 */
@Composable
fun SculpturalWaveBackdrop(
    featuredCoverUrl: String?,
    backgroundExtensionEnabled: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit = {}
) {
    val mat = LocalAppleMaterials.current
    val s = mat.scheme
    val hasActiveReadingCover = backgroundExtensionEnabled && !featuredCoverUrl.isNullOrBlank()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(s.baseBg)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(s.baseBg)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Base gradient kept intact
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(s.gradLinearStart, s.gradLinearMid, s.gradLinearEnd),
                        start = Offset(0f, 0f),
                        end = Offset(size.width, size.height)
                    )
                )
                if (hasActiveReadingCover) {
                    // Slightly smaller and softer radial lights to avoid overdraw / banding
                    val topRadius = size.width * 0.65f
                    val bottomRadius = size.width * 0.65f
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(s.gradTopRight.copy(alpha = s.gradTopRight.alpha * 0.80f), Color.Transparent),
                            center = Offset(size.width * 0.82f, size.height * 0.14f),
                            radius = topRadius
                        ),
                        center = Offset(size.width * 0.82f, size.height * 0.14f),
                        radius = topRadius
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(s.gradBottomLeft.copy(alpha = s.gradBottomLeft.alpha * 0.80f), Color.Transparent),
                            center = Offset(size.width * 0.15f, size.height * 0.82f),
                            radius = bottomRadius
                        ),
                        center = Offset(size.width * 0.15f, size.height * 0.82f),
                        radius = bottomRadius
                    )

                    // Sculptural ribbons remain but with slightly reduced alpha to avoid heavy blending
                    val sx = size.width / 600f
                    val sy = size.height / 900f
                    val pathA = Path().apply {
                        moveTo(-80f * sx, 180f * sy)
                        cubicTo(180f * sx, 290f * sy, 340f * sx, 40f * sy, 680f * sx, 220f * sy)
                        lineTo(680f * sx, 950f * sy)
                        lineTo(-80f * sx, 950f * sy)
                        close()
                    }
                    drawPath(pathA, color = s.ribbonA.copy(alpha = s.ribbonA.alpha * 0.35f))

                    val pathB = Path().apply {
                        moveTo(-80f * sx, 520f * sy)
                        cubicTo(220f * sx, 380f * sy, 410f * sx, 690f * sy, 680f * sx, 470f * sy)
                        lineTo(680f * sx, 950f * sy)
                        lineTo(-80f * sx, 950f * sy)
                        close()
                    }
                    drawPath(pathB, color = s.ribbonB.copy(alpha = s.ribbonB.alpha * 0.35f))
                }
            }

            if (hasActiveReadingCover && featuredCoverUrl != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp) // a bit shorter to reduce blur area
                        .alpha(0.26f)   // slightly less intense overlay
                ) {
                    val context = LocalContext.current
                    val ambientScaffoldReq = remember(featuredCoverUrl, context) {
                        val clean = featuredCoverUrl.replace("http://", "https://")
                        ImageRequest.Builder(context)
                            .data(clean)
                            .size(28, 42)
                            .precision(coil.size.Precision.INEXACT)
                            .crossfade(false)
                            .allowHardware(true)
                            .allowRgb565(true)
                            .memoryCacheKey(clean + "_scaffold_amb")
                            .diskCacheKey(clean + "_scaffold_amb")
                            .memoryCachePolicy(CachePolicy.ENABLED)
                            .diskCachePolicy(CachePolicy.ENABLED)
                            .build()
                    }
                    AsyncImage(
                        model = ambientScaffoldReq,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .blur(12.dp) // lower blur radius to reduce GPU cost
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(s.baseBg.copy(alpha = 0.30f), s.baseBg)
                                )
                            )
                    )
                }
            }
        }

        content()
    }
}
