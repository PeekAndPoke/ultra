package de.peekandpoke.funktor.auth.provider

import de.peekandpoke.funktor.auth.AuthError
import de.peekandpoke.funktor.auth.AuthFrontendRoutes
import de.peekandpoke.funktor.auth.AuthRealm
import de.peekandpoke.funktor.auth.AuthSystem
import de.peekandpoke.funktor.auth.domain.AuthRecord
import de.peekandpoke.funktor.auth.model.AuthProviderModel
import de.peekandpoke.funktor.auth.model.AuthRecoverAccountRequest
import de.peekandpoke.funktor.auth.model.AuthRecoverAccountResponse
import de.peekandpoke.funktor.auth.model.AuthSetPasswordRequest
import de.peekandpoke.funktor.auth.model.AuthSetPasswordResponse
import de.peekandpoke.funktor.auth.model.AuthSignInRequest
import de.peekandpoke.funktor.auth.model.AuthSignUpRequest
import de.peekandpoke.ultra.common.remote.buildUri
import de.peekandpoke.ultra.common.toBase64
import de.peekandpoke.ultra.log.Log
import de.peekandpoke.ultra.vault.Stored
import kotlinx.serialization.json.buildJsonObject
import kotlin.time.Duration.Companion.hours

/**
 * Authentication provider for handling email and password-based authentication.
 *
 * This class provides methods for user login and user account updates, such as
 * changing passwords. It validates the user's credentials against the stored
 * records while adhering to the realm's password policy.
 *
 * @param id A unique identifier for the provider.
 * @param deps Lazily loaded dependencies required for authentication operations.
 */
