package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.example.R
import com.example.data.CuratedCatalog
import com.example.model.AuthFeedbackBanner
import com.example.model.AuthFeedbackType
import com.example.model.AuthFormValidator
import com.example.model.AuthMode
import com.example.ui.components.LocalLiquidGlassBackdrop
import com.example.ui.components.LucideIcons
import com.example.ui.components.contentMaterialSurface
import com.example.ui.components.inputWellSurface
import com.example.ui.components.liquidGlass
import com.example.ui.components.liquidGlassNavSurface
import com.example.ui.components.tintedLiquidBlueButton
import com.example.ui.theme.LocalAppSettings
import com.example.ui.theme.LocalAppleMaterials
import com.example.ui.theme.LocalIOS27Theme
import com.example.ui.theme.PlusJakartaSansFamily
import com.example.ui.theme.SpaceGroteskTabularFamily
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.shapes.Capsule
import com.kyant.shapes.ContinuousCapsule
import com.kyant.shapes.ContinuousRoundedRectangle
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AuthScreen(
    isLoading: Boolean,
    feedbackMessage: String?,
    isFeedbackError: Boolean,
    isLinkingFromGuest: Boolean = false,
    initialMode: AuthMode = AuthMode.LOGIN,
    onClearFeedback: () -> Unit,
    onLoginWithEmail: (email: String, password: String) -> Unit,
    onRegisterWithEmail: (name: String, email: String, password: String) -> Unit,
    onLoginWithGoogle: () -> Unit,
    onContinueAsGuest: () -> Unit,
    onResetPassword: (email: String) -> Unit,
    onDismissModal: (() -> Unit)? = null
) {
    val mat = LocalAppleMaterials.current
    val s = mat.scheme
    val themeColors = LocalIOS27Theme.current
    val appSettings = LocalAppSettings.current
    val focusManager = LocalFocusManager.current

    var authMode by rememberSaveable { mutableStateOf(initialMode) }
    var nameInput by rememberSaveable { mutableStateOf("") }
    var emailInput by rememberSaveable { mutableStateOf("") }
    var passwordInput by rememberSaveable { mutableStateOf("") }
    var confirmPasswordInput by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var confirmPasswordVisible by rememberSaveable { mutableStateOf(false) }
    var localBanner by remember { mutableStateOf<AuthFeedbackBanner?>(null) }

    LaunchedEffect(initialMode) {
        authMode = initialMode
    }

    val authBackdrop = rememberLayerBackdrop()

    val submitPrimaryAction = {
        focusManager.clearFocus()
        onClearFeedback()
        val validationError = AuthFormValidator.validateForm(
            mode = authMode,
            name = nameInput,
            email = emailInput,
            password = passwordInput,
            confirmPassword = confirmPasswordInput
        )
        if (validationError != null) {
            localBanner = AuthFeedbackBanner(
                message = validationError,
                type = AuthFeedbackType.ERROR
            )
        } else {
            localBanner = null
            if (authMode == AuthMode.LOGIN) {
                onLoginWithEmail(emailInput.trim(), passwordInput)
            } else {
                onRegisterWithEmail(nameInput.trim(), emailInput.trim(), passwordInput)
            }
        }
    }

    val activeBanner: AuthFeedbackBanner? = remember(localBanner, feedbackMessage, isFeedbackError) {
        when {
            localBanner != null -> localBanner
            !feedbackMessage.isNullOrBlank() -> AuthFeedbackBanner(
                message = feedbackMessage,
                type = if (isFeedbackError) AuthFeedbackType.ERROR else AuthFeedbackType.SUCCESS
            )
            else -> null
        }
    }

    CompositionLocalProvider(
        LocalLiquidGlassBackdrop provides authBackdrop
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(themeColors.bgPrimary)
                .testTag("auth_screen_root")
        ) {
            // 1. Camada de Fundo Imersiva gravada no authBackdrop (respeita tema Claro/Escuro e Reduzir Transparência)
            AuthBackdropLayer(
                backdrop = authBackdrop,
                reduceTransparency = appSettings.reduceTransparency,
                isLight = s.isLight
            )

            // 2. Conteúdo Principal em Liquid Glass (Irmão do authBackdrop para refração real sem recursão)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 460.dp)
                ) {
                    if (onDismissModal != null) {
                        val closeCd = stringResource(R.string.auth_cd_close_modal)
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(48.dp)
                                .liquidGlassNavSurface(shape = CircleShape, elevation = 6.dp)
                                .clickable(role = Role.Button) { onDismissModal() }
                                .semantics { contentDescription = closeCd }
                                .testTag("auth_close_modal_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = null,
                                tint = themeColors.textPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(6.dp))

                        // 2.1 Cabeçalho da Marca
                        AuthBrandHeader(isLinkingFromGuest = isLinkingFromGuest)

                        Spacer(modifier = Modifier.height(22.dp))

                        // 2.2 Card Central usando os modificadores oficiais Liquid Glass do projeto
                        val cardShape = ContinuousRoundedRectangle(28.dp)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .liquidGlass(
                                    radiusX = 28f,
                                    radiusY = 28f,
                                    shape = cardShape,
                                    elevation = 16.dp
                                )
                                .contentMaterialSurface(shape = cardShape)
                                .padding(horizontal = 20.dp, vertical = 22.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // 2.3 Segmented Pill Toggle ("Entrar" vs "Criar Conta")
                            AuthModeSegmentedToggle(
                                selectedMode = authMode,
                                onSelectMode = { nextMode ->
                                    authMode = nextMode
                                    localBanner = null
                                    onClearFeedback()
                                }
                            )

                            // 2.4 Banner de Feedback Estruturado
                            AuthFeedbackBannerCard(banner = activeBanner)

                            // 2.5 Grupo de Campos de Credenciais
                            AuthCredentialsFieldsSection(
                                authMode = authMode,
                                nameInput = nameInput,
                                emailInput = emailInput,
                                passwordInput = passwordInput,
                                confirmPasswordInput = confirmPasswordInput,
                                passwordVisible = passwordVisible,
                                confirmPasswordVisible = confirmPasswordVisible,
                                onNameChange = {
                                    nameInput = it
                                    if (localBanner != null) localBanner = null
                                },
                                onEmailChange = {
                                    emailInput = it
                                    if (localBanner != null) localBanner = null
                                },
                                onPasswordChange = {
                                    passwordInput = it
                                    if (localBanner != null) localBanner = null
                                },
                                onConfirmPasswordChange = {
                                    confirmPasswordInput = it
                                    if (localBanner != null) localBanner = null
                                },
                                onTogglePasswordVisible = { passwordVisible = !passwordVisible },
                                onToggleConfirmPasswordVisible = { confirmPasswordVisible = !confirmPasswordVisible },
                                onForgotPassword = {
                                    focusManager.clearFocus()
                                    localBanner = null
                                    onResetPassword(emailInput.trim())
                                },
                                onSubmit = submitPrimaryAction
                            )

                            // 2.6 Ações Principais, Login com Google e Modo Convidado
                            AuthPrimaryActionsSection(
                                authMode = authMode,
                                isLoading = isLoading,
                                isLinkingFromGuest = isLinkingFromGuest,
                                backdrop = authBackdrop,
                                onSubmitPrimary = submitPrimaryAction,
                                onLoginWithGoogle = {
                                    focusManager.clearFocus()
                                    localBanner = null
                                    onClearFeedback()
                                    onLoginWithGoogle()
                                },
                                onContinueAsGuest = {
                                    focusManager.clearFocus()
                                    localBanner = null
                                    onClearFeedback()
                                    if (onDismissModal != null) {
                                        onDismissModal()
                                    } else {
                                        onContinueAsGuest()
                                    }
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun AuthBackdropLayer(
    backdrop: com.kyant.backdrop.backdrops.LayerBackdrop,
    reduceTransparency: Boolean,
    isLight: Boolean
) {
    val mat = LocalAppleMaterials.current
    val s = mat.scheme
    val themeColors = LocalIOS27Theme.current
    val context = LocalContext.current

    val showcaseCovers = remember {
        CuratedCatalog.curatedComicsCatalog
            .filter { it.coverUrl.isNotBlank() }
            .take(3)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .layerBackdrop(backdrop)
            .background(themeColors.bgPrimary)
    ) {
        if (!reduceTransparency && !themeColors.isAllBlack) {
            val infiniteTransition = rememberInfiniteTransition(label = "authLiquidOrbs")
            val orbPhaseState = infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = (2f * Math.PI).toFloat(),
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 16000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "orbPhase"
            )
            val pulseState = infiniteTransition.animateFloat(
                initialValue = 0.88f,
                targetValue = 1.14f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 5400, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "orbPulse"
            )

            // Silhuetas de capas do catálogo com desfoque atmosférico para refração óptica real
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 28.dp)
                    .alpha(if (isLight) 0.16f else 0.22f)
                    .blur(26.dp),
                horizontalArrangement = Arrangement.spacedBy(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                showcaseCovers.forEachIndexed { idx, comic ->
                    val req = remember(comic.coverUrl, context) {
                        ImageRequest.Builder(context)
                            .data(comic.coverUrl)
                            .size(120, 180)
                            .allowHardware(true)
                            .allowRgb565(true)
                            .memoryCachePolicy(CachePolicy.ENABLED)
                            .diskCachePolicy(CachePolicy.ENABLED)
                            .build()
                    }
                    AsyncImage(
                        model = req,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(if (idx == 1) 0.78f else 0.64f)
                            .clip(RoundedCornerShape(28.dp))
                    )
                }
            }

            // Orbes luminosos com leitura diferida (() -> Float) apenas na fase de draw
            AuthAnimatedOrbsCanvas(
                phaseProvider = { orbPhaseState.value },
                pulseProvider = { pulseState.value },
                primaryAccent = themeColors.accent,
                secondaryAccent = themeColors.accentSecondary,
                gradStart = s.gradLinearStart,
                gradMid = s.gradLinearMid,
                gradEnd = s.gradLinearEnd,
                isLight = isLight
            )
        } else {
            // Modo econômico / Reduzir Transparência: gradiente estático sem custo de animação contínua
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(s.gradLinearStart, s.gradLinearMid, s.gradLinearEnd),
                        start = Offset.Zero,
                        end = Offset(size.width, size.height)
                    ),
                    alpha = if (themeColors.isAllBlack) 0.1f else 0.85f
                )
            }
        }

        // Vinheta atmosférica adaptativa ao tema Claro/Escuro
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            themeColors.bgPrimary.copy(alpha = if (isLight) 0.28f else 0.34f),
                            themeColors.bgPrimary.copy(alpha = if (isLight) 0.52f else 0.58f),
                            themeColors.bgPrimary.copy(alpha = if (isLight) 0.82f else 0.88f)
                        )
                    )
                )
        )
    }
}

