package com.example.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AuthFeedbackBanner
import com.example.model.AuthFormState
import com.example.model.AuthMode
import com.example.ui.components.LiquidGlassPillButton
import com.example.ui.components.LucideIcons
import com.example.ui.components.QuadrinheiroBrandText
import com.example.ui.components.QuadrinheiroLogo
import com.example.ui.components.QuadrinheiroLogoVariant
import com.example.ui.components.inputWellSurface
import com.example.ui.components.liquidGlass
import com.example.ui.components.rememberAdaptiveLiquidGlassInk
import com.example.ui.theme.LocalAppMotion
import com.example.ui.theme.LocalAppSettings
import com.example.ui.theme.LocalIOS27Theme
import com.example.ui.theme.PlusJakartaSansFamily
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.shapes.ContinuousCapsule
import com.kyant.shapes.ContinuousRoundedRectangle

@Suppress("UNUSED_PARAMETER")
@Composable
fun AuthBackdropLayer(backdrop: Backdrop, reduceTransparency: Boolean, isLight: Boolean) {
    // GLASS_AUTH_STYLE_V2: static theme gradient recorded for the glass surfaces.
    val theme = LocalIOS27Theme.current
    val layerModifier = if (backdrop is LayerBackdrop) Modifier.layerBackdrop(backdrop) else Modifier
    Box(layerModifier.fillMaxSize().background(
        Brush.verticalGradient(listOf(
            theme.bgPrimary,
            androidx.compose.ui.graphics.lerp(theme.bgPrimary, theme.accent, if (reduceTransparency) 0.03f else 0.14f),
            theme.bgPrimary
        ))
    ))
}

fun Modifier.authGlassCardSurface(): Modifier = composed {
    val theme = LocalIOS27Theme.current
    val settings = LocalAppSettings.current
    val shape = remember { ContinuousRoundedRectangle(28.dp) }
    if (settings.reduceTransparency) {
        this
            .clip(shape)
            .background(theme.bgSecondary, shape)
            .border(1.dp, theme.divider, shape)
    } else {
        this.liquidGlass(shape = shape, elevation = 0.dp)
    }
}

@Composable
fun AuthBrandHeader(isLinkingFromGuest: Boolean) {
    val theme = LocalIOS27Theme.current
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        QuadrinheiroLogo(variant = QuadrinheiroLogoVariant.Primary, titleVisible = false, modifier = Modifier.size(76.dp))
        QuadrinheiroBrandText(text = "Quadrinheiro", fontSize = 28.sp, color = theme.textPrimary)
        Text(
            if (isLinkingFromGuest) "Conecte sua conta" else "Seu universo de quadrinhos, organizado.",
            style = MaterialTheme.typography.bodySmall,
            color = theme.textSecondary
        )
    }
}

