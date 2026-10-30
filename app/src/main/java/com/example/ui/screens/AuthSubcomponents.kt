package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.selected
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.AuthFeedbackBanner
import com.example.model.AuthFeedbackType
import com.example.model.AuthFormState
import com.example.model.AuthMode
import com.example.ui.components.LucideIcons
import com.example.ui.components.QuadrinheiroLogo
import com.example.ui.components.contentMaterialSurface
import com.example.ui.components.inputWellSurface
import com.example.ui.components.liquidGlass
import com.example.ui.components.liquidGlassNavSurface
import com.example.ui.components.tintedLiquidBlueButton
import com.example.ui.theme.LocalAppleMaterials
import com.example.ui.theme.LocalIOS27Theme
import com.example.ui.theme.PlusJakartaSansFamily
import com.example.ui.theme.SpaceGroteskTabularFamily
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.shapes.Capsule
import com.kyant.shapes.ContinuousCapsule
import com.kyant.shapes.ContinuousRoundedRectangle

/**
 * Tokens visuais centralizados da AuthScreen alinhados ao sistema Liquid Glass do Quadrinheiro.
 */
object AuthGlassDesignTokens {
    val CardCornerRadius: Dp = 24.dp
    val CardElevation: Dp = 8.dp
    val EmblemCornerRadius: Dp = 18.dp
    val EmblemElevation: Dp = 4.dp
    val InputCornerRadius: Dp = 14.dp
    val BannerCornerRadius: Dp = 14.dp
    val GuestCardCornerRadius: Dp = 16.dp

    val ErrorSemanticColor = Color(0xFFF43F5E)
    val SuccessSemanticColor = Color(0xFF10B981)

    fun activeSegmentAlpha(isLight: Boolean): Float = if (isLight) 0.18f else 0.28f
}

/**
 * Building block reutilizável para o container principal em Liquid Glass da autenticação.
 */
fun Modifier.authGlassCardSurface(
    cornerRadius: Dp = AuthGlassDesignTokens.CardCornerRadius,
    elevation: Dp = AuthGlassDesignTokens.CardElevation
): Modifier = composed {
    val shape: Shape = ContinuousRoundedRectangle(cornerRadius)
    val radiusPx = cornerRadius.value
    this
        .liquidGlass(
            radiusX = radiusPx,
            radiusY = radiusPx,
            shape = shape,
            elevation = elevation
        )
        .contentMaterialSurface(shape = shape)
}

/**
 * Building block reutilizável para o emblema óptico superior da marca.
 */
fun Modifier.authEmblemGlassSurface(
    cornerRadius: Dp = AuthGlassDesignTokens.EmblemCornerRadius,
    elevation: Dp = AuthGlassDesignTokens.EmblemElevation
): Modifier = composed {
    val themeColors = LocalIOS27Theme.current
    val shape: Shape = ContinuousRoundedRectangle(cornerRadius)
    val radiusPx = cornerRadius.value
    this
        .liquidGlass(
            radiusX = radiusPx,
            radiusY = radiusPx,
            shape = shape,
            elevation = elevation
        )
        .background(
            brush = Brush.linearGradient(
                colors = listOf(
                    themeColors.accent.copy(alpha = 0.25f),
                    themeColors.accentSecondary.copy(alpha = 0.16f)
                )
            ),
            shape = shape
        )
        .border(
            width = 1.dp,
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.35f),
                    Color.White.copy(alpha = 0.08f)
                )
            ),
            shape = shape
        )
}

/**
 * Fundo atmosférico sutil e estático: gradiente suave com brilhos ambientais e vinheta.
 * Elimina loops de animação contínuos na GPU para máxima fluidez e consistência visual.
 */