@Composable
private fun AuthAnimatedOrbsCanvas(
    phaseProvider: () -> Float,
    pulseProvider: () -> Float,
    primaryAccent: Color,
    secondaryAccent: Color,
    gradStart: Color,
    gradMid: Color,
    gradEnd: Color,
    isLight: Boolean
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(gradStart, gradMid, gradEnd),
                start = Offset.Zero,
                end = Offset(size.width, size.height)
            ),
            alpha = if (isLight) 0.75f else 0.55f
        )

        val phase = phaseProvider()
        val pulse = pulseProvider()
        val w = size.width
        val h = size.height
        val maxDim = size.maxDimension
        val alphaMult = if (isLight) 0.65f else 1f

        val orb1Center = Offset(
            x = w * (0.25f + 0.14f * cos(phase)),
            y = h * (0.20f + 0.10f * sin(phase))
        )
        val orb2Center = Offset(
            x = w * (0.78f + 0.12f * sin(phase * 0.85f)),
            y = h * (0.48f + 0.12f * cos(phase * 0.85f))
        )
        val orb3Center = Offset(
            x = w * (0.42f + 0.15f * cos(phase * 1.15f + 1.4f)),
            y = h * (0.82f + 0.08f * sin(phase * 1.15f + 1.4f))
        )

        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    primaryAccent.copy(alpha = 0.32f * alphaMult),
                    Color(0xFF2563EB).copy(alpha = 0.12f * alphaMult),
                    Color.Transparent
                ),
                center = orb1Center,
                radius = maxDim * 0.62f * pulse
            )
        )

        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF8B5CF6).copy(alpha = 0.28f * alphaMult),
                    secondaryAccent.copy(alpha = 0.12f * alphaMult),
                    Color.Transparent
                ),
                center = orb2Center,
                radius = maxDim * 0.58f * (2f - pulse * 0.85f)
            )
        )

        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF06B6D4).copy(alpha = 0.25f * alphaMult),
                    Color(0xFF10B981).copy(alpha = 0.08f * alphaMult),
                    Color.Transparent
                ),
                center = orb3Center,
                radius = maxDim * 0.55f * pulse
            )
        )
    }
}