class EmailAndPasswordAuth(
    override val id: String,
    override val capabilities: Set<AuthProviderModel.Capability> = setOf(AuthProviderModel.Capability.SignIn),
    private val log: Log,
    private val frontendUrls: FrontendUrls,
    deps: Lazy<AuthSystem.Deps>,
) : AuthProvider {
    /**
     * Factory for creating instances of the EmailAndPasswordAuth provider.
     *
     * This factory allows the configuration of authentication providers with a default
     * or custom identifier. It streamlines the process of instantiating the provider
     * with the required dependencies.
     *
     * @param deps Lazily loaded dependencies required for creating instances of the provider.
     */
    class Factory(
        private val deps: Lazy<AuthSystem.Deps>,
        private val log: Log,
    ) {
        operator fun invoke(
            id: String = "email-password",
            frontendUrls: FrontendUrls,
            capabilities: Set<AuthProviderModel.Capability> = setOf(AuthProviderModel.Capability.SignIn),
        ) = EmailAndPasswordAuth(
            id = id,
            deps = deps,
            log = log,
            capabilities = capabilities,
            frontendUrls = frontendUrls,
        )
    }

    data class FrontendUrls(
        /**
         * The base url of the auth routes in the frontend application.
         *
         * Usually https://my-app.io/auth/
         */
        val baseUrl: String,
        /**
         * Frontend routes, needed for generating deep-links, f.e. for:
         * - account activation/verification after email and password sign-up
         * - password reset
         */
        val routes: AuthFrontendRoutes = AuthFrontendRoutes(mountPoint = baseUrl.trimEnd('/')),
    )

    /**
     * Lazily loaded dependencies required for authentication operations.
     */
    private val deps by deps

    /**
     * {@inheritDoc}
     */
    override fun asApiModel(): AuthProviderModel {
        return AuthProviderModel(
            id = id,
            type = AuthProviderModel.TYPE_EMAIL_PASSWORD,
            capabilities = capabilities,
            config = buildJsonObject { },
        )
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun <USER> signIn(realm: AuthRealm<USER>, request: AuthSignInRequest): Stored<USER> {

        val typed: AuthSignInRequest.EmailAndPassword = (request as? AuthSignInRequest.EmailAndPassword)
            ?: throw AuthError.invalidCredentials()

        val email = typed.email.takeIf { it.isNotBlank() }
            ?: throw AuthError.invalidCredentials()

        val password = typed.password.takeIf { it.isNotBlank() }
            ?: throw AuthError.invalidCredentials()

        val user = realm.loadUserByEmail(email)
            ?: throw AuthError.invalidCredentials()

        validateCurrentPassword(realm, user, password).takeIf { it }
            ?: throw AuthError.invalidCredentials()

        return user
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun <USER> signUp(
        realm: AuthRealm<USER>, request: AuthSignUpRequest,
    ): AuthProvider.SignUpResult<USER> {

        val typed = request as? AuthSignUpRequest.EmailAndPassword
            ?: throw AuthError("Invalid sign-up request for email/password")

        val email = typed.email.trim().lowercase()

        if (email.isBlank()) throw AuthError("Invalid email")

        // Enforce the password policy
        if (!realm.passwordPolicy.matches(typed.password)) throw AuthError.weakPassword()

        // Ensure no existing user
        if (realm.loadUserByEmail(email) != null) throw AuthError("User already exists")

        // Create user via realm hook
        val user = realm.createUserForSignup(
            AuthRealm.CreateUserForSignupParams.of(
                email = email,
                displayName = typed.displayName,
            )
        )

        // Store password record
        deps.storage.authRecords.create {
            createPasswordRecord(realmId = realm.id, ownerId = user._id, password = typed.password)
        }

        return AuthProvider.SignUpResult(
            user = user,
            requiresActivation = true,
        )
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun <USER> setPassword(
        realm: AuthRealm<USER>, request: AuthSetPasswordRequest,
    ): AuthSetPasswordResponse {

        // 1. Check for new password to meet the password policy
        realm.passwordPolicy.matches(request.newPassword).takeIf { it }
            ?: throw AuthError.weakPassword()

        val user = realm.loadUserById(request.userId)
            ?: throw AuthError.userNotFound(request.userId)

        // 3. Write new password entry into database
        deps.storage.authRecords.create {
            createPasswordRecord(realmId = realm.id, ownerId = user._id, password = request.newPassword)
        }

        val emailResult = realm.messaging.sendPasswordChangedEmail(user)

        if (emailResult.success.not()) {
            log.warning(
                "Sending 'Password Changed' Email failed for user ${user._id} ${realm.getUserEmail(user)}"
            )
        }

        return AuthSetPasswordResponse(success = true)
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun <USER> recoverAccountInitPasswordReset(
        realm: AuthRealm<USER>, request: AuthRecoverAccountRequest.InitPasswordReset,
    ): AuthRecoverAccountResponse.InitPasswordReset {

        val user = realm.loadUserByEmail(request.email)
            ?: return AuthRecoverAccountResponse.InitPasswordReset

        val token = deps.random.getToken(length = 256).toBase64()

        // Store the token in the database
        deps.storage.authRecords.create {
            AuthRecord.PasswordRecoveryToken(
                realm = realm.id,
                ownerId = user._id,
                token = token,
                expiresAt = deps.kronos.instantNow().plus(1.hours).toEpochSeconds(),
            )
        }

        val emailResult = realm.messaging.sendPasswordRecoveryEmil(
            user = user,
            resetUrl = buildUri(frontendUrls.routes.resetPassword.pattern) {
                set(AuthFrontendRoutes.PROVIDER_PARAM, id)
                set(AuthFrontendRoutes.TOKEN_PARAM, token)
            }
        )

        if (emailResult.success.not()) {
            log.warning(
                "Sending 'Password Changed' Email failed for user ${user._id} ${realm.getUserEmail(user)}"
            )
        }

        return AuthRecoverAccountResponse.InitPasswordReset
    }

    /**
     * Validate token for password reset
     */
    override suspend fun <USER> recoverAccountValidatePasswordResetToken(
        realm: AuthRealm<USER>, request: AuthRecoverAccountRequest.ValidatePasswordResetToken,
    ): AuthRecoverAccountResponse.ValidatePasswordResetToken {

        val tokenRecord = deps.storage.authRecords
            .findByToken(type = AuthRecord.PasswordRecoveryToken, realm = realm.id, token = request.token)

        return AuthRecoverAccountResponse.ValidatePasswordResetToken(
            success = tokenRecord != null,
        )
    }

    /**
     * Validate token for password reset
     */
    override suspend fun <USER> recoverAccountSetPasswordWithToken(
        realm: AuthRealm<USER>, request: AuthRecoverAccountRequest.SetPasswordWithToken,
    ): AuthRecoverAccountResponse.SetPasswordWithToken {

        val tokenRecord = deps.storage.authRecords
            .findByToken(type = AuthRecord.PasswordRecoveryToken, realm = realm.id, token = request.token)
            ?: return AuthRecoverAccountResponse.SetPasswordWithToken(success = false)

        // Write a new password entry into the auth records storage
        deps.storage.authRecords.create {
            createPasswordRecord(realmId = realm.id, ownerId = tokenRecord.value.ownerId, password = request.password)
        }

        // TODO: send password changed email

        return AuthRecoverAccountResponse.SetPasswordWithToken(
            success = true,
        )
    }


    /**
     * Validates the given [password] against the latest password for the given [user] in the [realm].
     */
    private suspend fun <USER> validateCurrentPassword(
        realm: AuthRealm<USER>, user: Stored<USER>, password: String,
    ): Boolean {
        val record = deps.storage.authRecords
            .findLatestRecordBy(type = AuthRecord.Password, realm = realm.id, owner = user._id)
            ?: return false

        return deps.passwordHasher.check(plaintext = password, hash = record.value.token)
    }

    /**
     * Creates a new password record for the given [realmId], [ownerId] and [password].
     */
    private fun createPasswordRecord(realmId: String, ownerId: String, password: String): AuthRecord.Password {
        return AuthRecord.Password(
            realm = realmId,
            ownerId = ownerId,
            token = deps.passwordHasher.hash(password),
        )
    }
}
