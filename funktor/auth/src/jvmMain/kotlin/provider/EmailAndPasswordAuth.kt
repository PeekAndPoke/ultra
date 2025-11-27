package de.peekandpoke.funktor.auth.provider

import de.peekandpoke.funktor.auth.AuthError
import de.peekandpoke.funktor.auth.AuthFrontendRoutes
import de.peekandpoke.funktor.auth.AuthRandom
import de.peekandpoke.funktor.auth.AuthRealm
import de.peekandpoke.funktor.auth.AuthRecordStorage
import de.peekandpoke.funktor.auth.AuthSystem
import de.peekandpoke.funktor.auth.domain.AuthRecord
import de.peekandpoke.funktor.auth.model.AuthProviderModel
import de.peekandpoke.funktor.auth.model.AuthRecoverAccountRequest
import de.peekandpoke.funktor.auth.model.AuthRecoverAccountResponse
import de.peekandpoke.funktor.auth.model.AuthSetPasswordRequest
import de.peekandpoke.funktor.auth.model.AuthSetPasswordResponse
import de.peekandpoke.funktor.auth.model.AuthSignInRequest
import de.peekandpoke.funktor.auth.model.AuthSignUpRequest
import de.peekandpoke.ultra.common.datetime.Kronos
import de.peekandpoke.ultra.common.datetime.MpInstant
import de.peekandpoke.ultra.common.isEmail
import de.peekandpoke.ultra.common.remote.buildUri
import de.peekandpoke.ultra.log.Log
import de.peekandpoke.ultra.security.password.PasswordHasher
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
 * @param services Lazily loaded dependencies required for authentication operations.
 */
class EmailAndPasswordAuth(
    override val capabilities: Set<AuthProviderModel.Capability> = setOf(AuthProviderModel.Capability.SignIn),
    private val frontendUrls: FrontendUrls,
    private val log: Log,
    services: Lazy<Services>,
) : AuthProvider {

    companion object {
        const val ID = "email-password"
    }

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
            frontendUrls: FrontendUrls,
            capabilities: Set<AuthProviderModel.Capability> = setOf(AuthProviderModel.Capability.SignIn),
            services: Lazy<Services> = lazy { Services(deps.value) },
        ) = EmailAndPasswordAuth(
            services = services,
            log = log,
            capabilities = capabilities,
            frontendUrls = frontendUrls,
        )

        // TODO: from app config ... auto-build frontend urls from app config
