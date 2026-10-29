package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
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
import com.example.data.CuratedCatalog
import com.example.model.AuthMode
import com.example.ui.components.LucideIcons
import com.example.ui.theme.LocalAppleMaterials
import com.example.ui.theme.LocalIOS27Theme
import com.example.ui.theme.PlusJakartaSansFamily
import com.example.ui.theme.SpaceGroteskTabularFamily
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
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
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current

    var authMode by rememberSaveable { mutableStateOf(initialMode) }
    var nameInput by rememberSaveable { mutableStateOf("") }
    var emailInput by rememberSaveable { mutableStateOf("") }
    var passwordInput by rememberSaveable { mutableStateOf("") }
    var confirmPasswordInput by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var confirmPasswordVisible by rememberSaveable { mutableStateOf(false) }
    var localValidationError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(initialMode) {
        authMode = initialMode
    }

    // Limpa erro local ao digitar ou trocar de aba
    LaunchedEffect(authMode, nameInput, emailInput, passwordInput, confirmPasswordInput) {
        if (localValidationError != null) {
            localValidationError = null
        }
    }

    val authBackdrop = rememberLayerBackdrop()

    // Animação contínua dos orbes luminosos (lida apenas na fase de draw para zero recomposição)
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

    val showcaseCovers = remember {
        CuratedCatalog.curatedComicsCatalog
            .filter { it.coverUrl.isNotBlank() }
            .take(6)
    }

    val passwordsMatch = authMode == AuthMode.LOGIN ||
        (confirmPasswordInput.isNotEmpty() && passwordInput == confirmPasswordInput)
    val passwordMismatchVisible = authMode == AuthMode.REGISTER &&
        confirmPasswordInput.isNotEmpty() &&
        passwordInput != confirmPasswordInput

    fun submitPrimaryAction() {
        focusManager.clearFocus()
        onClearFeedback()
        val cleanEmail = emailInput.trim()
        val cleanName = nameInput.trim()

        if (authMode == AuthMode.REGISTER && cleanName.length < 2) {
            localValidationError = "Informe seu nome ou apelido (mínimo de 2 caracteres)."
            return
        }
        if (cleanEmail.isBlank() || !cleanEmail.contains("@") || !cleanEmail.contains(".")) {
            localValidationError = "Digite um endereço de e-mail válido."
            return
        }
        if (passwordInput.length < 6) {
            localValidationError = "A senha deve ter pelo menos 6 caracteres."
            return
        }
        if (authMode == AuthMode.REGISTER && passwordInput != confirmPasswordInput) {
            localValidationError = "As senhas digitadas não coincidem. Verifique a confirmação."
            return
        }

        localValidationError = null
        if (authMode == AuthMode.LOGIN) {
            onLoginWithEmail(cleanEmail, passwordInput)
        } else {
            onRegisterWithEmail(cleanName, cleanEmail, passwordInput)
        }
    }

    val activeBannerMessage = localValidationError ?: feedbackMessage
    val isActiveBannerError = if (localValidationError != null) true else isFeedbackError

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF07090F))
            .testTag("auth_screen_root")
    ) {
        // 1. CAMADA DE FUNDO IMERSIVA GRAVADA NO authBackdrop (Orbes + Silhuetas de Capas de HQs)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(authBackdrop)
                .background(Color(0xFF07090F))
        ) {
            // Silhuetas suaves de capas clássicas no fundo para refração cromática rica sob o Liquid Glass
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 28.dp)
                    .alpha(0.22f)
                    .blur(26.dp),
                horizontalArrangement = Arrangement.spacedBy(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                showcaseCovers.take(3).forEachIndexed { idx, comic ->
                    val req = remember(comic.coverUrl, context) {
                        ImageRequest.Builder(context)
                            .data(comic.coverUrl)
                            .size(140, 210)
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

            // Orbes luminosos animados (Azul Elétrico, Roxo Imperial e Ciano Óptico)
            // Leitura diferida de estado (() -> Float) diretamente no DrawScope para evitar recomposições
            AuthAnimatedOrbsCanvas(
                phaseProvider = { orbPhaseState.value },
                pulseProvider = { pulseState.value },
                primaryAccent = themeColors.accent,
                secondaryAccent = themeColors.accentSecondary
            )

            // Vinheta escultural para profundidade e contraste impecável
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF07090F).copy(alpha = 0.36f),
                                Color(0xFF07090F).copy(alpha = 0.58f),
                                Color(0xFF07090F).copy(alpha = 0.88f)
                            )
                        )
                    )
            )
        }

        // 2. CONTEÚDO PRINCIPAL EM LIQUID GLASS (Irmão do authBackdrop para refração real sem recursão)
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
                // Botão fechar opcional caso aberto a partir da tela de Ajustes para vincular conta
                if (onDismissModal != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(38.dp)
                            .drawBackdrop(
                                backdrop = authBackdrop,
                                shape = { CircleShape },
                                effects = {
                                    vibrancy()
                                    blur(6f.dp.toPx())
                                    lens(8f.dp.toPx(), 14f.dp.toPx())
                                },
                                highlight = { Highlight.Default.copy(alpha = 0.45f) },
                                onDrawSurface = {
                                    drawRect(Color.White.copy(alpha = 0.12f))
                                }
                            )
                            .clickable { onDismissModal() }
                            .testTag("auth_close_modal_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Fechar",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(6.dp))

                    // CABEÇALHO DE BOAS-VINDAS (BRAND HEADER)
                    AuthLiquidBrandEmblem(
                        backdrop = authBackdrop,
                        accentColor = themeColors.accent
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Quadrinheiro",
                        fontFamily = SpaceGroteskTabularFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 32.sp,
                        letterSpacing = (-0.8).sp,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = if (isLinkingFromGuest) {
                            "Vincule sua conta para sincronizar todo o seu acervo local na nuvem."
                        } else {
                            "Seu catálogo, diário cronológico e guia de leitura definitivo."
                        },
                        fontFamily = PlusJakartaSansFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 13.5.sp,
                        lineHeight = 19.sp,
                        color = Color.White.copy(alpha = 0.76f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )

                    Spacer(modifier = Modifier.height(22.dp))

                    // CARD CENTRAL LIQUID GLASS (FORMULÁRIO PRINCIPAL)
                    val cardShape = ContinuousRoundedRectangle(30.dp)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .drawBackdrop(
                                backdrop = authBackdrop,
                                shape = { cardShape },
                                effects = {
                                    vibrancy()
                                    blur(14f.dp.toPx())
                                    lens(
                                        refractionHeight = 14f.dp.toPx(),
                                        refractionAmount = 22f.dp.toPx(),
                                        depthEffect = true,
                                        chromaticAberration = true
                                    )
                                },
                                highlight = {
                                    Highlight.Ambient.copy(
                                        width = 1.1.dp,
                                        alpha = 0.55f
                                    )
                                },
                                shadow = {
                                    Shadow(
                                        radius = 28.dp,
                                        color = Color.Black.copy(alpha = 0.45f)
                                    )
                                },
                                innerShadow = {
                                    InnerShadow(
                                        radius = 3.dp,
                                        alpha = 0.28f
                                    )
                                },
                                onDrawSurface = {
                                    drawRect(Color(0xFF101522).copy(alpha = 0.52f))
                                    drawRect(
                                        brush = Brush.verticalGradient(
                                            colors = listOf(
                                                Color.White.copy(alpha = 0.14f),
                                                Color.White.copy(alpha = 0.04f),
                                                Color.Transparent
                                            )
                                        )
                                    )
                                }
                            )
                            .padding(horizontal = 20.dp, vertical = 22.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // 1. Segmented Pill Toggle Animado ("Entrar" vs "Criar Conta")
                        AuthSegmentedPillToggle(
                            selectedMode = authMode,
                            accentColor = themeColors.accent,
                            backdrop = authBackdrop,
                            onSelectMode = { nextMode ->
                                authMode = nextMode
                                onClearFeedback()
                            }
                        )

                        // Banner de Feedback (Erro ou Confirmação) em PT-BR
                        AnimatedVisibility(
                            visible = !activeBannerMessage.isNullOrBlank(),
                            enter = fadeIn(tween(220)) + expandVertically(tween(220)),
                            exit = fadeOut(tween(180)) + shrinkVertically(tween(180))
                        ) {
                            val msg = activeBannerMessage.orEmpty()
                            val bannerColor = if (isActiveBannerError) Color(0xFFF43F5E) else Color(0xFF10B981)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(ContinuousRoundedRectangle(16.dp))
                                    .background(bannerColor.copy(alpha = 0.16f))
                                    .border(
                                        width = 1.dp,
                                        color = bannerColor.copy(alpha = 0.42f),
                                        shape = ContinuousRoundedRectangle(16.dp)
                                    )
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(bannerColor.copy(alpha = 0.25f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isActiveBannerError) Icons.Default.Close else Icons.Default.Check,
                                        contentDescription = null,
                                        tint = if (isActiveBannerError) Color(0xFFFB7185) else Color(0xFF34D399),
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                                Text(
                                    text = msg,
                                    fontFamily = PlusJakartaSansFamily,
                                    fontSize = 12.5.sp,
                                    lineHeight = 17.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White.copy(alpha = 0.94f),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        // 2. Campo Nome (visível apenas no modo REGISTER)
                        AnimatedVisibility(
                            visible = authMode == AuthMode.REGISTER,
                            enter = fadeIn(tween(220)) + expandVertically(tween(220)),
                            exit = fadeOut(tween(180)) + shrinkVertically(tween(180))
                        ) {
                            AuthGlassTextField(
                                value = nameInput,
                                onValueChange = { nameInput = it },
                                label = "Seu Nome ou Apelido",
                                placeholder = "Ex: Felipe Colecionador",
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

                        // 3. Campo E-mail
                        AuthGlassTextField(
                            value = emailInput,
                            onValueChange = { emailInput = it },
                            label = "E-mail",
                            placeholder = "seu@email.com",
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

                        // 4. Campo Senha
                        AuthGlassTextField(
                            value = passwordInput,
                            onValueChange = { passwordInput = it },
                            label = "Senha",
                            placeholder = "Mínimo de 6 caracteres",
                            leadingIcon = LucideIcons.Lock,
                            accentColor = themeColors.accent,
                            isPassword = true,
                            passwordVisible = passwordVisible,
                            onTogglePasswordVisibility = { passwordVisible = !passwordVisible },
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.None,
                                keyboardType = KeyboardType.Password,
                                imeAction = if (authMode == AuthMode.REGISTER) ImeAction.Next else ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) },
                                onDone = { submitPrimaryAction() }
                            ),
                            testTag = "auth_password_input"
                        )

                        // 5. Campo Confirmar Senha (visível apenas no modo REGISTER)
                        AnimatedVisibility(
                            visible = authMode == AuthMode.REGISTER,
                            enter = fadeIn(tween(220)) + expandVertically(tween(220)),
                            exit = fadeOut(tween(180)) + shrinkVertically(tween(180))
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                AuthGlassTextField(
                                    value = confirmPasswordInput,
                                    onValueChange = { confirmPasswordInput = it },
                                    label = "Confirmar Senha",
                                    placeholder = "Repita sua senha",
                                    leadingIcon = LucideIcons.Lock,
                                    accentColor = if (passwordMismatchVisible) Color(0xFFF43F5E) else themeColors.accent,
                                    isPassword = true,
                                    passwordVisible = confirmPasswordVisible,
                                    onTogglePasswordVisibility = { confirmPasswordVisible = !confirmPasswordVisible },
                                    keyboardOptions = KeyboardOptions(
                                        capitalization = KeyboardCapitalization.None,
                                        keyboardType = KeyboardType.Password,
                                        imeAction = ImeAction.Done
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onDone = { submitPrimaryAction() }
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
                                            tint = if (passwordsMatch) Color(0xFF34D399) else Color(0xFFFB7185),
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Text(
                                            text = if (passwordsMatch) "Senhas conferem" else "As senhas não coincidem",
                                            fontFamily = PlusJakartaSansFamily,
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = if (passwordsMatch) Color(0xFF34D399) else Color(0xFFFB7185)
                                        )
                                    }
                                }
                            }
                        }

                        // Link "Esqueci minha senha" (visível no modo LOGIN)
                        AnimatedVisibility(visible = authMode == AuthMode.LOGIN) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Text(
                                    text = "Esqueci minha senha",
                                    fontFamily = PlusJakartaSansFamily,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF60A5FA),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            focusManager.clearFocus()
                                            onResetPassword(emailInput.trim())
                                        }
                                        .padding(horizontal = 6.dp, vertical = 4.dp)
                                        .testTag("auth_forgot_password_button")
                                )
                            }
                        }

                        // 6. Botão Principal de Ação (Liquid Button Refrativo)
                        val primaryInteraction = remember { MutableInteractionSource() }
                        val isPrimaryPressed by primaryInteraction.collectIsPressedAsState()
                        val primaryScale by animateFloatAsState(
                            targetValue = if (isPrimaryPressed) 0.97f else 1f,
                            animationSpec = spring(dampingRatio = 0.75f, stiffness = 400f),
                            label = "primaryAuthBtnScale"
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .graphicsLayer {
                                    scaleX = primaryScale
                                    scaleY = primaryScale
                                }
                                .drawBackdrop(
                                    backdrop = authBackdrop,
                                    shape = { ContinuousCapsule },
                                    effects = {
                                        vibrancy()
                                        blur(8f.dp.toPx())
                                        lens(
                                            refractionHeight = 12f.dp.toPx(),
                                            refractionAmount = 20f.dp.toPx(),
                                            depthEffect = true,
                                            chromaticAberration = true
                                        )
                                    },
                                    highlight = {
                                        Highlight.Ambient.copy(
                                            width = 1.2.dp,
                                            alpha = 0.65f
                                        )
                                    },
                                    shadow = {
                                        Shadow(
                                            radius = 16.dp,
                                            color = themeColors.accent.copy(alpha = 0.42f)
                                        )
                                    },
                                    onDrawSurface = {
                                        drawRect(
                                            brush = Brush.horizontalGradient(
                                                colors = listOf(
                                                    Color(0xFF2563EB).copy(alpha = 0.88f),
                                                    themeColors.accent.copy(alpha = 0.90f),
                                                    Color(0xFF06B6D4).copy(alpha = 0.86f)
                                                )
                                            )
                                        )
                                        drawRect(
                                            brush = Brush.verticalGradient(
                                                colors = listOf(
                                                    Color.White.copy(alpha = 0.26f),
                                                    Color.Transparent
                                                )
                                            )
                                        )
                                    }
                                )
                                .clickable(
                                    interactionSource = primaryInteraction,
                                    indication = null,
                                    enabled = !isLoading
                                ) {
                                    submitPrimaryAction()
                                }
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
                                        text = "Autenticando...",
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
                                            text = if (mode == AuthMode.LOGIN) "Entrar na Coleção" else "Criar Minha Conta",
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

                        // 7. Divisor Translúcido ("ou acesse rapidamente com")
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
                                    .background(Color.White.copy(alpha = 0.14f))
                            )
                            Text(
                                text = "ou acesse rapidamente com",
                                fontFamily = PlusJakartaSansFamily,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White.copy(alpha = 0.56f)
                            )
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(0.8.dp)
                                    .background(Color.White.copy(alpha = 0.14f))
                            )
                        }

                        // 8. Botão "Continuar com o Google" (Cápsula Liquid Glass)
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
                                .drawBackdrop(
                                    backdrop = authBackdrop,
                                    shape = { ContinuousCapsule },
                                    effects = {
                                        vibrancy()
                                        blur(10f.dp.toPx())
                                        lens(
                                            refractionHeight = 10f.dp.toPx(),
                                            refractionAmount = 16f.dp.toPx(),
                                            depthEffect = true
                                        )
                                    },
                                    highlight = {
                                        Highlight.Ambient.copy(
                                            width = 1.dp,
                                            alpha = 0.52f
                                        )
                                    },
                                    onDrawSurface = {
                                        drawRect(Color.White.copy(alpha = 0.13f))
                                    }
                                )
                                .clickable(
                                    interactionSource = googleInteraction,
                                    indication = null,
                                    enabled = !isLoading
                                ) {
                                    focusManager.clearFocus()
                                    onClearFeedback()
                                    onLoginWithGoogle()
                                }
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
                                    text = "Continuar com o Google",
                                    fontFamily = PlusJakartaSansFamily,
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }
                        }

                        // 9. Botão "Explorar como Convidado (Offline)"
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
                                .graphicsLayer {
                                    scaleX = guestScale
                                    scaleY = guestScale
                                }
                                .clip(ContinuousRoundedRectangle(20.dp))
                                .background(Color.White.copy(alpha = 0.06f))
                                .border(
                                    width = 0.9.dp,
                                    brush = Brush.linearGradient(
                                        colors = listOf(
                                            Color.White.copy(alpha = 0.24f),
                                            Color.White.copy(alpha = 0.07f)
                                        )
                                    ),
                                    shape = ContinuousRoundedRectangle(20.dp)
                                )
                                .clickable(
                                    interactionSource = guestInteraction,
                                    indication = null,
                                    enabled = !isLoading
                                ) {
                                    focusManager.clearFocus()
                                    onClearFeedback()
                                    if (onDismissModal != null) {
                                        onDismissModal()
                                    } else {
                                        onContinueAsGuest()
                                    }
                                }
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
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(17.dp)
                                )
                                Text(
                                    text = if (isLinkingFromGuest) {
                                        "Continuar no Modo Convidado por enquanto"
                                    } else {
                                        "Explorar como Convidado (Offline)"
                                    },
                                    fontFamily = PlusJakartaSansFamily,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White.copy(alpha = 0.92f)
                                )
                            }
                            Text(
                                text = "Seus quadrinhos ficam salvos no aparelho e você pode vincular uma conta quando quiser.",
                                fontFamily = PlusJakartaSansFamily,
                                fontSize = 11.sp,
                                lineHeight = 15.sp,
                                color = Color.White.copy(alpha = 0.58f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                }
            }
        }
    }
}