@Composable
private fun AuthBrandHeader(isLinkingFromGuest: Boolean) {
    val themeColors = LocalIOS27Theme.current
    val emblemShape = ContinuousRoundedRectangle(24.dp)
    val emblemCd = stringResource(R.string.auth_cd_brand_emblem)

    Box(
        modifier = Modifier
            .size(76.dp)
            .liquidGlass(
                radiusX = 24f,
                radiusY = 24f,
                shape = emblemShape,
                elevation = 14.dp
            )
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        themeColors.accent.copy(alpha = 0.34f),
                        themeColors.accentSecondary.copy(alpha = 0.24f)
                    )
                ),
                shape = emblemShape
            )
            .border(
                width = 1.1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.45f),
                        Color.White.copy(alpha = 0.10f)
                    )
                ),
                shape = emblemShape
            )
            .semantics { contentDescription = emblemCd },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = LucideIcons.OpenBookFilled,
            contentDescription = null,
            tint = themeColors.textPrimary,
            modifier = Modifier.size(34.dp)
        )
    }

    Spacer(modifier = Modifier.height(14.dp))

    Text(
        text = stringResource(R.string.app_name),
        fontFamily = SpaceGroteskTabularFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        letterSpacing = (-0.8).sp,
        color = themeColors.textPrimary,
        textAlign = TextAlign.Center,
        modifier = Modifier.semantics { heading() }
    )

    Spacer(modifier = Modifier.height(4.dp))

    Text(
        text = stringResource(
            if (isLinkingFromGuest) R.string.auth_brand_subtitle_linking
            else R.string.auth_brand_subtitle
        ),
        fontFamily = PlusJakartaSansFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.5.sp,
        lineHeight = 19.sp,
        color = themeColors.textSecondary,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(horizontal = 12.dp)
    )
}