//        fun fromAppConfig()
    }

    /** Interface for providing dependencies to the [EmailAndPasswordAuth] provider. */
    interface Services {
        companion object {
            operator fun invoke(deps: AuthSystem.Deps): Services = DefaultServices(
                kronos = lazy { deps.kronos },
                authRandom = lazy { deps.random },
                authRecordStorage = lazy { deps.storage.authRecords },
                passwordHasher = lazy { deps.passwordHasher },
            )
        }

        /** Get the current instant */
        fun instantNow(): MpInstant

        /** Generate a random base64-encoded token with the given length in bytes */
        fun generateRandomBase64Token(length: Int): String

        /** Hash the given plaintext password */
        fun hashPassword(password: String): String

        /** Check if the given plaintext password matches the given hash */
        fun checkPassword(plaintext: String, hash: String): Boolean

        /** Create a new auth record of the given type */
        suspend fun <T : AuthRecord> createAuthRecord(record: () -> T): Stored<T>

        /** Find the password recovery token for the given [realm] and [owner] */
        suspend fun findLatestPasswordRecord(realm: String, owner: String): Stored<AuthRecord.Password>?

        /** Find the password recovery token for the given [realm] and [token] */
        suspend fun findPasswordRecoveryToken(realm: String, token: String): Stored<AuthRecord.PasswordRecoveryToken>?
    }

    /** Default implementation of the [Services] interface. */
    private class DefaultServices(
        kronos: Lazy<Kronos>,
        authRandom: Lazy<AuthRandom>,
        authRecordStorage: Lazy<AuthRecordStorage>,
        passwordHasher: Lazy<PasswordHasher>,
    ) : Services {
        private val kronos by kronos
        private val authRandom by authRandom
        private val authRecordStorage by authRecordStorage
        private val passwordHasher by passwordHasher

        /** @{inheritDoc} */
        override fun instantNow(): MpInstant {
            return kronos.instantNow()
        }

        /** @{inheritDoc} */
        override fun generateRandomBase64Token(length: Int): String {
            return authRandom.getTokenAsBase64(length)
        }

        /** @{inheritDoc} */
        override fun hashPassword(password: String): String {
            return passwordHasher.hashAsString(password)
        }

        /** @{inheritDoc} */
        override fun checkPassword(plaintext: String, hash: String): Boolean {
            return passwordHasher.check(plaintext, hash)
        }

        /** @{inheritDoc} */
        override suspend fun findLatestPasswordRecord(realm: String, owner: String): Stored<AuthRecord.Password>? {
            return authRecordStorage
                .findLatestRecordBy(type = AuthRecord.Password, realm = realm, owner = owner)
        }

        /** @{inheritDoc} */
        override suspend fun <T : AuthRecord> createAuthRecord(record: () -> T): Stored<T> {
            return authRecordStorage.create { record() }
        }

        /** @{inheritDoc} */
        override suspend fun findPasswordRecoveryToken(
            realm: String,
            token: String,
        ): Stored<AuthRecord.PasswordRecoveryToken>? {
            return authRecordStorage
                .findByToken(type = AuthRecord.PasswordRecoveryToken, realm = realm, token = token)
        }
    }

    /** The URLs used for deep-linking into the frontend application. */
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

    /** Lazily loaded dependencies required for authentication operations. */
    private val services: Services by services

    /** The provider id */
    override val id: String = ID

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
        // Validate request type
        val typed: AuthSignInRequest.EmailAndPassword = (request as? AuthSignInRequest.EmailAndPassword)
            ?: throw AuthError.invalidRequest()
        // Validate email
        val email = typed.email.takeIf { it.isEmail() }
            ?: throw AuthError.invalidCredentials()
        // Validate password
        val password = typed.password.takeIf { it.isNotBlank() }
            ?: throw AuthError.invalidCredentials()
        // Load user
        val user = realm.loadUserByEmail(email.trim().lowercase())
            ?: throw AuthError.invalidCredentials()
        // Validate password
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
        // Check request
        val typed = request as? AuthSignUpRequest.EmailAndPassword
            ?: throw AuthError.invalidRequest()
        // Create params for user creation
        val createParams = AuthRealm.CreateUserForSignupParams
            .of(email = typed.email, displayName = typed.displayName)
        // Validate email address
        if (createParams.email.isEmail().not()) throw AuthError.invalidRequest()
        // Enforce the password policy
        if (!realm.passwordPolicy.matches(typed.password)) throw AuthError.weakPassword()
        // Ensure no existing user
        if (realm.loadUserByEmail(createParams.email) != null) throw AuthError("User already exists")
        // Create user via realm hook
        val user = realm.createUserForSignup(createParams)
        // Store password record
        services.createAuthRecord {
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
        services.createAuthRecord {
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

        // TODO: make length configurable
        val token = services.generateRandomBase64Token(length = 256)

        // Store the token in the database
        services.createAuthRecord {
            AuthRecord.PasswordRecoveryToken(
                realm = realm.id,
                ownerId = user._id,
                token = token,
                // TODO: make expiration configurable
                expiresAt = services.instantNow().plus(1.hours).toEpochSeconds(),
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

        val tokenRecord = services
            .findPasswordRecoveryToken(realm = realm.id, token = request.token)

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

        val tokenRecord = services
            .findPasswordRecoveryToken(realm = realm.id, token = request.token)
            ?: return AuthRecoverAccountResponse.SetPasswordWithToken(success = false)

        // Write a new password entry into the auth records storage
        services.createAuthRecord {
            createPasswordRecord(realmId = realm.id, ownerId = tokenRecord.value.ownerId, password = request.password)
        }

        // Send email to the user to notify them that their password has been changed
        realm.loadUserById(tokenRecord.value.ownerId)?.let { user ->
            realm.messaging.sendPasswordChangedEmail(user)
        }

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
        val record = services
            .findLatestPasswordRecord(realm = realm.id, owner = user._id)
            ?: return false

        return services.checkPassword(plaintext = password, hash = record.value.token)
    }

    /**
     * Creates a new password record for the given [realmId], [ownerId] and [password].
     */
    private fun createPasswordRecord(realmId: String, ownerId: String, password: String): AuthRecord.Password {
        return AuthRecord.Password(
            realm = realmId,
            ownerId = ownerId,
            token = services.hashPassword(password),
        )
    }
}