@Composable
private fun AuthAnimatedOrbsCanvas(
    phaseProvider: () -> Float,
    pulseProvider: () -> Float,
    primaryAccent: Color,
    secondaryAccent: Color
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val phase = phaseProvider()
        val pulse = pulseProvider()
        val w = size.width
        val h = size.height
        val maxDim = size.maxDimension

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
                    primaryAccent.copy(alpha = 0.34f),
                    Color(0xFF2563EB).copy(alpha = 0.14f),
                    Color.Transparent
                ),
                center = orb1Center,
                radius = maxDim * 0.62f * pulse
            )
        )

        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF8B5CF6).copy(alpha = 0.30f),
                    secondaryAccent.copy(alpha = 0.12f),
                    Color.Transparent
                ),
                center = orb2Center,
                radius = maxDim * 0.58f * (2f - pulse * 0.85f)
            )
        )

        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF06B6D4).copy(alpha = 0.28f),
                    Color(0xFF10B981).copy(alpha = 0.08f),
                    Color.Transparent
                ),
                center = orb3Center,
                radius = maxDim * 0.55f * pulse
            )
        )
    }
}

@Composable
private fun AuthLiquidBrandEmblem(
    backdrop: Backdrop,
    accentColor: Color
) {
    val emblemShape = ContinuousRoundedRectangle(26.dp)
    Box(
        modifier = Modifier
            .size(78.dp)
            .drawBackdrop(
                backdrop = backdrop,
                shape = { emblemShape },
                effects = {
                    vibrancy()
                    blur(10f.dp.toPx())
                    lens(
                        refractionHeight = 14f.dp.toPx(),
                        refractionAmount = 24f.dp.toPx(),
                        depthEffect = true,
                        chromaticAberration = true
                    )
                },
                highlight = {
                    Highlight.Ambient.copy(
                        width = 1.4.dp,
                        alpha = 0.72f
                    )
                },
                shadow = {
                    Shadow(
                        radius = 22.dp,
                        color = accentColor.copy(alpha = 0.45f)
                    )
                },
                onDrawSurface = {
                    drawRect(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                accentColor.copy(alpha = 0.42f),
                                Color(0xFF8B5CF6).copy(alpha = 0.28f),
                                Color(0xFF06B6D4).copy(alpha = 0.34f)
                            )
                        )
                    )
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = LucideIcons.OpenBookFilled,
            contentDescription = "Emblema Quadrinheiro",
            tint = Color.White,
            modifier = Modifier.size(34.dp)
        )
    }
}

