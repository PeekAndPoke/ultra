package de.peekandpoke.funktor.auth

import de.peekandpoke.funktor.auth.model.AuthProviderModel.Capability
import de.peekandpoke.funktor.auth.model.AuthRealmModel
import de.peekandpoke.funktor.auth.model.AuthRecoverAccountRequest
import de.peekandpoke.funktor.auth.model.AuthRecoverAccountResponse
import de.peekandpoke.funktor.auth.model.AuthSetPasswordRequest
import de.peekandpoke.funktor.auth.model.AuthSetPasswordResponse
import de.peekandpoke.funktor.auth.model.AuthSignInRequest
import de.peekandpoke.funktor.auth.model.AuthSignInResponse
import de.peekandpoke.funktor.auth.model.AuthSignUpRequest
import de.peekandpoke.funktor.auth.model.AuthSignUpResponse
import de.peekandpoke.funktor.auth.model.PasswordPolicy
import de.peekandpoke.funktor.auth.provider.AuthProvider
import de.peekandpoke.funktor.auth.provider.hasCapability
import de.peekandpoke.funktor.auth.provider.supportsSignIn
import de.peekandpoke.funktor.messaging.Email
import de.peekandpoke.funktor.messaging.api.EmailBody
import de.peekandpoke.funktor.messaging.api.EmailDestination
import de.peekandpoke.funktor.messaging.api.EmailResult
import de.peekandpoke.funktor.messaging.storage.EmailStoring
import de.peekandpoke.funktor.messaging.storage.EmailStoring.Companion.store
import de.peekandpoke.ultra.vault.Stored
import kotlinx.html.a
import kotlinx.html.body
import kotlinx.html.br
import kotlinx.html.h1
import kotlinx.html.p
import kotlinx.serialization.json.JsonObject

interface AuthRealm<USER> {

    interface Messaging<USER> {
        suspend fun sendPasswordChangedEmail(user: Stored<USER>): EmailResult

        suspend fun sendPasswordRecoveryEmil(user: Stored<USER>, resetUrl: String): EmailResult
    }

    class DefaultMessaging<USER>(
        val senderEmail: String,
        val senderName: String,
        val applicationName: String,
        val realm: AuthRealm<USER>,
    ) : Messaging<USER> {

        override suspend fun sendPasswordChangedEmail(user: Stored<USER>): EmailResult {
            val userEmail = realm.getUserEmail(user)

            return realm.deps.messaging.mailing.send(
                Email(
                    source = senderEmail,
                    destination = EmailDestination.to(userEmail),
                    subject = "$applicationName: Your password was changed",
                    body = EmailBody.Html {
                        body {
                            h1 { +"Heads up!" }

                            p {
                                +"Your password was changed. If this was not you, please contact us!"
                            }

                            p {
                                +"Yours sincerely,"
                                br()
                                +senderName
                            }
                        }
                    }
                ).store(
                    EmailStoring.withAnonymizedContent(
                        refs = setOf(user._id, userEmail),
                        tags = setOf("password-changed"),
                    )
                )
            )
        }

        override suspend fun sendPasswordRecoveryEmil(user: Stored<USER>, resetUrl: String): EmailResult {
            val userEmail = realm.getUserEmail(user)

            return realm.deps.messaging.mailing.send(
                Email(
                    source = senderEmail,
                    destination = EmailDestination.to(userEmail),
                    subject = "$applicationName: Recover your Account",
                    body = EmailBody.Html {
                        body {
                            h1 { +"Heads up!" }

                            p {
                                +"Click the link below to recover your account and set a new password."
                            }

                            p {
                                a(href = resetUrl) {
                                    +"Recover account"
                                }
                            }

                            p {
                                +"Yours sincerely,"
                                br()
                                +senderName
                            }
                        }
                    }
                ).store(
                    EmailStoring.withAnonymizedContent(
                        refs = setOf(user._id, userEmail),
                        tags = setOf("password-reset"),
                    )
                )
            )
        }
    }

    /**
     * Parameters for creating a new user during sign-up, see [createUserForSignup]
     */
    @ConsistentCopyVisibility
    data class CreateUserForSignupParams private constructor(
        val email: String,
        val displayName: String,
    ) {
        companion object {
            fun of(email: String, displayName: String? = null) = CreateUserForSignupParams(
                email = email.trim().lowercase(),
                displayName = displayName?.trim() ?: email.substringBefore("@").trim(),
            )
        }
    }

    /** Unique id of the realm */
    val id: String