@Composable
private fun AuthModeSegmentedToggle(
    selectedMode: AuthMode,
    onSelectMode: (AuthMode) -> Unit
) {
    val themeColors = LocalIOS27Theme.current
    val mat = LocalAppleMaterials.current
    val isLight = mat.scheme.isLight

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .inputWellSurface(shape = ContinuousCapsule)
            .padding(4.dp)
    ) {
        val segmentWidth = maxWidth / 2
        val indicatorOffset by animateDpAsState(
            targetValue = if (selectedMode == AuthMode.LOGIN) 0.dp else segmentWidth,
            animationSpec = spring(
                dampingRatio = 0.82f,
                stiffness = 380f
            ),
            label = "authSegmentOffset"
        )

        Box(
            modifier = Modifier
                .offset(x = indicatorOffset)
                .width(segmentWidth)
                .fillMaxHeight()
                .liquidGlassNavSurface(shape = ContinuousCapsule, elevation = 4.dp)
                .background(
                    color = themeColors.accent.copy(alpha = if (isLight) 0.22f else 0.36f),
                    shape = ContinuousCapsule
                )
        )

        Row(modifier = Modifier.fillMaxSize()) {
            AuthMode.entries.forEach { mode ->
                val isSelected = selectedMode == mode
                val textColor by animateColorAsState(
                    targetValue = if (isSelected) themeColors.textPrimary else themeColors.textSecondary,
                    animationSpec = tween(180),
                    label = "segmentTextColor"
                )
                val tabLabel = stringResource(
                    if (mode == AuthMode.LOGIN) R.string.auth_tab_login else R.string.auth_tab_register
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(ContinuousCapsule)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            role = Role.Tab
                        ) {
                            onSelectMode(mode)
                        }
                        .testTag(if (mode == AuthMode.LOGIN) "auth_tab_login" else "auth_tab_register"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tabLabel,
                        fontFamily = PlusJakartaSansFamily,
                        fontSize = 13.5.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                        color = textColor
                    )
                }
            }
        }
    }
}

