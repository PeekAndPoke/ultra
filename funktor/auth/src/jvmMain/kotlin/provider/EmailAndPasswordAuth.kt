package io.peekandpoke.funktor.auth.provider

import io.peekandpoke.funktor.auth.AccountRetirement
import io.peekandpoke.funktor.auth.AuthError
import io.peekandpoke.funktor.auth.AuthFrontendRoutes
import io.peekandpoke.funktor.auth.AuthRandom
import io.peekandpoke.funktor.auth.AuthRealm
import io.peekandpoke.funktor.auth.AuthRecordStorage
import io.peekandpoke.funktor.auth.AuthSystem
import io.peekandpoke.funktor.auth.AuthUserAdapter
import io.peekandpoke.funktor.auth.domain.AuthRecord
import io.peekandpoke.funktor.auth.model.AuthActivateAccountRequest
import io.peekandpoke.funktor.auth.model.AuthActivateAccountResponse
import io.peekandpoke.funktor.auth.model.AuthCancelSignUpRequest
import io.peekandpoke.funktor.auth.model.AuthCancelSignUpResponse
import io.peekandpoke.funktor.auth.model.AuthProviderModel
import io.peekandpoke.funktor.auth.model.AuthRecoverAccountRequest
import io.peekandpoke.funktor.auth.model.AuthRecoverAccountResponse
import io.peekandpoke.funktor.auth.model.AuthResendActivationResponse
import io.peekandpoke.funktor.auth.model.AuthSetPasswordRequest
import io.peekandpoke.funktor.auth.model.AuthSetPasswordResponse
import io.peekandpoke.funktor.auth.model.AuthSignInRequest
import io.peekandpoke.funktor.auth.model.AuthSignUpRequest
import io.peekandpoke.funktor.auth.model.AuthUser
import io.peekandpoke.funktor.auth.model.RealmId
import io.peekandpoke.funktor.messaging.api.EmailResult
import io.peekandpoke.ultra.datetime.Kronos
import io.peekandpoke.ultra.datetime.MpInstant
import io.peekandpoke.ultra.log.Log
import io.peekandpoke.ultra.remote.buildUri
import io.peekandpoke.ultra.security.password.PasswordHasher
import io.peekandpoke.ultra.security.user.EmailAddress
import io.peekandpoke.ultra.security.user.UserId
import io.peekandpoke.ultra.vault.Stored
import io.peekandpoke.ultra.vault.value
import kotlinx.serialization.json.buildJsonObject