    /** Auth providers for this realm */
    val providers: List<AuthProvider>

    /** AuthSystem dependencies */
    val deps: AuthSystem.Deps

    /** User messaging */
    val messaging: Messaging<USER>

    /** The password policy for this realm */
    val passwordPolicy: PasswordPolicy get() = PasswordPolicy.default

    /** Loads a user by its id. */
    suspend fun loadUserById(id: String): Stored<USER>?

    /** Loads a user by its email. */
    suspend fun loadUserByEmail(email: String): Stored<USER>?

    /** Generates a JWT for the given user */
    suspend fun generateJwt(user: Stored<USER>): AuthSignInResponse.Token

    /** Loads the user email from the given user */
    suspend fun getUserEmail(user: Stored<USER>): String

    /** Serializes the given user */
    suspend fun serializeUser(user: Stored<USER>): JsonObject

    /** Creates a new user during sign-up. Providers call this to create a user for the given email/displayName. */
    suspend fun createUserForSignup(params: CreateUserForSignupParams): Stored<USER>

    /**
     * Signs in a user. Providers should check their SignIn capability internally.
     */
    suspend fun signIn(request: AuthSignInRequest): AuthSignInResponse {
        val provider = getProvider(request.provider).supporting(Capability.SignIn)

        if (provider.supportsSignIn().not()) {
            throw AuthError.providerDoesNotSupportAction(
                provider = request.provider,
                action = Capability.SignIn.name,
            )
        }

        val user = provider.signIn<USER>(realm = this, request = request)

        return user.toSignInResponse()
    }

    /**
     * Signs up a new user. Providers should check their SignUp capability internally.
     */
    suspend fun signUp(request: AuthSignUpRequest): AuthSignUpResponse {
        val provider = getProvider(request.provider).supporting(Capability.SignUp)

        val result = provider.signUp(realm = this, request = request)

        if (result.requiresActivation) {
            // TODO: send account activation email
        }

        val response = AuthSignUpResponse(
            signIn = result.user.toSignInResponse(),
            requiresActivation = result.requiresActivation,
        )

        return response
    }

    /**
     * Sets a new password for the given user.
     */
    suspend fun setPassword(request: AuthSetPasswordRequest): AuthSetPasswordResponse {
        return getProvider(request.provider)
            .setPassword(realm = this, request = request)
    }

    /**
     * Initiates a password reset.
     */
    suspend fun recoverAccountInitPasswordReset(
        request: AuthRecoverAccountRequest.InitPasswordReset,
    ): AuthRecoverAccountResponse.InitPasswordReset {
        return getProvider(request.provider)
            .recoverAccountInitPasswordReset(realm = this, request = request)
    }

    /**
     * Validates the password reset token.
     */
    suspend fun recoverAccountValidatePasswordResetToken(
        request: AuthRecoverAccountRequest.ValidatePasswordResetToken,
    ): AuthRecoverAccountResponse.ValidatePasswordResetToken {
        return getProvider(request.provider)
            .recoverAccountValidatePasswordResetToken(realm = this, request = request)
    }

    /**
     * Resets the password with the given token.
     */
    suspend fun recoverAccountSetPasswordWithToken(
        request: AuthRecoverAccountRequest.SetPasswordWithToken,
    ): AuthRecoverAccountResponse.SetPasswordWithToken {
        return getProvider(request.provider)
            .recoverAccountSetPasswordWithToken(realm = this, request = request)
    }

    /**
     * Converts a user to a sign in response.
     */
    suspend fun Stored<USER>.toSignInResponse() = AuthSignInResponse(
        token = generateJwt(this),
        realm = asApiModel(),
        user = serializeUser(this),
    )

    /**
     * Converts the realm to an api model.
     */
    fun asApiModel(): AuthRealmModel = AuthRealmModel(
        id = id,
        providers = providers.map { it.asApiModel() },
        passwordPolicy = passwordPolicy,
    )

    /**
     * Gets a provider by its id or throws an [AuthError] if not found.
     */
    fun getProvider(id: String): AuthProvider {
        return providers.firstOrNull { it.id == id }
            ?: throw AuthError.providerNotFound(provider = id)
    }

    /**
     * Checks if the provider has the given [capability] or throws an [AuthError] if capability is not supported.
     */
    fun AuthProvider.supporting(capability: Capability): AuthProvider {
        return takeIf { hasCapability(capability) }
            ?: throw AuthError.providerDoesNotSupportAction(
                provider = id,
                action = capability.name,
            )
    }
}