@Composable
internal fun AuthBackdropLayer(
    backdrop: LayerBackdrop,
    reduceTransparency: Boolean,
    isLight: Boolean
) {
    val mat = LocalAppleMaterials.current
    val s = mat.scheme
    val themeColors = LocalIOS27Theme.current

    val vignetteColors = remember(themeColors.bgPrimary, isLight) {
        listOf(
            themeColors.bgPrimary.copy(alpha = if (isLight) 0.25f else 0.35f),
            themeColors.bgPrimary.copy(alpha = if (isLight) 0.55f else 0.65f),
            themeColors.bgPrimary.copy(alpha = if (isLight) 0.85f else 0.92f)
        )
    }

    val primaryGlow = remember(themeColors.accent, isLight) {
        listOf(
            themeColors.accent.copy(alpha = if (isLight) 0.14f else 0.20f),
            themeColors.accent.copy(alpha = 0.05f),
            Color.Transparent
        )
    }
    val secondaryGlow = remember(themeColors.accentSecondary, isLight) {
        listOf(
            themeColors.accentSecondary.copy(alpha = if (isLight) 0.12f else 0.16f),
            themeColors.accentSecondary.copy(alpha = 0.04f),
            Color.Transparent
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .layerBackdrop(backdrop)
            .background(themeColors.bgPrimary)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val maxDim = size.maxDimension

            // 1. Gradiente linear base suave
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(s.gradLinearStart, s.gradLinearMid, s.gradLinearEnd),
                    start = Offset.Zero,
                    end = Offset(w, h)
                ),
                alpha = if (themeColors.isAllBlack) 0.08f else if (isLight) 0.65f else 0.45f
            )

            // 2. Brilhos radiais estáticos ambientais
            if (!reduceTransparency && !themeColors.isAllBlack) {
                drawRect(
                    brush = Brush.radialGradient(
                        colors = primaryGlow,
                        center = Offset(w * 0.25f, h * 0.22f),
                        radius = maxDim * 0.55f
                    )
                )
                drawRect(
                    brush = Brush.radialGradient(
                        colors = secondaryGlow,
                        center = Offset(w * 0.75f, h * 0.70f),
                        radius = maxDim * 0.50f
                    )
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(colors = vignetteColors))
        )
    }
}

@Composable
internal fun AuthBrandHeader(isLinkingFromGuest: Boolean) {
    val themeColors = LocalIOS27Theme.current
    val emblemCd = stringResource(R.string.auth_cd_brand_emblem)

    // Logo "solto" na tela de login, sem moldura em Liquid Glass
    Box(
        modifier = Modifier
            .size(50.dp)
            .semantics { contentDescription = emblemCd },
        contentAlignment = Alignment.Center
    ) {
        QuadrinheiroLogo(
            modifier = Modifier.fillMaxSize(0.9f)
        )
    }

    Spacer(modifier = Modifier.height(6.dp))

    Text(
        text = stringResource(R.string.app_name),
        fontFamily = SpaceGroteskTabularFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        letterSpacing = (-0.5).sp,
        color = themeColors.textPrimary,
        textAlign = TextAlign.Center,
        modifier = Modifier.semantics { heading() }
    )

    Spacer(modifier = Modifier.height(2.dp))

    Text(
        text = stringResource(
            if (isLinkingFromGuest) R.string.auth_brand_subtitle_linking
            else R.string.auth_brand_subtitle
        ),
        fontFamily = PlusJakartaSansFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        color = themeColors.textSecondary,
        textAlign = TextAlign.Center,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(horizontal = 8.dp)
    )
}

@Composable
internal fun AuthModeSegmentedToggle(
    selectedMode: AuthMode,
    onSelectMode: (AuthMode) -> Unit
) {
    val themeColors = LocalIOS27Theme.current
    val mat = LocalAppleMaterials.current
    val isLight = mat.scheme.isLight

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .inputWellSurface(shape = ContinuousCapsule)
            .padding(3.dp)
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
                .liquidGlassNavSurface(shape = ContinuousCapsule, elevation = 3.dp)
                .background(
                    color = themeColors.accent.copy(alpha = AuthGlassDesignTokens.activeSegmentAlpha(isLight)),
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
                        .semantics { selected = isSelected }
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
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                        color = textColor
                    )
                }
            }
        }
    }
}

