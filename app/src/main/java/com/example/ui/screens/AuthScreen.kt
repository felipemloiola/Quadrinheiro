package com.example.ui.screens

import androidx.compose.ui.input.pointer.pointerInput

import androidx.compose.ui.input.pointer.PointerEventPass

import androidx.compose.foundation.gestures.waitForUpOrCancellation

import androidx.compose.foundation.gestures.awaitFirstDown

import androidx.compose.foundation.gestures.awaitEachGesture

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.AuthActions
import com.example.model.AuthFeedbackBanner
import com.example.model.AuthFeedbackType
import com.example.model.AuthFormState
import com.example.model.AuthFormValidator
import com.example.model.AuthMode
import com.example.model.AuthScreenUiState
import com.example.ui.components.LocalLiquidGlassBackdrop
import com.example.ui.components.liquidGlassNavSurface
import com.example.ui.theme.LocalAppSettings
import com.example.ui.theme.LocalAppleMaterials
import com.example.ui.theme.LocalIOS27Theme
import com.kyant.backdrop.backdrops.rememberLayerBackdrop

/**
 * Overload declarativo da AuthScreen baseado em estado único consolidado (`AuthScreenUiState`)
 * e contrato de ações agrupado (`AuthActions`).
 */
@Composable
fun AuthScreen(
    uiState: AuthScreenUiState,
    isLinkingFromGuest: Boolean = false,
    actions: AuthActions
) {
    AuthScreen(
        isLoading = uiState.isLoading,
        feedbackMessage = uiState.banner?.message,
        isFeedbackError = uiState.banner?.isError ?: true,
        isLinkingFromGuest = isLinkingFromGuest,
        initialMode = uiState.modalInitialMode,
        onClearFeedback = actions.onClearFeedback,
        onLoginWithEmail = actions.onLoginWithEmail,
        onRegisterWithEmail = actions.onRegisterWithEmail,
        onLoginWithGoogle = actions.onLoginWithGoogle,
        onContinueAsGuest = actions.onContinueAsGuest,
        onResetPassword = actions.onResetPassword,
        onDismissModal = actions.onDismissModal
    )
}