@Composable
private fun AuthFeedbackBannerCard(banner: AuthFeedbackBanner?) {
    val themeColors = LocalIOS27Theme.current
    AnimatedVisibility(
        visible = banner != null,
        enter = fadeIn(tween(220)) + expandVertically(tween(220)),
        exit = fadeOut(tween(180)) + shrinkVertically(tween(180))
    ) {
        if (banner != null) {
            val bannerColor = when (banner.type) {
                AuthFeedbackType.ERROR -> Color(0xFFF43F5E)
                AuthFeedbackType.SUCCESS -> Color(0xFF10B981)
                AuthFeedbackType.INFO -> themeColors.accent
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(ContinuousRoundedRectangle(16.dp))
                    .background(bannerColor.copy(alpha = 0.15f))
                    .border(
                        width = 1.dp,
                        color = bannerColor.copy(alpha = 0.42f),
                        shape = ContinuousRoundedRectangle(16.dp)
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp)
                    .testTag("auth_feedback_banner"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(bannerColor.copy(alpha = 0.24f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (banner.isError) Icons.Default.Close else Icons.Default.Check,
                        contentDescription = null,
                        tint = bannerColor,
                        modifier = Modifier.size(13.dp)
                    )
                }
                Text(
                    text = banner.message,
                    fontFamily = PlusJakartaSansFamily,
                    fontSize = 12.5.sp,
                    lineHeight = 17.sp,
                    fontWeight = FontWeight.Medium,
                    color = themeColors.textPrimary,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun AuthCredentialsFieldsSection(
    authMode: AuthMode,
    nameInput: String,
    emailInput: String,
    passwordInput: String,
    confirmPasswordInput: String,
    passwordVisible: Boolean,
    confirmPasswordVisible: Boolean,
    onNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onTogglePasswordVisible: () -> Unit,
    onToggleConfirmPasswordVisible: () -> Unit,
    onForgotPassword: () -> Unit,
    onSubmit: () -> Unit
) {
    val themeColors = LocalIOS27Theme.current
    val focusManager = LocalFocusManager.current

    val passwordsMatch = authMode == AuthMode.LOGIN ||
        (confirmPasswordInput.isNotEmpty() && passwordInput == confirmPasswordInput)
    val passwordMismatchVisible = authMode == AuthMode.REGISTER &&
        confirmPasswordInput.isNotEmpty() &&
        passwordInput != confirmPasswordInput

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Campo Nome (visível apenas no modo REGISTER)
        AnimatedVisibility(
            visible = authMode == AuthMode.REGISTER,
            enter = fadeIn(tween(220)) + expandVertically(tween(220)),
            exit = fadeOut(tween(180)) + shrinkVertically(tween(180))
        ) {
            AuthGlassTextField(
                value = nameInput,
                onValueChange = onNameChange,
                label = stringResource(R.string.auth_field_name_label),
                placeholder = stringResource(R.string.auth_field_name_placeholder),
                leadingIcon = LucideIcons.Person,
                accentColor = themeColors.accent,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                ),
                testTag = "auth_name_input"
            )
        }

        // Campo E-mail
        AuthGlassTextField(
            value = emailInput,
            onValueChange = onEmailChange,
            label = stringResource(R.string.auth_field_email_label),
            placeholder = stringResource(R.string.auth_field_email_placeholder),
            leadingIcon = LucideIcons.Mail,
            accentColor = themeColors.accent,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            ),
            testTag = "auth_email_input"
        )

        // Campo Senha
        AuthGlassTextField(
            value = passwordInput,
            onValueChange = onPasswordChange,
            label = stringResource(R.string.auth_field_password_label),
            placeholder = stringResource(R.string.auth_field_password_placeholder),
            leadingIcon = LucideIcons.Lock,
            accentColor = themeColors.accent,
            isPassword = true,
            passwordVisible = passwordVisible,
            onTogglePasswordVisibility = onTogglePasswordVisible,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                keyboardType = KeyboardType.Password,
                imeAction = if (authMode == AuthMode.REGISTER) ImeAction.Next else ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) },
                onDone = { onSubmit() }
            ),
            testTag = "auth_password_input"
        )

        // Campo Confirmar Senha (visível apenas no modo REGISTER)
        AnimatedVisibility(
            visible = authMode == AuthMode.REGISTER,
            enter = fadeIn(tween(220)) + expandVertically(tween(220)),
            exit = fadeOut(tween(180)) + shrinkVertically(tween(180))
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                AuthGlassTextField(
                    value = confirmPasswordInput,
                    onValueChange = onConfirmPasswordChange,
                    label = stringResource(R.string.auth_field_confirm_password_label),
                    placeholder = stringResource(R.string.auth_field_confirm_password_placeholder),
                    leadingIcon = LucideIcons.Lock,
                    accentColor = if (passwordMismatchVisible) Color(0xFFF43F5E) else themeColors.accent,
                    isPassword = true,
                    passwordVisible = confirmPasswordVisible,
                    onTogglePasswordVisibility = onToggleConfirmPasswordVisible,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.None,
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { onSubmit() }
                    ),
                    testTag = "auth_confirm_password_input"
                )

                if (confirmPasswordInput.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(horizontal = 4.dp)
                    ) {
                        Icon(
                            imageVector = if (passwordsMatch) Icons.Default.Check else Icons.Default.Close,
                            contentDescription = null,
                            tint = if (passwordsMatch) Color(0xFF10B981) else Color(0xFFF43F5E),
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = if (passwordsMatch) "Senhas conferem" else "As senhas não coincidem",
                            fontFamily = PlusJakartaSansFamily,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (passwordsMatch) Color(0xFF10B981) else Color(0xFFF43F5E)
                        )
                    }
                }
            }
        }

        // Link "Esqueci minha senha" (com alvo de toque mínimo de 48dp para acessibilidade)
        AnimatedVisibility(visible = authMode == AuthMode.LOGIN) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Box(
                    modifier = Modifier
                        .defaultMinSize(minHeight = 48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(role = Role.Button, onClick = onForgotPassword)
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.auth_forgot_password),
                        fontFamily = PlusJakartaSansFamily,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = themeColors.accent,
                        modifier = Modifier.testTag("auth_forgot_password_button")
                    )
                }
            }
        }
    }
}