@Composable
internal fun AuthFeedbackBannerCard(banner: AuthFeedbackBanner?) {
    val themeColors = LocalIOS27Theme.current
    val bannerShape = ContinuousRoundedRectangle(AuthGlassDesignTokens.BannerCornerRadius)

    AnimatedVisibility(
        visible = banner != null,
        enter = fadeIn(tween(220)) + expandVertically(tween(220)),
        exit = fadeOut(tween(180)) + shrinkVertically(tween(180))
    ) {
        if (banner != null) {
            val bannerColor = when (banner.type) {
                AuthFeedbackType.ERROR -> AuthGlassDesignTokens.ErrorSemanticColor
                AuthFeedbackType.SUCCESS -> AuthGlassDesignTokens.SuccessSemanticColor
                AuthFeedbackType.INFO -> themeColors.accent
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(bannerShape)
                    .background(bannerColor.copy(alpha = 0.15f))
                    .border(
                        width = 1.dp,
                        color = bannerColor.copy(alpha = 0.42f),
                        shape = bannerShape
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp)
                    .semantics { liveRegion = LiveRegionMode.Polite }
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
internal fun AuthCredentialsFieldsSection(
    form: AuthFormState,
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

    AnimatedContent(
        targetState = form.mode,
        transitionSpec = {
            fadeIn(animationSpec = tween(160)) togetherWith fadeOut(animationSpec = tween(120))
        },
        label = "authFormCrossfade"
    ) { mode ->
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Campo Nome (apenas no modo REGISTER)
            if (mode == AuthMode.REGISTER) {
                AuthGlassTextField(
                    value = form.name,
                    onValueChange = onNameChange,
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
                value = form.email,
                onValueChange = onEmailChange,
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
                value = form.password,
                onValueChange = onPasswordChange,
                placeholder = stringResource(R.string.auth_field_password_placeholder),
                leadingIcon = LucideIcons.Lock,
                accentColor = themeColors.accent,
                isPassword = true,
                passwordVisible = form.isPasswordVisible,
                onTogglePasswordVisibility = onTogglePasswordVisible,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    keyboardType = KeyboardType.Password,
                    imeAction = if (mode == AuthMode.REGISTER) ImeAction.Next else ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) },
                    onDone = { onSubmit() }
                ),
                testTag = "auth_password_input"
            )

            // Campo Confirmar Senha (apenas no modo REGISTER)
            if (mode == AuthMode.REGISTER) {
                val matchIndicator: (@Composable () -> Unit)? = if (form.confirmPassword.isNotEmpty()) {
                    {
                        val indicatorColor = if (form.passwordsMatch) {
                            AuthGlassDesignTokens.SuccessSemanticColor
                        } else {
                            AuthGlassDesignTokens.ErrorSemanticColor
                        }
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(indicatorColor.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (form.passwordsMatch) Icons.Default.Check else Icons.Default.Close,
                                contentDescription = if (form.passwordsMatch) {
                                    stringResource(R.string.auth_passwords_match)
                                } else {
                                    stringResource(R.string.auth_passwords_mismatch)
                                },
                                tint = indicatorColor,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                } else null

                AuthGlassTextField(
                    value = form.confirmPassword,
                    onValueChange = onConfirmPasswordChange,
                    placeholder = stringResource(R.string.auth_field_confirm_password_placeholder),
                    leadingIcon = LucideIcons.Lock,
                    accentColor = if (form.showPasswordMismatchWarning) {
                        AuthGlassDesignTokens.ErrorSemanticColor
                    } else {
                        themeColors.accent
                    },
                    isPassword = true,
                    passwordVisible = form.isConfirmPasswordVisible,
                    onTogglePasswordVisibility = onToggleConfirmPasswordVisible,
                    trailingStatus = matchIndicator,
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
            } else {
                // Link "Esqueci minha senha" compacto à direita no modo LOGIN
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 1.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(role = Role.Button, onClick = onForgotPassword)
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.auth_forgot_password),
                            fontFamily = PlusJakartaSansFamily,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = themeColors.accent,
                            modifier = Modifier.testTag("auth_forgot_password_button")
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun AuthPrimaryActionsSection(
    authMode: AuthMode,
    isLoading: Boolean,
    isLinkingFromGuest: Boolean,
    backdrop: Backdrop,
    onSubmitPrimary: () -> Unit,
    onLoginWithGoogle: () -> Unit,
    onContinueAsGuest: () -> Unit
) {
    val themeColors = LocalIOS27Theme.current

    // 1. Botão Principal de Ação (44.dp de altura)
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
            .height(44.dp)
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
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = Color.White
                )
                Text(
                    text = stringResource(R.string.auth_submitting),
                    fontFamily = PlusJakartaSansFamily,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        } else {
            AnimatedContent(
                targetState = authMode,
                transitionSpec = {
                    fadeIn(tween(160)) togetherWith fadeOut(tween(120))
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
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Icon(
                        imageVector = LucideIcons.ArrowRight,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }

    // 2. Divisor Translúcido
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp),
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

    // 3. Botões Circulares de Autenticação Rápida (Google & Convidado lado a lado)
    val googleInteraction = remember { MutableInteractionSource() }
    val isGooglePressed by googleInteraction.collectIsPressedAsState()
    val googleScale by animateFloatAsState(
        targetValue = if (isGooglePressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 400f),
        label = "googleBtnScale"
    )

    val guestInteraction = remember { MutableInteractionSource() }
    val isGuestPressed by guestInteraction.collectIsPressedAsState()
    val guestScale by animateFloatAsState(
        targetValue = if (isGuestPressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 400f),
        label = "guestBtnScale"
    )

    val continueGoogleCd = stringResource(R.string.auth_continue_google)
    val continueGuestCd = stringResource(
        if (isLinkingFromGuest) R.string.auth_continue_guest_linking
        else R.string.auth_continue_guest
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Círculo 1: Login com Google (apenas o 'G' da logo sem fundo branco, sobre a superfície translúcida)
        Box(
            modifier = Modifier
                .size(46.dp)
                .graphicsLayer {
                    scaleX = googleScale
                    scaleY = googleScale
                }
                .liquidGlassNavSurface(shape = CircleShape, elevation = 4.dp)
                .clickable(
                    interactionSource = googleInteraction,
                    indication = null,
                    enabled = !isLoading,
                    role = Role.Button,
                    onClick = onLoginWithGoogle
                )
                .semantics { contentDescription = continueGoogleCd }
                .testTag("auth_google_button"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = LucideIcons.GoogleIcon,
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(18.dp))

        // Círculo 2: Modo Convidado
        Box(
            modifier = Modifier
                .size(46.dp)
                .graphicsLayer {
                    scaleX = guestScale
                    scaleY = guestScale
                }
                .liquidGlassNavSurface(shape = CircleShape, elevation = 4.dp)
                .clickable(
                    interactionSource = guestInteraction,
                    indication = null,
                    enabled = !isLoading,
                    role = Role.Button,
                    onClick = onContinueAsGuest
                )
                .semantics { contentDescription = continueGuestCd }
                .testTag("auth_guest_button"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = LucideIcons.UserCheck,
                contentDescription = null,
                tint = themeColors.accent,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
internal fun AuthGlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: ImageVector,
    accentColor: Color,
    isPassword: Boolean = false,
    passwordVisible: Boolean = false,
    onTogglePasswordVisibility: (() -> Unit)? = null,
    trailingStatus: (@Composable () -> Unit)? = null,
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
    val fieldShape = ContinuousRoundedRectangle(AuthGlassDesignTokens.InputCornerRadius)
    val showPwdCd = stringResource(R.string.auth_cd_show_password)
    val hidePwdCd = stringResource(R.string.auth_cd_hide_password)

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        interactionSource = interactionSource,
        textStyle = TextStyle(
            fontFamily = PlusJakartaSansFamily,
            fontSize = 13.5.sp,
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
                    .height(44.dp)
                    .inputWellSurface(shape = fieldShape)
                    .border(
                        width = if (isFocused) 1.2.dp else 0.dp,
                        color = borderColor,
                        shape = fieldShape
                    )
                    .padding(start = 12.dp, end = if (isPassword || trailingStatus != null) 4.dp else 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = if (isFocused) accentColor else themeColors.textSecondary,
                    modifier = Modifier.size(17.dp)
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            fontFamily = PlusJakartaSansFamily,
                            fontSize = 13.sp,
                            color = themeColors.textTertiary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    innerTextField()
                }

                if (trailingStatus != null) {
                    trailingStatus()
                }

                if (isPassword && onTogglePasswordVisibility != null) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
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
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }
        }
    )
}
