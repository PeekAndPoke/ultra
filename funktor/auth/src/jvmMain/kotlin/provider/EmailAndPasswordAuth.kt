package de.peekandpoke.funktor.auth.provider

import de.peekandpoke.funktor.auth.AuthError
import de.peekandpoke.funktor.auth.AuthRealm
import de.peekandpoke.funktor.auth.AuthSystem
import de.peekandpoke.funktor.auth.domain.AuthRecord
import de.peekandpoke.funktor.auth.model.AuthProviderModel
import de.peekandpoke.funktor.auth.model.AuthRecoveryRequest
import de.peekandpoke.funktor.auth.model.AuthRecoveryResponse
import de.peekandpoke.funktor.auth.model.AuthSignInRequest
import de.peekandpoke.funktor.auth.model.AuthSignUpRequest
import de.peekandpoke.funktor.auth.model.AuthUpdateRequest
import de.peekandpoke.funktor.auth.model.AuthUpdateResponse
import de.peekandpoke.ultra.log.Log
import de.peekandpoke.ultra.vault.Stored

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
            capabilities: Set<AuthProviderModel.Capability> = setOf(AuthProviderModel.Capability.SignIn),
        ) = EmailAndPasswordAuth(
            id = id,
            deps = deps,
            log = log,
            capabilities = capabilities,
        )
    }

    private val deps by deps

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
    override suspend fun <USER> update(
        realm: AuthRealm<USER>,
        user: Stored<USER>,
        request: AuthUpdateRequest,
    ): AuthUpdateResponse {

        @Suppress("REDUNDANT_ELSE_IN_WHEN")
        val result = when (request) {
            is AuthUpdateRequest.SetPassword -> {
                // 1. Check for new password to meet the password policy
                realm.passwordPolicy.matches(request.newPassword).takeIf { it }
                    ?: throw AuthError.weakPassword()

                // 3. Write new password entry into database
                deps.storage.createRecord {
                    AuthRecord.Password(
                        realm = realm.id,
                        ownerId = user._id,
                        hash = deps.passwordHasher.hash(request.newPassword),
                    )
                }

                val emailResult = realm.messaging.sendPasswordChangedEmail(user)

                if (emailResult.success.not()) {
                    log.warning("Sending 'Password Changed' Email failed for user ${user._id} ${realm.getUserEmail(user)}")
                }

                AuthUpdateResponse(success = true)
            }

            else -> AuthUpdateResponse(success = false)
        }

        return result
    }

    override suspend fun <USER> recover(
        realm: AuthRealm<USER>,
        request: AuthRecoveryRequest,
    ): AuthRecoveryResponse {

        @Suppress("REDUNDANT_ELSE_IN_WHEN")
        return when (request) {
            is AuthRecoveryRequest.ResetPassword -> {

                val user = realm.loadUserByEmail(request.email)
                    ?: return AuthRecoveryResponse.failed

                val token = deps.random.getToken()

                // TODO: write recovery token into db
                // TODO: send email with recovery token
                // TODO: build ui-endpoint to handle the token
                // TODO: add additional LoginRequest.WithOnetimeToken()
                // TODO: handle this one above in the login method

                AuthRecoveryResponse.success
            }

            else -> AuthRecoveryResponse.failed
        }
    }

    override suspend fun <USER> signUp(
        realm: AuthRealm<USER>,
        request: AuthSignUpRequest,
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
            email = email,
            displayName = typed.displayName?.trim(),
        )

        // Store password record
        deps.storage.createRecord {
            AuthRecord.Password(
                realm = realm.id,
                ownerId = user._id,
                hash = deps.passwordHasher.hash(typed.password),
            )
        }

        return AuthProvider.SignUpResult(
            user = user,
            requiresActivation = true,
        )
    }

    private suspend fun <USER> validateCurrentPassword(
        realm: AuthRealm<USER>,
        user: Stored<USER>,
        password: String,
    ): Boolean {
        val record = deps.storage.findLatestRecordBy(
            type = AuthRecord.Password,
            realm = realm.id,
            owner = user._id,
        ) ?: return false

        return deps.passwordHasher.check(plaintext = password, hash = record.value.hash)
    }

    override fun asApiModel(): AuthProviderModel {
        return AuthProviderModel(
            id = id,
            capabilities = capabilities,
            type = AuthProviderModel.TYPE_EMAIL_PASSWORD,
        )
    }
}