/**
 * Orquestrador principal da tela de Login / Cadastro em Liquid Glass.
 * Delega fundo óptico, cabeçalho, controle segmentado, campos e ações para `AuthSubcomponents.kt`.
 */
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
    val keyboard = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current

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

    val formState = remember(
        authMode,
        nameInput,
        emailInput,
        passwordInput,
        confirmPasswordInput,
        passwordVisible,
        confirmPasswordVisible
    ) {
        AuthFormState(
            mode = authMode,
            name = nameInput,
            email = emailInput,
            password = passwordInput,
            confirmPassword = confirmPasswordInput,
            isPasswordVisible = passwordVisible,
            isConfirmPasswordVisible = confirmPasswordVisible
        )
    }

    val authBackdrop = rememberLayerBackdrop()

    val submitPrimaryAction = {
        focusManager.clearFocus()
        onClearFeedback()
        val validationError = AuthFormValidator.validateForm(formState)
        if (validationError != null) {
            localBanner = AuthFeedbackBanner(
                message = validationError,
                type = AuthFeedbackType.ERROR
            )
        } else {
            localBanner = null
            if (formState.mode == AuthMode.LOGIN) {
                onLoginWithEmail(formState.trimmedEmail, formState.password)
            } else {
                onRegisterWithEmail(formState.trimmedName, formState.trimmedEmail, formState.password)
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

    val clearLocalAndRemoteFeedback = {
        if (localBanner != null) localBanner = null
        if (!feedbackMessage.isNullOrBlank()) onClearFeedback()
    }

    CompositionLocalProvider(
        LocalLiquidGlassBackdrop provides authBackdrop
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(themeColors.bgPrimary)
                .testTag("auth_screen_root")
                // AUTH_OUTSIDE_FOCUS_V4: observe unconsumed taps, never steal field/button taps.
                .pointerInput(focusManager, keyboard) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Final)
                        if (!down.isConsumed) {
                            val up = waitForUpOrCancellation(pass = PointerEventPass.Final)
                            if (up != null && !up.isConsumed) {
                                focusManager.clearFocus()
                                keyboard?.hide()
                            }
                        }
                    }
                }
        ) {
            // 1. Camada de Fundo Imersiva gravada no authBackdrop (respeita tema Claro/Escuro e Reduzir Transparência)
            AuthBackdropLayer(
                backdrop = authBackdrop,
                reduceTransparency = appSettings.reduceTransparency,
                isLight = s.isLight
            )

            // 2. Shell de Layout Responsivo em Liquid Glass com card mais compacto e consistente com o app
            AuthGlassScreenShell(
                isLinkingFromGuest = isLinkingFromGuest,
                onDismissModal = onDismissModal
            ) {
                // 2.1 Segmented Pill Toggle ("Entrar" vs "Criar Conta")
                AuthModeSegmentedToggle(
                    selectedMode = formState.mode,
                    onSelectMode = { nextMode ->
                        focusManager.clearFocus()
                        keyboard?.hide()
                        authMode = nextMode
                        localBanner = null
                        onClearFeedback()
                    }
                )

                // 2.2 Banner de Feedback Estruturado (apenas quando existe mensagem)
                AuthFeedbackBannerCard(banner = activeBanner)

                // 2.3 Grupo de Campos de Credenciais
                AuthCredentialsFieldsSection(
                    form = formState,
                    onNameChange = {
                        nameInput = it
                        clearLocalAndRemoteFeedback()
                    },
                    onEmailChange = {
                        emailInput = it
                        clearLocalAndRemoteFeedback()
                    },
                    onPasswordChange = {
                        passwordInput = it
                        clearLocalAndRemoteFeedback()
                    },
                    onConfirmPasswordChange = {
                        confirmPasswordInput = it
                        clearLocalAndRemoteFeedback()
                    },
                    onTogglePasswordVisible = { passwordVisible = !passwordVisible },
                    onToggleConfirmPasswordVisible = { confirmPasswordVisible = !confirmPasswordVisible },
                    onForgotPassword = {
                        focusManager.clearFocus()
                        localBanner = null
                        onResetPassword(formState.trimmedEmail)
                    },
                    onSubmit = submitPrimaryAction
                )

                // 2.4 Ações Principais, Login com Google e Modo Convidado
                AuthPrimaryActionsSection(
                    authMode = formState.mode,
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
                        // Sempre entra como convidado com app "zerado"; não apenas fecha o modal
                        onContinueAsGuest()
                    }
                )
            }
        }
    }
}

/**
 * Pure UI Shell responsável pelo scroll, WindowInsets, botão de fechar modal,
 * cabeçalho da marca e card translúcido central em Liquid Glass.
 * Mantém layout mais compacto, alinhado aos demais cards do app.
 */
@Composable
private fun AuthGlassScreenShellContent(
    isLinkingFromGuest: Boolean,
    onDismissModal: (() -> Unit)?,
    content: @Composable ColumnScope.() -> Unit
) {
    val themeColors = LocalIOS27Theme.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 420.dp)
        ) {
            if (onDismissModal != null) {
                val closeCd = stringResource(R.string.auth_cd_close_modal)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(44.dp)
                        .liquidGlassNavSurface(shape = CircleShape, elevation = 4.dp)
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
                Spacer(modifier = Modifier.height(2.dp))

                AuthBrandHeader(isLinkingFromGuest = isLinkingFromGuest)

                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .authGlassCardSurface()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    content = content
                )

                Spacer(modifier = Modifier.height(6.dp))
            }
        }
    }
}


// AUTH_SPRING_ENTRANCE_V3
@Composable
private fun AuthGlassScreenShell(
    isLinkingFromGuest: Boolean,
    onDismissModal: (() -> Unit)?,
    content: @Composable ColumnScope.() -> Unit
) {
    AuthGlassEntrance {
        AuthGlassScreenShellContent(isLinkingFromGuest, onDismissModal, content)
    }
}
