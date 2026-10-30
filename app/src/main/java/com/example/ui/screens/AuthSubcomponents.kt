package com.example.ui.screens

import androidx.compose.animation.animateContentSize

import androidx.compose.animation.core.tween

import androidx.compose.animation.core.spring

import androidx.compose.animation.shrinkVertically

import androidx.compose.animation.expandVertically

import androidx.compose.animation.fadeOut

import androidx.compose.animation.fadeIn

import androidx.compose.animation.AnimatedVisibility

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.model.AuthFeedbackBanner
import com.example.model.AuthFormState
import com.example.model.AuthMode
import com.example.ui.components.QuadrinheiroLogo
import com.example.ui.components.QuadrinheiroLogoVariant
import com.example.ui.theme.LocalIOS27Theme
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import androidx.compose.ui.graphics.Brush
import com.example.ui.components.liquidGlass

@Suppress("UNUSED_PARAMETER")
@Composable
fun AuthBackdropLayer(backdrop: Backdrop, reduceTransparency: Boolean, isLight: Boolean) {
    // GLASS_AUTH_STYLE_V2: static theme gradient recorded for the glass surfaces.
    val theme = LocalIOS27Theme.current
    val layerModifier = if (backdrop is LayerBackdrop) Modifier.layerBackdrop(backdrop) else Modifier
    Box(layerModifier.fillMaxSize().background(
        Brush.verticalGradient(listOf(
            theme.bgPrimary,
            androidx.compose.ui.graphics.lerp(theme.bgPrimary, theme.accent, if (reduceTransparency) 0.03f else 0.12f),
            theme.bgPrimary
        ))
    ))
}

fun Modifier.authGlassCardSurface(): Modifier = composed {
    liquidGlass(shape = RoundedCornerShape(26.dp), elevation = 0.dp)
}

@Composable
fun AuthBrandHeader(isLinkingFromGuest: Boolean) {
    val theme = LocalIOS27Theme.current
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        QuadrinheiroLogo(variant = QuadrinheiroLogoVariant.Primary, titleVisible = false, modifier = Modifier.size(120.dp))
        Text("Quadrinheiro", style = MaterialTheme.typography.headlineSmall, color = theme.textPrimary)
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
    Text(
        text = banner.message,
        color = if (banner.isError) MaterialTheme.colorScheme.error else theme.textPrimary,
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite }
    )
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
    val theme = LocalIOS27Theme.current
    val focus = LocalFocusManager.current
    val registering = form.mode != AuthMode.LOGIN
    val next = KeyboardActions(onNext = { focus.moveFocus(FocusDirection.Down) })
    // AUTH_UNIFIED_HEIGHT_V5: a single size animation, not two competing field springs.
    Column(
        modifier = Modifier.fillMaxWidth().animateContentSize(
            animationSpec = tween(260, easing = androidx.compose.animation.core.FastOutSlowInEasing)
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (registering) {
            OutlinedTextField(
            shape = RoundedCornerShape(18.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = theme.textPrimary,
                unfocusedTextColor = theme.textPrimary,
                focusedContainerColor = theme.textPrimary.copy(alpha = 0.05f),
                unfocusedContainerColor = theme.textPrimary.copy(alpha = 0.04f),
                focusedBorderColor = theme.accent.copy(alpha = 0.45f),
                unfocusedBorderColor = theme.textPrimary.copy(alpha = 0.12f),
                focusedLabelColor = theme.accent,
                unfocusedLabelColor = theme.textSecondary,
                cursorColor = theme.accent
            ),
            value = form.name, onValueChange = onNameChange,
            label = { Text("Nome") }, singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("auth_name_field"),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next), keyboardActions = next
        )
        }
        OutlinedTextField(
            shape = RoundedCornerShape(18.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = theme.textPrimary,
                unfocusedTextColor = theme.textPrimary,
                focusedContainerColor = theme.textPrimary.copy(alpha = 0.05f),
                unfocusedContainerColor = theme.textPrimary.copy(alpha = 0.04f),
                focusedBorderColor = theme.accent.copy(alpha = 0.45f),
                unfocusedBorderColor = theme.textPrimary.copy(alpha = 0.12f),
                focusedLabelColor = theme.accent,
                unfocusedLabelColor = theme.textSecondary,
                cursorColor = theme.accent
            ),
            value = form.email, onValueChange = onEmailChange,
            label = { Text("E-mail") }, singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("auth_email_field"),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            keyboardActions = next
        )
        OutlinedTextField(
            shape = RoundedCornerShape(18.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = theme.textPrimary,
                unfocusedTextColor = theme.textPrimary,
                focusedContainerColor = theme.textPrimary.copy(alpha = 0.05f),
                unfocusedContainerColor = theme.textPrimary.copy(alpha = 0.04f),
                focusedBorderColor = theme.accent.copy(alpha = 0.45f),
                unfocusedBorderColor = theme.textPrimary.copy(alpha = 0.12f),
                focusedLabelColor = theme.accent,
                unfocusedLabelColor = theme.textSecondary,
                cursorColor = theme.accent
            ),
            value = form.password, onValueChange = onPasswordChange,
            label = { Text("Senha") }, singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("auth_password_field"),
            visualTransformation = if (form.isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = { TextButton(onClick = onTogglePasswordVisible) { Text(if (form.isPasswordVisible) "Ocultar" else "Mostrar") } },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = if (registering) ImeAction.Next else ImeAction.Done),
            keyboardActions = KeyboardActions(onNext = { focus.moveFocus(FocusDirection.Down) }, onDone = { onSubmit() })
        )
        if (registering) {
            OutlinedTextField(
            shape = RoundedCornerShape(18.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = theme.textPrimary,
                unfocusedTextColor = theme.textPrimary,
                focusedContainerColor = theme.textPrimary.copy(alpha = 0.05f),
                unfocusedContainerColor = theme.textPrimary.copy(alpha = 0.04f),
                focusedBorderColor = theme.accent.copy(alpha = 0.45f),
                unfocusedBorderColor = theme.textPrimary.copy(alpha = 0.12f),
                focusedLabelColor = theme.accent,
                unfocusedLabelColor = theme.textSecondary,
                cursorColor = theme.accent
            ),
            value = form.confirmPassword, onValueChange = onConfirmPasswordChange,
            label = { Text("Confirmar senha") }, singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("auth_confirm_password_field"),
            visualTransformation = if (form.isConfirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = { TextButton(onClick = onToggleConfirmPasswordVisible) { Text(if (form.isConfirmPasswordVisible) "Ocultar" else "Mostrar") } },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onSubmit() })
        )
        }
        if (!registering) TextButton(onClick = onForgotPassword, modifier = Modifier.align(Alignment.End)) {
            Text("Esqueci minha senha", color = LocalIOS27Theme.current.accent)
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
    val theme = LocalIOS27Theme.current
    val foreground = if (theme.accent.luminance() > 0.179f) Color.Black else Color.White
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(
            onClick = onSubmitPrimary, enabled = !isLoading,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("auth_primary_button"),
            colors = ButtonDefaults.buttonColors(containerColor = theme.accent, contentColor = foreground)
        ) {
            if (isLoading) {
                com.example.ui.components.IOSLoadingIndicator(Modifier.size(20.dp), color = theme.textPrimary)
                Spacer(Modifier.width(8.dp))
                Text("Aguarde…")
            } else Text(if (authMode == AuthMode.LOGIN) "Entrar na coleção" else "Criar conta")
        }
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
