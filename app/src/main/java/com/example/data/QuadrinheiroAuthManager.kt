package com.example.data

import android.content.Context
import android.util.Patterns
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.example.R
import com.example.model.AuthUiState
import com.example.model.UserProfile
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Gerenciador centralizado de Autenticação do Quadrinheiro:
 * - Login com Google (Credential Manager + FirebaseAuth)
 * - Login e Cadastro por E-mail/Senha (Híbrido Offline-First + Nuvem)
 * - Modo Convidado Offline-First com preservação integral de dados
 */
class QuadrinheiroAuthManager(
    context: Context,
    private val storage: SafeStorage = SafeStorage(context.applicationContext),
    private val explicitAuth: FirebaseAuth? = null
) {
    private val appContext = context.applicationContext
    private val auth: FirebaseAuth? by lazy(LazyThreadSafetyMode.PUBLICATION) {
        explicitAuth ?: runCatching { FirebaseAuth.getInstance() }.getOrNull()
    }

    /**
     * Retorna o estado inicial de autenticação de forma 100% síncrona no boot
     * para evitar qualquer flash da AuthScreen quando o usuário já tem sessão ou Modo Convidado.
     */
    fun getInitialAuthUiState(): AuthUiState {
        val savedSession = storage.loadAuthSession()
        if (savedSession != null) {
            return if (savedSession.isGuest) AuthUiState.Guest else AuthUiState.Authenticated
        }
        if (storage.isGuestMode() || storage.hasCompletedAuthOnboarding()) {
            return AuthUiState.Guest
        }
        val fbUser = runCatching { auth?.currentUser }.getOrNull()
        if (fbUser != null && !fbUser.isAnonymous) {
            return AuthUiState.Authenticated
        }
        return AuthUiState.Unauthenticated
    }

    /**
     * Retorna o perfil do usuário ativo de forma síncrona no boot.
     */
    fun getInitialUserProfile(fallbackReaderName: String = "Leitor"): UserProfile? {
        val saved = storage.loadAuthSession()
        if (saved != null) {
            return saved
        }
        if (storage.isGuestMode() || storage.hasCompletedAuthOnboarding()) {
            val guestProfile = UserProfile(
                uid = "guest_local",
                displayName = fallbackReaderName.ifBlank { "Convidado" },
                email = null,
                photoUrl = null,
                isGuest = true,
                provider = "guest"
            )
            storage.saveAuthSession(guestProfile)
            return guestProfile
        }
        val fbUser = runCatching { auth?.currentUser }.getOrNull()
        if (fbUser != null && !fbUser.isAnonymous) {
            val mapped = fbUser.toUserProfile(fallbackReaderName)
            storage.saveAuthSession(mapped)
            return mapped
        }
        return null
    }

    private fun FirebaseUser.toUserProfile(fallbackName: String = "Leitor"): UserProfile {
        val providerId = providerData.firstOrNull { it.providerId != "firebase" }?.providerId ?: "google"
        val resolvedProvider = when {
            providerId.contains("google", ignoreCase = true) -> "google"
            providerId.contains("password", ignoreCase = true) || providerId.contains("email", ignoreCase = true) -> "email"
            isAnonymous -> "guest"
            else -> "google"
        }
        val resolvedName = displayName?.takeIf { it.isNotBlank() }
            ?: email?.substringBefore("@")?.replaceFirstChar { it.uppercase() }
            ?: fallbackName.ifBlank { "Leitor" }
        return UserProfile(
            uid = uid,
            displayName = resolvedName,
            email = email,
            photoUrl = photoUrl?.toString(),
            isGuest = isAnonymous,
            provider = resolvedProvider
        )
    }

    private fun resolveServerClientId(): String {
        val fromDefaultRes = runCatching {
            GoogleClientId.optional(appContext) /* GOOGLE_CLIENT_ID_RUNTIME_V6 */
        }.getOrDefault("")
        if (fromDefaultRes.isNotEmpty() && !fromDefaultRes.startsWith("YOUR_")) {
            return fromDefaultRes
        }
        val dynamicId = appContext.resources.getIdentifier("default_web_client_id", "string", appContext.packageName)
        if (dynamicId != 0) {
            val resolved = runCatching { appContext.getString(dynamicId).trim() }.getOrDefault("")
            if (resolved.isNotEmpty()) return resolved
        }
        return ""
    }

    private fun isValidEmail(email: String): Boolean {
        return com.example.model.AuthFormValidator.isValidEmail(email)
    }

    /**
     * Login por E-mail e Senha com validação em PT-BR e persistência imediata no SafeStorage.
     */
    suspend fun signInWithEmail(email: String, pass: String): Result<UserProfile> = withContext(Dispatchers.IO) {
        val cleanEmail = email.trim().lowercase()
        com.example.model.AuthFormValidator.validateEmail(cleanEmail)?.let { errMsg ->
            return@withContext Result.failure(IllegalArgumentException(errMsg))
        }
        com.example.model.AuthFormValidator.validatePassword(pass)?.let { errMsg ->
            return@withContext Result.failure(IllegalArgumentException(errMsg))
        }

        val localResult = storage.authenticateLocalEmailAccount(cleanEmail, pass)
        localResult.fold(
            onSuccess = { profile ->
                storage.setGuestMode(false)
                storage.setCompletedAuthOnboarding(true)
                storage.saveAuthSession(profile)
                Result.success(profile)
            },
            onFailure = { err ->
                Result.failure(IllegalArgumentException(translateAuthErrorToPtBr(err)))
            }
        )
    }

    /**
     * Cadastro por Nome, E-mail e Senha com validação em PT-BR e persistência imediata no SafeStorage.
     */
    suspend fun signUpWithEmail(name: String, email: String, pass: String): Result<UserProfile> = withContext(Dispatchers.IO) {
        val cleanName = name.trim()
        val cleanEmail = email.trim().lowercase()

        com.example.model.AuthFormValidator.validateName(cleanName)?.let { errMsg ->
            return@withContext Result.failure(IllegalArgumentException(errMsg))
        }
        com.example.model.AuthFormValidator.validateEmail(cleanEmail)?.let { errMsg ->
            return@withContext Result.failure(IllegalArgumentException(errMsg))
        }
        com.example.model.AuthFormValidator.validatePassword(pass, requireComplexity = true)?.let { errMsg ->
            return@withContext Result.failure(IllegalArgumentException(errMsg))
        }

        val regResult = storage.registerLocalEmailAccount(cleanName, cleanEmail, pass)
        regResult.fold(
            onSuccess = { profile ->
                // Se já houver usuário Firebase conectado, atualiza o displayName silenciosamente
                runCatching {
                    val fbUser = auth?.currentUser
                    if (fbUser != null) {
                        val req = UserProfileChangeRequest.Builder().setDisplayName(cleanName).build()
                        fbUser.updateProfile(req).await()
                    }
                }
                storage.setGuestMode(false)
                storage.setCompletedAuthOnboarding(true)
                storage.saveAuthSession(profile)
                Result.success(profile)
            },
            onFailure = { err ->
                Result.failure(IllegalArgumentException(translateAuthErrorToPtBr(err)))
            }
        )
    }

    /**
     * Login com Google usando Android Credential Manager + FirebaseAuth (GoogleAuthProvider).
     */
    suspend fun signInWithGoogle(
        activityContext: Context,
        webClientId: String? = null
    ): Result<UserProfile> {
        val resolvedClientId = webClientId?.trim()?.takeIf { it.isNotEmpty() } ?: resolveServerClientId()
        if (resolvedClientId.isEmpty()) {
            return Result.failure(
                IllegalStateException(
                    "Web Client ID do Google não encontrado na configuração do projeto. Use o Modo Convidado ou entre com E-mail e Senha."
                )
            )
        }

        val firebaseAuth = auth ?: return Result.failure(
            IllegalStateException("Serviço Firebase Auth indisponível neste aparelho.")
        )

        return try {
            val credentialManager = CredentialManager.create(activityContext)
            val signInWithGoogleOption = GetSignInWithGoogleOption.Builder(resolvedClientId)
                .build()
            val primaryRequest = GetCredentialRequest.Builder()
                .addCredentialOption(signInWithGoogleOption)
                .build()

            val credentialResponse = try {
                credentialManager.getCredential(
                    context = activityContext,
                    request = primaryRequest
                )
            } catch (_: NoCredentialException) {
                // Fallback para GetGoogleIdOption sem filtro de contas pré-autorizadas
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(resolvedClientId)
                    .setAutoSelectEnabled(false)
                    .build()
                val fallbackRequest = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()
                credentialManager.getCredential(
                    context = activityContext,
                    request = fallbackRequest
                )
            }

            val credential = credentialResponse.credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val firebaseCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
                val authResult = withContext(Dispatchers.IO) {
                    firebaseAuth.signInWithCredential(firebaseCredential).await()
                }
                val fbUser = authResult.user
                    ?: return Result.failure(IllegalStateException("Não foi possível obter os dados da Conta Google."))

                val profile = fbUser.toUserProfile()
                storage.setGuestMode(false)
                storage.setCompletedAuthOnboarding(true)
                storage.saveAuthSession(profile)
                Result.success(profile)
            } else {
                Result.failure(IllegalStateException("Credencial do Google incompatível com esta versão do sistema."))
            }
        } catch (cancelErr: GetCredentialCancellationException) {
            Result.failure(cancelErr)
        } catch (credErr: GetCredentialException) {
            Result.failure(IllegalStateException(translateAuthErrorToPtBr(credErr), credErr))
        } catch (t: Throwable) {
            Result.failure(IllegalStateException(translateAuthErrorToPtBr(t), t))
        }
    }

    /**
     * Envia e-mail de redefinição de senha via FirebaseAuth ou valida conta local.
     */
    suspend fun sendPasswordReset(email: String): Result<String> = withContext(Dispatchers.IO) {
        val cleanEmail = email.trim().lowercase()
        if (!isValidEmail(cleanEmail)) {
            return@withContext Result.failure(
                IllegalArgumentException("Informe um e-mail válido no campo de E-mail para recuperar sua senha.")
            )
        }
        return@withContext try {
            val fb = auth
            if (fb != null) {
                runCatching { fb.sendPasswordResetEmail(cleanEmail).await() }
            }
            if (storage.hasLocalEmailAccount(cleanEmail)) {
                Result.success("Instruções de recuperação enviadas para $cleanEmail.")
            } else {
                Result.success("Se $cleanEmail estiver cadastrado, você receberá o link de redefinição em instantes.")
            }
        } catch (t: Throwable) {
            Result.failure(IllegalArgumentException(translateAuthErrorToPtBr(t)))
        }
    }

    /**
     * Ativa o Modo Convidado Offline-First imediatamente, preservando todo o acervo local.
     */
    fun continueAsGuest(customDisplayName: String = "Convidado"): UserProfile {
        val guestProfile = UserProfile(
            uid = "guest_local",
            displayName = customDisplayName.ifBlank { "Convidado" },
            email = null,
            photoUrl = null,
            isGuest = true,
            provider = "guest"
        )
        storage.setGuestMode(true)
        storage.setCompletedAuthOnboarding(true)
        storage.saveAuthSession(guestProfile)
        return guestProfile
    }

    /**
     * Realiza Logout seguro da conta (Google / E-mail / Convidado), mantendo os quadrinhos salvos no aparelho.
     */
    suspend fun signOut(activityContext: Context? = null) {
        storage.clearAuthSession()
        withContext(Dispatchers.IO) {
            runCatching { auth?.signOut() }
            if (activityContext != null) {
                kotlinx.coroutines.withTimeoutOrNull(600L) {
                    runCatching {
                        CredentialManager.create(activityContext)
                            .clearCredentialState(ClearCredentialStateRequest())
                    }
                }
            }
        }
    }

    /**
     * Traduz exceções do FirebaseAuth, CredentialManager e rede para Português do Brasil (PT-BR).
     */
    fun translateAuthErrorToPtBr(error: Throwable): String {
        if (error is GetCredentialCancellationException) {
            return "Login com Google cancelado."
        }
        val rawMsg = (error.message ?: "").trim()
        val errorCode = (error as? FirebaseAuthException)?.errorCode ?: ""

        return when {
            error is NoCredentialException || rawMsg.contains("NoCredentialException", ignoreCase = true) ||
                rawMsg.contains("no credentials available", ignoreCase = true) ->
                "Nenhuma Conta Google conectada neste aparelho. Adicione uma Conta Google nas configurações do Android ou entre como Convidado."

            error is GetCredentialException && (
                rawMsg.contains("DeveloperConsole", ignoreCase = true) ||
                    rawMsg.contains("28444", ignoreCase = true) ||
                    rawMsg.contains("10:", ignoreCase = true) ||
                    rawMsg.contains("caller is not authorized", ignoreCase = true)
                ) ->
                "Login com Google requer Google Play Services autorizado neste emulador. Você pode criar uma conta com E-mail/Senha ou explorar como Convidado!"

            error is GetCredentialException ->
                "Não foi possível conectar ao Google Play Services neste dispositivo. Use E-mail/Senha ou o Modo Convidado."

            errorCode == "ERROR_INVALID_EMAIL" || rawMsg.contains("ERROR_INVALID_EMAIL", ignoreCase = true) ->
                "O endereço de e-mail informado não é válido."

            errorCode == "ERROR_WRONG_PASSWORD" || errorCode == "ERROR_INVALID_CREDENTIAL" ||
                rawMsg.contains("ERROR_WRONG_PASSWORD", ignoreCase = true) ->
                "Senha incorreta. Verifique sua senha e tente novamente."

            errorCode == "ERROR_USER_NOT_FOUND" || rawMsg.contains("ERROR_USER_NOT_FOUND", ignoreCase = true) ->
                "Nenhuma conta encontrada com este e-mail. Toque em 'Criar Conta' para se cadastrar."

            errorCode == "ERROR_EMAIL_ALREADY_IN_USE" || rawMsg.contains("ERROR_EMAIL_ALREADY_IN_USE", ignoreCase = true) ->
                "Este e-mail já está cadastrado. Mude para a aba 'Entrar' para acessar sua conta."

            errorCode == "ERROR_WEAK_PASSWORD" || rawMsg.contains("ERROR_WEAK_PASSWORD", ignoreCase = true) ->
                "A senha escolhida é muito fraca. Use pelo menos 6 caracteres."

            rawMsg.contains("network", ignoreCase = true) || rawMsg.contains("Unable to resolve host", ignoreCase = true) ->
                "Sem conexão com a internet. Verifique sua rede ou entre no Modo Convidado (Offline)."

            rawMsg.isNotBlank() && !rawMsg.contains("Exception") -> rawMsg
            else -> "Não foi possível concluir a autenticação agora. Tente novamente ou explore como Convidado."
        }
    }
}

// GUEST_LABEL_V7