@Composable
fun AuthModeSegmentedToggle(selectedMode: AuthMode, onSelectMode: (AuthMode) -> Unit) {
    // AUTH_CIRCULAR_ACTIONS_V3: use the app's existing glass segmented picker.
    com.example.ui.components.AppleSegmentedPicker(
        options = AuthMode.values().map {
            com.example.ui.components.SegmentOption(it, if (it == AuthMode.LOGIN) "Entrar" else "Criar conta")
        },
        value = selectedMode,
        onChange = onSelectMode,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
fun AuthFeedbackBannerCard(banner: AuthFeedbackBanner?) {
    if (banner == null) return
    val theme = LocalIOS27Theme.current
    val shape = remember { ContinuousRoundedRectangle(14.dp) }
    val accentColor = if (banner.isError) MaterialTheme.colorScheme.error else theme.accent
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(accentColor.copy(alpha = if (theme.isDark) 0.16f else 0.10f), shape)
            .border(0.75.dp, accentColor.copy(alpha = 0.34f), shape)
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .semantics { liveRegion = LiveRegionMode.Polite }
    ) {
        Text(
            text = banner.message,
            color = if (banner.isError) MaterialTheme.colorScheme.error else theme.textPrimary,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun AuthGlassInputField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: ImageVector? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    trailingContent: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val theme = LocalIOS27Theme.current
    val settings = LocalAppSettings.current
    var focused by remember { mutableStateOf(false) }
    val shape = remember { ContinuousRoundedRectangle(16.dp) }

    val surfaceModifier = if (settings.reduceTransparency) {
        Modifier
            .clip(shape)
            .background(theme.bgPrimary, shape)
            .border(1.dp, if (focused) theme.accent else theme.divider, shape)
    } else {
        Modifier
            .inputWellSurface(shape)
            .then(
                if (focused) Modifier.border(1.5.dp, theme.accent.copy(alpha = 0.72f), shape)
                else Modifier
            )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .then(surfaceModifier)
            .padding(start = 16.dp, end = if (trailingContent != null) 4.dp else 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = if (focused) theme.accent else theme.textSecondary,
                modifier = Modifier.size(18.dp)
            )
        }
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterStart
        ) {
            if (value.isEmpty()) {
                Text(
                    text = placeholder,
                    color = theme.textSecondary,
                    fontSize = 14.sp
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                visualTransformation = visualTransformation,
                keyboardOptions = keyboardOptions,
                keyboardActions = keyboardActions,
                textStyle = TextStyle(
                    fontFamily = PlusJakartaSansFamily,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = theme.textPrimary
                ),
                cursorBrush = SolidColor(theme.accent),
                modifier = modifier
                    .fillMaxWidth()
                    .padding(vertical = 14.dp)
                    .onFocusChanged { focused = it.isFocused }
            )
        }
        trailingContent?.invoke()
    }
}

@Composable
fun AuthCredentialsFieldsSection(
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
    val motion by rememberUpdatedState(LocalAppMotion.current)
    val theme = LocalIOS27Theme.current
    val focus = LocalFocusManager.current
    val registering = form.mode != AuthMode.LOGIN
    val next = KeyboardActions(onNext = { focus.moveFocus(FocusDirection.Down) })
    // AUTH_UNIFIED_HEIGHT_V5: a single size animation, not two competing field springs.
    Column(
        modifier = Modifier.fillMaxWidth().animateContentSize(
            animationSpec = motion.tween(220, easing = com.example.ui.theme.AppMotion.EaseOutStrong)
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (registering) {
            AuthGlassInputField(
                value = form.name,
                onValueChange = onNameChange,
                placeholder = "Nome",
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = next,
                modifier = Modifier.testTag("auth_name_field")
            )
        }
        AuthGlassInputField(
            value = form.email,
            onValueChange = onEmailChange,
            placeholder = "E-mail",
            leadingIcon = LucideIcons.Mail,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            keyboardActions = next,
            modifier = Modifier.testTag("auth_email_field")
        )
        AuthGlassInputField(
            value = form.password,
            onValueChange = onPasswordChange,
            placeholder = "Senha",
            leadingIcon = LucideIcons.Lock,
            visualTransformation = if (form.isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingContent = {
                IconButton(
                    onClick = onTogglePasswordVisible,
                    modifier = Modifier.size(48.dp).testTag("auth_toggle_password_visibility")
                ) {
                    Icon(
                        imageVector = if (form.isPasswordVisible) LucideIcons.EyeOff else LucideIcons.Eye,
                        contentDescription = if (form.isPasswordVisible) "Ocultar senha" else "Mostrar senha",
                        tint = theme.textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = if (registering) ImeAction.Next else ImeAction.Done),
            keyboardActions = KeyboardActions(onNext = { focus.moveFocus(FocusDirection.Down) }, onDone = { onSubmit() }),
            modifier = Modifier.testTag("auth_password_field")
        )
        if (registering) {
            AuthGlassInputField(
                value = form.confirmPassword,
                onValueChange = onConfirmPasswordChange,
                placeholder = "Confirmar senha",
                leadingIcon = LucideIcons.Lock,
                visualTransformation = if (form.isConfirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingContent = {
                    IconButton(
                        onClick = onToggleConfirmPasswordVisible,
                        modifier = Modifier.size(48.dp).testTag("auth_toggle_confirm_password_visibility")
                    ) {
                        Icon(
                            imageVector = if (form.isConfirmPasswordVisible) LucideIcons.EyeOff else LucideIcons.Eye,
                            contentDescription = if (form.isConfirmPasswordVisible) "Ocultar senha" else "Mostrar senha",
                            tint = theme.textSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onSubmit() }),
                modifier = Modifier.testTag("auth_confirm_password_field")
            )
        }
        if (!registering) {
            Box(
                modifier = Modifier
                    .align(Alignment.End)
                    .heightIn(min = 48.dp)
                    .clip(ContinuousCapsule)
                    .clickable(role = Role.Button, onClick = onForgotPassword)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Esqueci minha senha",
                    color = rememberAdaptiveLiquidGlassInk(theme.accent, theme.bgSecondary, 4.5f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Suppress("UNUSED_PARAMETER")
@Composable
fun AuthPrimaryActionsSection(
    authMode: AuthMode,
    isLoading: Boolean,
    isLinkingFromGuest: Boolean,
    backdrop: Backdrop,
    onSubmitPrimary: () -> Unit,
    onLoginWithGoogle: () -> Unit,
    onContinueAsGuest: () -> Unit
) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        LiquidGlassPillButton(
            text = when {
                isLoading -> "Aguarde…"
                authMode == AuthMode.LOGIN -> "Entrar na coleção"
                else -> "Criar conta"
            },
            onClick = onSubmitPrimary,
            enabled = !isLoading,
            emphasized = true,
            loading = isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("auth_primary_button")
        )
        Spacer(Modifier.height(6.dp))
        AuthAlternativeActions(
            enabled = !isLoading,
            showGuest = !isLinkingFromGuest,
            onGoogle = onLoginWithGoogle,
            onGuest = onContinueAsGuest
        )
    }
}

// AUTH_FORM_MOTION_V4