/**
 * Authentication provider for handling email and password-based authentication.
 *
 * This class provides methods for user login and user account updates, such as
 * changing passwords. It validates the user's credentials against the stored
 * records while adhering to the realm's password policy.
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
        suspend fun findLatestPasswordRecord(realm: RealmId, owner: UserId): Stored<AuthRecord.Password>?

        /** Find the password recovery token for the given [realm] and [token] */
        suspend fun findPasswordRecoveryToken(realm: RealmId, token: String): Stored<AuthRecord.PasswordRecoveryToken>?

        /** Find the email verification token for the given [realm] and [token] */
        suspend fun findEmailVerificationToken(
            realm: RealmId, token: String,
        ): Stored<AuthRecord.EmailVerificationToken>?

        /** Remove every email verification token for [realm] / [owner], except [exceptId] */
        suspend fun removeEmailVerificationTokens(realm: RealmId, owner: UserId, exceptId: String? = null)

        /** Find the pending-activation marker for [realm] / [owner], or null when activated */
        suspend fun findPendingActivation(realm: RealmId, owner: UserId): Stored<AuthRecord.PendingActivation>?

        /** Replace the pending-activation [marker], e.g. to advance its resend schedule */
        suspend fun savePendingActivation(
            marker: Stored<AuthRecord.PendingActivation>,
        ): Stored<AuthRecord.PendingActivation>

        /** Remove every pending-activation marker for [realm] / [owner] - i.e. activate the account */
        suspend fun removePendingActivations(realm: RealmId, owner: UserId)

        /** Remove an auth record by its [id]; true when THIS call removed it */
        suspend fun removeAuthRecord(id: String): Boolean
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
        override suspend fun findLatestPasswordRecord(realm: RealmId, owner: UserId): Stored<AuthRecord.Password>? {
            return authRecordStorage
                .findLatestRecordBy(type = AuthRecord.Password, realm = realm, owner = owner)
        }

        /** @{inheritDoc} */
        override suspend fun <T : AuthRecord> createAuthRecord(record: () -> T): Stored<T> {
            return authRecordStorage.create { record() }
        }

        /** @{inheritDoc} */
        override suspend fun findPasswordRecoveryToken(
            realm: RealmId,
            token: String,
        ): Stored<AuthRecord.PasswordRecoveryToken>? {
            return authRecordStorage
                .findByToken(type = AuthRecord.PasswordRecoveryToken, realm = realm, token = token)
        }

        /** @{inheritDoc} */
        override suspend fun findEmailVerificationToken(
            realm: RealmId,
            token: String,
        ): Stored<AuthRecord.EmailVerificationToken>? {
            return authRecordStorage
                .findByToken(type = AuthRecord.EmailVerificationToken, realm = realm, token = token)
        }

        /** @{inheritDoc} */
        override suspend fun removeEmailVerificationTokens(realm: RealmId, owner: UserId, exceptId: String?) {
            authRecordStorage.removeAllByOwner(
                type = AuthRecord.EmailVerificationToken, realm = realm, owner = owner, exceptId = exceptId,
            )
        }

        /** @{inheritDoc} */
        override suspend fun findPendingActivation(
            realm: RealmId,
            owner: UserId,
        ): Stored<AuthRecord.PendingActivation>? {
            return authRecordStorage
                .findLatestRecordBy(type = AuthRecord.PendingActivation, realm = realm, owner = owner)
        }

        /** @{inheritDoc} */
        override suspend fun savePendingActivation(
            marker: Stored<AuthRecord.PendingActivation>,
        ): Stored<AuthRecord.PendingActivation> {
            return authRecordStorage.save(marker)
        }

        /** @{inheritDoc} */
        override suspend fun removePendingActivations(realm: RealmId, owner: UserId) {
            authRecordStorage
                .removeAllByOwner(type = AuthRecord.PendingActivation, realm = realm, owner = owner)
        }

        /** @{inheritDoc} */
        override suspend fun removeAuthRecord(id: String): Boolean {
            return authRecordStorage.removeById(id).count == 1L
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
    override suspend fun <USER : AuthUser> signIn(realm: AuthRealm<USER>, request: AuthSignInRequest): Stored<USER> {
        // Validate request type
        val typed: AuthSignInRequest.EmailAndPassword = (request as? AuthSignInRequest.EmailAndPassword)
            ?: throw AuthError.invalidRequest()
        // Raw user input — parse at the boundary. This both VALIDATES and canonicalizes, so the
        // case-sensitive lookup below matches regardless of how the user typed it.
        val email = EmailAddress.parseOrNull(typed.email)
            ?: throw AuthError.invalidCredentials()
        // Validate password
        val password = typed.password.takeIf { it.isNotBlank() }
            ?: throw AuthError.invalidCredentials()
        // Load user
        val user = realm.users.loadByEmail(email)
            ?: throw AuthError.invalidCredentials()
        // Validate password
        validateCurrentPassword(realm, user, password).takeIf { it }
            ?: throw AuthError.invalidCredentials()

        // The account must have proven it owns the address. Keyed on the PENDING MARKER, never on
        // "an unexpired verification token exists": tokens expire and expired records are filtered
        // out of every lookup, so the token-based check would erase itself after 24 hours and quietly
        // let every lapsed sign-up in.
        if (services.findPendingActivation(realm = realm.id, owner = UserId(user._id)) != null) {
            throw AuthError.AccountNotActivated(userId = UserId(user._id))
        }

        return user
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun <USER : AuthUser> signUp(
        realm: AuthRealm<USER>, request: AuthSignUpRequest,
    ): AuthProvider.SignUpResult<USER> {
        // Check request
        val typed = request as? AuthSignUpRequest.EmailAndPassword
            ?: throw AuthError.invalidRequest()
        // Raw user input. This CREATES an account, so the format check applies here — and it runs
        // before `CreateUserForSignupParams` so an invalid address is a clean AuthError rather than an
        // IllegalArgumentException escaping as a 500.
        val email = EmailAddress.parseOrNull(typed.email)?.takeIf { it.isValidFormat }
            ?: throw AuthError.invalidRequest()
        // Create params for user creation
        val createParams = AuthUserAdapter.CreateUserForSignupParams
            .of(email = email, displayName = typed.displayName)
        // Enforce the password policy
        if (!realm.passwordPolicy.matches(typed.password)) throw AuthError.weakPassword()
        // Ensure no existing user. An EXPIRED pending account gives its address up right here, so expiry
        // does not depend on the sweep having run.
        realm.users.loadByEmail(createParams.email)?.let { existing ->
            if (!realm.retirePendingAccount(UserId(existing._id), expiredOnly = true)) {
                throw AuthError("User already exists")
            }
        }
        // Create user via realm hook
        // NOTE: The check above is a best-effort guard. A unique index on email in the user repository
        //       is required to prevent a TOCTOU race under concurrent sign-ups.
        val user = try {
            realm.users.createForSignup(createParams)
        } catch (e: Exception) {
            // Duplicate key exception from a concurrent sign-up race — treat as "already exists"
            throw AuthError("User already exists", cause = e)
        }
        // Store password record
        services.createAuthRecord {
            createPasswordRecord(realmId = realm.id, ownerId = UserId(user._id), password = typed.password)
        }

        // Mark the account as not-yet-activated BEFORE mailing anything. The marker is what blocks
        // sign-in, so writing it first means a crash between the two steps leaves an account that
        // cannot be used rather than one that is silently activated.
        // `createdAt` explicitly: the account's expiry, and with it the activation link's, counts from it.
        val marker = services.createAuthRecord {
            AuthRecord.PendingActivation(realm = realm.id, ownerId = UserId(user._id), createdAt = services.instantNow())
        }

        issueAndSendActivationToken(realm = realm, user = user, marker = marker.value)

        return AuthProvider.SignUpResult(
            user = user,
            requiresActivation = true,
        )
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun <USER : AuthUser> resendActivation(
        realm: AuthRealm<USER>, owner: UserId,
    ): AuthResendActivationResponse {

        val user = realm.users.loadById(owner)?.takeIf { it._id == owner.value }
            ?: return AuthResendActivationResponse(sent = false)

        // Only accounts that are actually waiting — an already-activated account must not be mailed.
        val marker = services.findPendingActivation(realm = realm.id, owner = owner)
            ?: return AuthResendActivationResponse(sent = false)

        val now = services.instantNow()

        // An expired account is retired, not re-mailed: expiry is what bounds the schedule below.
        if (marker.value.createdAt.plus(realm.tokenConfig.pendingAccountLifetime) <= now) {
            return AuthResendActivationResponse(sent = false)
        }

        // The growing delay, per account. It is what stops the account's own password holder — who may
        // have pre-registered someone else's address — from turning resend into a mail faucet.
        val dueAt = marker.value.nextResendAt(realm)

        if (dueAt > now) {
            return AuthResendActivationResponse(sent = false, nextResendAt = dueAt)
        }

        // Reserve the slot BEFORE sending, so a failing or slow mail cannot be retried at full speed.
        val advanced = services.savePendingActivation(
            marker.modify { it.copy(resends = it.resends + 1, lastSentAt = now) }
        )

        val result = issueAndSendActivationToken(realm = realm, user = user, marker = advanced.value)

        // Honest: a failed mail is `sent = false` — and WITHOUT `nextResendAt`, which a client reads as "too
        // soon, we already sent one". The slot stays reserved; a retry inside it is answered as too soon.
        return AuthResendActivationResponse(
            sent = result.success,
            nextResendAt = advanced.value.nextResendAt(realm).takeIf { result.success },
        )
    }

    /** When the next activation resend becomes possible for this marker's account. */
    private fun AuthRecord.PendingActivation.nextResendAt(realm: AuthRealm<*>): MpInstant =
        (lastSentAt ?: createdAt).plus(realm.tokenConfig.activationResendDelay(resends))

    /**
     * Issues a fresh [AuthRecord.EmailVerificationToken] for [user] and mails the deep-links.
     *
     * Shared by [signUp] and [resendActivation] so the token lifetime, the link shape and the failure
     * logging cannot drift apart between the two — a resend that built a different URL would be a
     * dead link that every server-side test still passes.
     *
     * Lives on the PROVIDER, not on `AuthRealm`, because the deep-link needs [frontendUrls], which is
     * provider configuration. Mirrors [recoverAccountInitPasswordReset].
     */
    private suspend fun <USER : AuthUser> issueAndSendActivationToken(
        realm: AuthRealm<USER>,
        user: Stored<USER>,
        marker: AuthRecord.PendingActivation,
    ): EmailResult {
        val token = services.generateRandomBase64Token(length = realm.tokenConfig.randomTokenByteLength)

        // Never outlives the pending account: nothing can activate after expiry, whether or not the
        // sweep has retired the account yet.
        val tokenExpiresAt = minOf(
            services.instantNow().plus(realm.tokenConfig.emailVerificationTokenLifetime),
            marker.createdAt.plus(realm.tokenConfig.pendingAccountLifetime),
        )

        val issued = services.createAuthRecord {
            AuthRecord.EmailVerificationToken(
                realm = realm.id,
                ownerId = UserId(user._id),
                token = token,
                expiresAt = tokenExpiresAt.toEpochSeconds(),
            )
        }

        // Rotate AFTER creating, excluding what was just created: only the newest link stays live, and
        // row growth stays bounded. Ordered this way because the reverse (delete, then create) lets two
        // interleaved resends delete the token the other has already mailed, so a user would receive a
        // link that was dead before it arrived.
        services.removeEmailVerificationTokens(realm = realm.id, owner = UserId(user._id), exceptId = issued._id)

        val emailResult = realm.messaging.sendAccountActivationEmail(
            user = user,
            activationUrl = buildUri(frontendUrls.routes.activateAccount.pattern) {
                set(AuthFrontendRoutes.PROVIDER_PARAM, id)
                set(AuthFrontendRoutes.TOKEN_PARAM, token)
            },
            cancelUrl = buildUri(frontendUrls.routes.cancelSignUp.pattern) {
                set(AuthFrontendRoutes.PROVIDER_PARAM, id)
                set(AuthFrontendRoutes.TOKEN_PARAM, token)
            },
        )

        if (emailResult.success.not()) {
            // The account stays blocked. The ways back are a resend and a password reset, which proves
            // the same mailbox and clears the marker - see [recoverAccountSetPasswordWithToken].
            log.warning(
                "Sending 'Account Activation' Email failed for user ${user._id} ${user.value.email}"
            )
        }

        return emailResult
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun <USER : AuthUser> activateAccount(
        realm: AuthRealm<USER>, request: AuthActivateAccountRequest,
    ): AuthActivateAccountResponse {

        val tokenRecord = services
            .findEmailVerificationToken(realm = realm.id, token = request.token)
            ?: return AuthActivateAccountResponse(success = false)

        // Dropping the marker IS the activation, and it goes FIRST. The two writes are not atomic, so
        // the order decides how a failure between them lands:
        //   marker first  -> if the delete fails, the account is activated and the token stays live
        //                    for its remaining lifetime. Replaying it activates an already-activated
        //                    account, which is a no-op.
        //   token first   -> if the marker drop fails, the link is dead AND the account is still
        //                    blocked, until the user signs in again and asks for a resend.
        services.removePendingActivations(realm = realm.id, owner = tokenRecord.value.ownerId)

        // Invalidate the token so it cannot be reused
        services.removeAuthRecord(tokenRecord._id)

        return AuthActivateAccountResponse(success = true)
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun <USER : AuthUser> cancelSignUp(
        realm: AuthRealm<USER>, request: AuthCancelSignUpRequest,
    ): AuthCancelSignUpResponse {

        val tokenRecord = services
            .findEmailVerificationToken(realm = realm.id, token = request.token)
            ?: return AuthCancelSignUpResponse(success = false)

        // Single-use, by the delete count: of two concurrent cancels only one goes on.
        if (!services.removeAuthRecord(tokenRecord._id)) {
            return AuthCancelSignUpResponse(success = false)
        }

        // Refuses, changing nothing, unless the account is still pending.
        return AuthCancelSignUpResponse(
            success = realm.retirePendingAccount(tokenRecord.value.ownerId),
        )
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun <USER : AuthUser> setPassword(
        realm: AuthRealm<USER>, request: AuthSetPasswordRequest,
    ): AuthSetPasswordResponse {

        // 1. Check for new password to meet the password policy
        realm.passwordPolicy.matches(request.newPassword).takeIf { it }
            ?: throw AuthError.weakPassword()

        val user = realm.users.loadById(request.userId)
            ?: throw AuthError.userNotFound(request.userId.value)

        // 2. Verify the current password before allowing a change
        validateCurrentPassword(realm, user, request.currentPassword).takeIf { it }
            ?: throw AuthError.invalidCredentials()

        // 3. Write new password entry into database
        services.createAuthRecord {
            createPasswordRecord(realmId = realm.id, ownerId = UserId(user._id), password = request.newPassword)
        }

        val emailResult = realm.messaging.sendPasswordChangedEmail(user)

        if (emailResult.success.not()) {
            log.warning(
                "Sending 'Password Changed' Email failed for user ${user._id} ${user.value.email}"
            )
        }

        return AuthSetPasswordResponse(success = true)
    }

    /**
     * {@inheritDoc}
     */
    override suspend fun <USER : AuthUser> recoverAccountInitPasswordReset(
        realm: AuthRealm<USER>, request: AuthRecoverAccountRequest.InitPasswordReset,
    ): AuthRecoverAccountResponse.InitPasswordReset {

        // GAP CLOSED: this used the RAW request email, so a mixed-case reset request silently found
        // no user and sent no email. An unparseable address returns the SAME neutral response as an
        // unknown one — no account enumeration.
        // A retired account is answered like an unknown one: its address cannot receive mail, and a
        // reset would quietly bring it back to life.
        val user = EmailAddress.parseOrNull(request.email)
            ?.takeUnless { AccountRetirement.isRetired(it) }
            ?.let { realm.users.loadByEmail(it) }
            ?: return AuthRecoverAccountResponse.InitPasswordReset

        val token = services.generateRandomBase64Token(length = realm.tokenConfig.randomTokenByteLength)

        // Store the token in the database
        services.createAuthRecord {
            AuthRecord.PasswordRecoveryToken(
                realm = realm.id,
                ownerId = UserId(user._id),
                token = token,
                expiresAt = services.instantNow()
                    .plus(realm.tokenConfig.passwordRecoveryTokenLifetime).toEpochSeconds(),
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
                "Sending 'Password Recovery' Email failed for user ${user._id} ${user.value.email}"
            )
        }

        return AuthRecoverAccountResponse.InitPasswordReset
    }

    /**
     * Validate token for password reset
     */
    override suspend fun <USER : AuthUser> recoverAccountValidatePasswordResetToken(
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
    override suspend fun <USER : AuthUser> recoverAccountSetPasswordWithToken(
        realm: AuthRealm<USER>, request: AuthRecoverAccountRequest.SetPasswordWithToken,
    ): AuthRecoverAccountResponse.SetPasswordWithToken {

        val tokenRecord = services
            .findPasswordRecoveryToken(realm = realm.id, token = request.token)
            ?: return AuthRecoverAccountResponse.SetPasswordWithToken(success = false)

        // Invalidate the token immediately so it cannot be reused
        services.removeAuthRecord(tokenRecord._id)

        // Write a new password entry into the auth records storage
        services.createAuthRecord {
            createPasswordRecord(realmId = realm.id, ownerId = tokenRecord.value.ownerId, password = request.password)
        }

        // Following a link mailed to the address proves what the activation link proves, AND the new
        // password supersedes the sign-up one, so a completed reset also activates. It is the way back
        // for a pending owner who does not know the sign-up password.
        services.removePendingActivations(realm = realm.id, owner = tokenRecord.value.ownerId)

        // Send email to the user to notify them that their password has been changed
        realm.users.loadById(tokenRecord.resolve().ownerId)?.let { user ->
            realm.messaging.sendPasswordChangedEmail(user)
        }

        return AuthRecoverAccountResponse.SetPasswordWithToken(
            success = true,
        )
    }

    /**
     * Validates the given [password] against the latest password for the given [user] in the [realm].
     */
    private suspend fun <USER : AuthUser> validateCurrentPassword(
        realm: AuthRealm<USER>, user: Stored<USER>, password: String,
    ): Boolean {
        val record = services
            .findLatestPasswordRecord(realm = realm.id, owner = UserId(user._id))
            ?: return false

        return services.checkPassword(plaintext = password, hash = record.resolve().token)
    }

    /**
     * Creates a new password record for the given [realmId], [ownerId] and [password].
     */
    private fun createPasswordRecord(realmId: RealmId, ownerId: UserId, password: String): AuthRecord.Password {
        return AuthRecord.Password(
            realm = realmId,
            ownerId = ownerId,
            token = services.hashPassword(password),
        )
    }
}