@Composable
private fun AuthSegmentedPillToggle(
    selectedMode: AuthMode,
    accentColor: Color,
    backdrop: Backdrop,
    onSelectMode: (AuthMode) -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
            .clip(ContinuousCapsule)
            .background(Color.Black.copy(alpha = 0.34f))
            .border(
                width = 0.8.dp,
                color = Color.White.copy(alpha = 0.14f),
                shape = ContinuousCapsule
            )
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

        // Cápsula ativa deslizante com refração
        Box(
            modifier = Modifier
                .offset(x = indicatorOffset)
                .width(segmentWidth)
                .fillMaxHeight()
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { ContinuousCapsule },
                    effects = {
                        vibrancy()
                        blur(6f.dp.toPx())
                        lens(8f.dp.toPx(), 14f.dp.toPx())
                    },
                    highlight = {
                        Highlight.Ambient.copy(width = 1.dp, alpha = 0.55f)
                    },
                    onDrawSurface = {
                        drawRect(accentColor.copy(alpha = 0.42f))
                        drawRect(Color.White.copy(alpha = 0.12f))
                    }
                )
        )

        Row(modifier = Modifier.fillMaxSize()) {
            AuthMode.entries.forEach { mode ->
                val isSelected = selectedMode == mode
                val textColor by animateColorAsState(
                    targetValue = if (isSelected) Color.White else Color.White.copy(alpha = 0.62f),
                    animationSpec = tween(180),
                    label = "segmentTextColor"
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(ContinuousCapsule)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            onSelectMode(mode)
                        }
                        .testTag(if (mode == AuthMode.LOGIN) "auth_tab_login" else "auth_tab_register"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (mode == AuthMode.LOGIN) "Entrar" else "Criar Conta",
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
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val borderColor by animateColorAsState(
        targetValue = if (isFocused) accentColor.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.16f),
        animationSpec = tween(180),
        label = "authFieldBorder"
    )
    val containerBg by animateColorAsState(
        targetValue = if (isFocused) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.28f),
        animationSpec = tween(180),
        label = "authFieldBg"
    )

    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(
            text = label,
            fontFamily = PlusJakartaSansFamily,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White.copy(alpha = 0.82f),
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
                color = Color.White
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
                        .height(48.dp)
                        .clip(ContinuousRoundedRectangle(16.dp))
                        .background(containerBg)
                        .border(
                            width = if (isFocused) 1.3.dp else 0.9.dp,
                            color = borderColor,
                            shape = ContinuousRoundedRectangle(16.dp)
                        )
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = leadingIcon,
                        contentDescription = null,
                        tint = if (isFocused) accentColor else Color.White.copy(alpha = 0.62f),
                        modifier = Modifier.size(18.dp)
                    )

                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (value.isEmpty()) {
                            Text(
                                text = placeholder,
                                fontFamily = PlusJakartaSansFamily,
                                fontSize = 14.sp,
                                color = Color.White.copy(alpha = 0.38f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        innerTextField()
                    }

                    if (isPassword && onTogglePasswordVisibility != null) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .clickable { onTogglePasswordVisibility() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (passwordVisible) LucideIcons.EyeOff else LucideIcons.Eye,
                                contentDescription = if (passwordVisible) "Ocultar senha" else "Mostrar senha",
                                tint = Color.White.copy(alpha = 0.68f),
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
                }
            }
        )
    }
}