@Composable
private fun AuthPrimaryActionsSection(
    authMode: AuthMode,
    isLoading: Boolean,
    isLinkingFromGuest: Boolean,
    backdrop: Backdrop,
    onSubmitPrimary: () -> Unit,
    onLoginWithGoogle: () -> Unit,
    onContinueAsGuest: () -> Unit
) {
    val themeColors = LocalIOS27Theme.current

    // 1. Botão Principal de Ação reutilizando `tintedLiquidBlueButton` oficial do sistema
    val primaryInteraction = remember { MutableInteractionSource() }
    val isPrimaryPressed by primaryInteraction.collectIsPressedAsState()
    val primaryPressProgress by animateFloatAsState(
        targetValue = if (isPrimaryPressed) 0.35f else 0f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 360f),
        label = "primaryAuthPressProgress"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .tintedLiquidBlueButton(
                backdrop = backdrop,
                pressProgress = primaryPressProgress,
                isActive = true
            )
            .clickable(
                interactionSource = primaryInteraction,
                indication = null,
                enabled = !isLoading,
                role = Role.Button,
                onClick = onSubmitPrimary
            )
            .testTag("auth_submit_primary_button"),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.2.dp,
                    color = Color.White
                )
                Text(
                    text = stringResource(R.string.auth_submitting),
                    fontFamily = PlusJakartaSansFamily,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        } else {
            AnimatedContent(
                targetState = authMode,
                transitionSpec = {
                    fadeIn(tween(180)) togetherWith fadeOut(tween(140))
                },
                label = "primaryAuthLabel"
            ) { mode ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(
                            if (mode == AuthMode.LOGIN) R.string.auth_submit_login
                            else R.string.auth_submit_register
                        ),
                        fontFamily = PlusJakartaSansFamily,
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Icon(
                        imageVector = LucideIcons.ArrowRight,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }

    // 2. Divisor Translúcido
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(0.8.dp)
                .background(themeColors.divider)
        )
        Text(
            text = stringResource(R.string.auth_divider_or),
            fontFamily = PlusJakartaSansFamily,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Medium,
            color = themeColors.textTertiary
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(0.8.dp)
                .background(themeColors.divider)
        )
    }

    // 3. Botão "Continuar com o Google" usando `liquidGlassNavSurface` oficial
    val googleInteraction = remember { MutableInteractionSource() }
    val isGooglePressed by googleInteraction.collectIsPressedAsState()
    val googleScale by animateFloatAsState(
        targetValue = if (isGooglePressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 400f),
        label = "googleBtnScale"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .graphicsLayer {
                scaleX = googleScale
                scaleY = googleScale
            }
            .liquidGlassNavSurface(shape = Capsule(), elevation = 8.dp)
            .clickable(
                interactionSource = googleInteraction,
                indication = null,
                enabled = !isLoading,
                role = Role.Button,
                onClick = onLoginWithGoogle
            )
            .testTag("auth_google_button"),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = LucideIcons.GoogleIcon,
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = stringResource(R.string.auth_continue_google),
                fontFamily = PlusJakartaSansFamily,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = themeColors.textPrimary
            )
        }
    }

    // 4. Botão "Explorar como Convidado (Offline)" usando `inputWellSurface`
    val guestInteraction = remember { MutableInteractionSource() }
    val isGuestPressed by guestInteraction.collectIsPressedAsState()
    val guestScale by animateFloatAsState(
        targetValue = if (isGuestPressed) 0.98f else 1f,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = 400f),
        label = "guestBtnScale"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .graphicsLayer {
                scaleX = guestScale
                scaleY = guestScale
            }
            .inputWellSurface(shape = ContinuousRoundedRectangle(20.dp))
            .clickable(
                interactionSource = guestInteraction,
                indication = null,
                enabled = !isLoading,
                role = Role.Button,
                onClick = onContinueAsGuest
            )
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("auth_guest_button"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = LucideIcons.UserCheck,
                contentDescription = null,
                tint = themeColors.accent,
                modifier = Modifier.size(17.dp)
            )
            Text(
                text = stringResource(
                    if (isLinkingFromGuest) R.string.auth_continue_guest_linking
                    else R.string.auth_continue_guest
                ),
                fontFamily = PlusJakartaSansFamily,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = themeColors.textPrimary
            )
        }
        Text(
            text = stringResource(R.string.auth_guest_subtitle),
            fontFamily = PlusJakartaSansFamily,
            fontSize = 11.sp,
            lineHeight = 15.sp,
            color = themeColors.textSecondary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun AuthGlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    leadingIcon: ImageVector,
    accentColor: Color,
    isPassword: Boolean = false,
    passwordVisible: Boolean = false,
    onTogglePasswordVisibility: (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    testTag: String = ""
) {
    val themeColors = LocalIOS27Theme.current
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val borderColor by animateColorAsState(
        targetValue = if (isFocused) accentColor.copy(alpha = 0.88f) else Color.Transparent,
        animationSpec = tween(180),
        label = "authFieldBorder"
    )
    val fieldShape = ContinuousRoundedRectangle(16.dp)
    val showPwdCd = stringResource(R.string.auth_cd_show_password)
    val hidePwdCd = stringResource(R.string.auth_cd_hide_password)

    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(
            text = label,
            fontFamily = PlusJakartaSansFamily,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = themeColors.textSecondary,
            modifier = Modifier.padding(start = 4.dp)
        )

        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            interactionSource = interactionSource,
            textStyle = TextStyle(
                fontFamily = PlusJakartaSansFamily,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Medium,
                color = themeColors.textPrimary
            ),
            cursorBrush = SolidColor(accentColor),
            visualTransformation = if (isPassword && !passwordVisible) {
                PasswordVisualTransformation()
            } else {
                VisualTransformation.None
            },
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            modifier = Modifier
                .fillMaxWidth()
                .let { if (testTag.isNotEmpty()) it.testTag(testTag) else it },
            decorationBox = { innerTextField ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .inputWellSurface(shape = fieldShape)
                        .border(
                            width = if (isFocused) 1.4.dp else 0.dp,
                            color = borderColor,
                            shape = fieldShape
                        )
                        .padding(start = 14.dp, end = if (isPassword) 4.dp else 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = leadingIcon,
                        contentDescription = null,
                        tint = if (isFocused) accentColor else themeColors.textSecondary,
                        modifier = Modifier.size(18.dp)
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (value.isEmpty()) {
                            Text(
                                text = placeholder,
                                fontFamily = PlusJakartaSansFamily,
                                fontSize = 14.sp,
                                color = themeColors.textTertiary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        innerTextField()
                    }

                    if (isPassword && onTogglePasswordVisibility != null) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .clickable(
                                    role = Role.Button,
                                    onClick = onTogglePasswordVisibility
                                )
                                .semantics {
                                    contentDescription = if (passwordVisible) hidePwdCd else showPwdCd
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (passwordVisible) LucideIcons.EyeOff else LucideIcons.Eye,
                                contentDescription = null,
                                tint = themeColors.textSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        )
    }
}
