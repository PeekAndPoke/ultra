package io.peekandpoke.funktor.auth

import io.peekandpoke.funktor.auth.domain.AuthRecord
import io.peekandpoke.funktor.auth.emails.AccountActivationEmailTemplate
import io.peekandpoke.funktor.auth.emails.AuthEmailTemplates
import io.peekandpoke.funktor.auth.emails.PasswordChangedEmailTemplate
import io.peekandpoke.funktor.auth.emails.PasswordRecoveryEmailTemplate
import io.peekandpoke.funktor.auth.emails.authEmailLocales
import io.peekandpoke.funktor.auth.emails.requireAuthEmailEnvelope
import io.peekandpoke.funktor.auth.model.AuthActivateAccountRequest
import io.peekandpoke.funktor.auth.model.AuthActivateAccountResponse
import io.peekandpoke.funktor.auth.model.AuthOrgRef
import io.peekandpoke.funktor.auth.model.AuthProviderModel.Capability
import io.peekandpoke.funktor.auth.model.AuthRealmModel
import io.peekandpoke.funktor.auth.model.AuthRecoverAccountRequest
import io.peekandpoke.funktor.auth.model.AuthRecoverAccountResponse
import io.peekandpoke.funktor.auth.model.AuthResendActivationRequest
import io.peekandpoke.funktor.auth.model.AuthResendActivationResponse
import io.peekandpoke.funktor.auth.model.AuthSetPasswordRequest
import io.peekandpoke.funktor.auth.model.AuthSetPasswordResponse
import io.peekandpoke.funktor.auth.model.AuthSignInRequest
import io.peekandpoke.funktor.auth.model.AuthSignInResponse
import io.peekandpoke.funktor.auth.model.AuthSignUpRequest
import io.peekandpoke.funktor.auth.model.AuthSignUpResponse
import io.peekandpoke.funktor.auth.model.AuthUser
import io.peekandpoke.funktor.auth.model.LanguageSettings
import io.peekandpoke.funktor.auth.model.PasswordPolicy
import io.peekandpoke.funktor.auth.model.RealmId
import io.peekandpoke.funktor.auth.provider.AuthProvider
import io.peekandpoke.funktor.auth.provider.hasCapability
import io.peekandpoke.funktor.auth.provider.supportsSignIn
import io.peekandpoke.funktor.messaging.Email
import io.peekandpoke.funktor.messaging.api.EmailResult
import io.peekandpoke.funktor.messaging.storage.EmailStoring
import io.peekandpoke.funktor.messaging.storage.EmailStoring.Companion.store
import io.peekandpoke.ultra.datetime.MpInstant
import io.peekandpoke.ultra.i18n.Locale
import io.peekandpoke.ultra.security.jwt.JwtPayload
import io.peekandpoke.ultra.security.jwt.JwtVerificationException
import io.peekandpoke.ultra.security.user.EmailAddress
import io.peekandpoke.ultra.security.user.KnownRole
import io.peekandpoke.ultra.security.user.OrgId
import io.peekandpoke.ultra.security.user.OrgMembership
import io.peekandpoke.ultra.security.user.SelectedOrg
import io.peekandpoke.ultra.security.user.UserId
import io.peekandpoke.ultra.security.user.UserRecord
import io.peekandpoke.ultra.vault.Stored

/**
 * Defines an auth realm
 */
interface AuthRealm<USER : AuthUser> {

    /**
     * Messaging interface for sending emails.
     */
    interface Messaging<USER : AuthUser> {
        suspend fun sendPasswordChangedEmail(user: Stored<USER>): EmailResult

        suspend fun sendPasswordRecoveryEmil(user: Stored<USER>, resetUrl: String): EmailResult

        /** Sent at sign-up. Until the link is followed the account cannot sign in. */
        suspend fun sendAccountActivationEmail(user: Stored<USER>, activationUrl: String): EmailResult
    }

    /**
     * Default implementation of the messaging interface.
     */
    class DefaultMessaging<USER : AuthUser>(
        val senderEmail: String,
        val senderName: String,
        val applicationName: String,
        val realm: AuthRealm<USER>,
        /**
         * The renderings behind the three auth mails. An app overrides one by supplying its own
         * subclass of that template; see [AuthEmailTemplates].
         */
        val templates: AuthEmailTemplates = AuthEmailTemplates(),
    ) : Messaging<USER> {

        /**
         * The locale preference for a mail to [user] — see [authEmailLocales] for the rule.
         *
         * `internal` rather than private, and taking the user VALUE rather than a `Stored`, so a test
         * can assert the one thing the free function cannot: that this reads [AuthRealm.defaultLanguage]
         * instead of a hardcoded `en`.
         */
        internal fun preferredLocales(user: USER): List<Locale> =
            authEmailLocales(user = user, realmDefault = realm.defaultLanguage)

        /**
         * Renders through [build], pins the envelope, applies the storing policy, and sends.
         *
         * **The `try` is the point.** Everything below `mailing.send` reports failure by RETURNING a
         * failed `EmailResult` — the senders catch `Throwable` themselves — and callers are written
         * against that: `EmailAndPasswordAuth` logs a warning and leaves the account recoverable via
         * resend or password reset. Rendering is now app-overridable code running OUTSIDE that
         * protocol, and `AuthApi` catches only `AuthError`, so an app template that throws would
         * instead 500 the request. On sign-up that happens after the account, the password record and
         * the `PendingActivation` marker are already written; on resend it happens after the previous
         * activation link has been revoked — a deterministic throw there is an unrecoverable lockout.
         * It would also hand `recoverAccountInitPasswordReset` an account-enumeration oracle, since
         * that endpoint answers a neutral 200 for unknown addresses but only reaches this code for
         * ones that exist.
         */
        private suspend fun sendAuthEmail(
            user: Stored<USER>,
            tag: String,
            build: suspend (recipient: EmailAddress) -> Email,
        ): EmailResult {
            val userEmail = user.value().email

            val email = try {
                val built = build(userEmail)

                // Inside the try on purpose, so a violation degrades to a failed EmailResult
                // rather than a 500. See requireAuthEmailEnvelope for why it is re-asserted at all.
                requireAuthEmailEnvelope(built, userEmail)

                built
            } catch (e: Throwable) {
                realm.deps.log.error("Rendering the '$tag' email failed for user ${user._id}", e)

                return EmailResult.ofError(e)
            }

            return realm.deps.messaging.mailing.send(
                email.store(
                    EmailStoring.withAnonymizedContent(
                        refs = setOf(user._id, userEmail.value),
                        tags = setOf(tag),
                    )
                )
            )
        }

        override suspend fun sendPasswordChangedEmail(user: Stored<USER>): EmailResult =
            sendAuthEmail(user, tag = "password-changed") { recipient ->
                templates.passwordChanged.render(
                    params = PasswordChangedEmailTemplate.Params(
                        recipient = recipient,
                        senderEmail = senderEmail,
                        senderName = senderName,
                        applicationName = applicationName,
                    ),
                    preferred = preferredLocales(user.value()),
                )
            }

        override suspend fun sendAccountActivationEmail(user: Stored<USER>, activationUrl: String): EmailResult =
            sendAuthEmail(user, tag = "account-activation") { recipient ->
                templates.accountActivation.render(
                    params = AccountActivationEmailTemplate.Params(
                        recipient = recipient,
                        senderEmail = senderEmail,
                        senderName = senderName,
                        applicationName = applicationName,
                        activationUrl = activationUrl,
                    ),
                    preferred = preferredLocales(user.value()),
                )
            }

        override suspend fun sendPasswordRecoveryEmil(user: Stored<USER>, resetUrl: String): EmailResult =
            sendAuthEmail(user, tag = "password-reset") { recipient ->
                templates.passwordRecovery.render(
                    params = PasswordRecoveryEmailTemplate.Params(
                        recipient = recipient,
                        senderEmail = senderEmail,
                        senderName = senderName,
                        applicationName = applicationName,
                        resetUrl = resetUrl,
                    ),
                    preferred = preferredLocales(user.value()),
                )
            }
    }

    /** Unique id of the realm */
    val id: RealmId

    /** Auth providers for this realm */
    val providers: List<AuthProvider>

    /** AuthSystem dependencies */
    val deps: AuthSystem.Deps

    /** User messaging */
    val messaging: Messaging<USER>

    /**
     * The language mails are rendered in for a user who has expressed no preference of their own
     * ([LanguageSettings.messaging] is `null`).
     *
     * Sits BETWEEN the user's setting and the hardcoded `en` fallback, so a German-only deployment can
     * mail German to accounts that predate any language picker, without every realm having to
     * translate every template the framework might add later.
     */
    val defaultLanguage: Locale get() = Locale("en")

    /** Operations on this realm's users (loading, creating, serializing), see [AuthUserAdapter]. */
    val users: AuthUserAdapter<USER>

    /** The password policy for this realm */
    val passwordPolicy: PasswordPolicy get() = PasswordPolicy.default

    /** Per-realm token/session lifetimes. */
    val tokenConfig: RealmTokenConfig get() = RealmTokenConfig()

    /** How this realm relates to organisations. Default: [OrgPolicy.None] (org-less realm). */
    val orgPolicy: OrgPolicy get() = OrgPolicy.None

    /**
     * Returns the user's organisation memberships — the session-level [OrgMembership] value objects
     * fed into the JWT.
     *
     * Default: none. An org-less realm (e.g. operators) needs no override; org realms override this
     * to source memberships, e.g. from the saas `OrgMember` collection via
     * `OrgMembersStorage.sessionMembershipsOf(user._id)`.
     */
    suspend fun getMemberships(user: Stored<USER>): Set<OrgMembership> = emptySet()

    /**
     * Resolves the organisations a user may sign into (active only), for the login org-picker.
     *
     * Default: none. Org realms override this (typically backed by the `saas` OrgsStorage) to map
     * the user's [memberships] into [AuthOrgRef]s.
     */
    suspend fun getAccessibleOrgs(memberships: Set<OrgMembership>): List<AuthOrgRef> = emptyList()

    /**
     * Resolves a chosen [orgId] into the permission inputs for the session, or `null` if the user
     * may not select it. Default: none. Org realms override this to load the org's plan permissions.
     */
    suspend fun resolveSelectedOrg(orgId: OrgId, memberships: Set<OrgMembership>): SelectedOrg? = null

    /**
     * Generates a JWT for the given user and the org selected for this session (null for org-less realms).
     *
     * Returns the token only. Permissions and expiry are NOT returned alongside it: [successFor] reads
     * them back out of this token, so what the response says and what the token carries cannot drift.
     */
    suspend fun generateJwt(user: Stored<USER>, selectedOrg: SelectedOrg?): String

    /**
     * The application-specific roles this realm declares, for the API access matrix in funktor:inspect.
     *
     * EMPTY by default, and deliberately so: SuperUser and Anonymous exist in every application and are
     * appended once by the tooling ([KnownRole.universal]). A realm that restated them would put a
     * duplicate in the matrix for every realm registered.
     */
    fun getKnownRoles(): List<KnownRole> = emptyList()

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

        val user = try {
            provider.signIn<USER>(realm = this, request = request)
        } catch (e: AuthError.AccountNotActivated) {
            // NOT a failure — the credential check passed. Answering with a response case rather than
            // an error is what lets a client offer "resend the activation email" instead of showing a
            // generic "login failed". Caught by TYPE: matching on the message would break on the first
            // rewording or translation.
            //
            // The resend token is minted HERE, on the only path that has proven the password, and is
            // what authorizes `resendActivation`. Exactly the OrgSelectionToken pattern below.
            val resendToken = deps.random.getTokenAsBase64(tokenConfig.randomTokenByteLength)

            deps.storage.authRecords.create {
                AuthRecord.ActivationResendToken(
                    realm = id,
                    ownerId = e.userId,
                    token = resendToken,
                    expiresAt = deps.kronos.instantNow()
                        .plus(tokenConfig.activationResendTokenLifetime).toEpochSeconds(),
                )
            }

            return AuthSignInResponse.ActivationRequired(
                realm = asApiModel(),
                resendToken = resendToken,
            )
        }

        return issueSignIn(user)
    }

    /**
     * Signs up a new user. Providers should check their SignUp capability internally.
     */
    suspend fun signUp(request: AuthSignUpRequest): AuthSignUpResponse {
        val provider = getProvider(request.provider).supporting(Capability.SignUp)

        val result = provider.signUp(realm = this, request = request)

        // An account that has not proved it owns its address gets NO session. The provider has already
        // issued the activation token and mailed the link — the realm's whole share of activation is
        // withholding the sign-in, because the deep-link URLs are provider configuration.
        if (result.requiresActivation) {
            return AuthSignUpResponse(signIn = null, requiresActivation = true)
        }

        // Best-effort auto sign-in. For org realms the new user may have no org yet (e.g. invite-only),
        // in which case issueSignIn throws noOrganisationAccess — the account exists but can't sign in yet.
        val signInResponse = try {
            issueSignIn(result.user)
        } catch (e: AuthError) {
            deps.log.error("Failed to auto-sign-in after sign-up", e)
            null
        }

        return AuthSignUpResponse(
            signIn = signInResponse,
            requiresActivation = result.requiresActivation,
        )
    }

    /**
     * Activates an account with the token from the activation mail.
     *
     * Deliberately NOT gated on [Capability.SignUp]. Redeeming a token that was already issued is not
     * signing up, and gating it would mean that closing public registration retroactively voids every
     * outstanding activation link — locking out accounts that were created legitimately. A provider
     * that does not issue these tokens refuses on its own: [AuthProvider.activateAccount] defaults to
     * `notSupported()`.
     */
    suspend fun activate(request: AuthActivateAccountRequest): AuthActivateAccountResponse {
        return getProvider(request.provider).activateAccount(realm = this, request = request)
    }

    /**
     * Sends a fresh activation mail, authorized by the single-use token from
     * [AuthSignInResponse.ActivationRequired].
     *
     * Ungated on capabilities, like [activate], but for its own reason: the token was already issued
     * to someone who proved the password, and stranding those users when registration closes helps
     * nobody. The token — not the capability — is what stops this being an open mail relay.
     */
    suspend fun resendActivation(request: AuthResendActivationRequest): AuthResendActivationResponse {
        return getProvider(request.provider).resendActivation(realm = this, request = request)
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
     * Refreshes the token for the user with the given [userId].
     * Loads the user, generates a new JWT, and returns a full sign-in response.
     *
     * [expectedUserType] is validated against the newly generated token to prevent
     * cross-realm token refresh attacks (a user from realm A requesting a token from realm B).
     */
    suspend fun refreshToken(userId: UserId, expectedUserType: String?, currentOrgId: OrgId?): AuthSignInResponse {
        val user = users.loadById(userId)
            ?: throw AuthError("User not found: $userId")

        // An [OrgPolicy.Required] realm has NO valid org-less session: `signIn` and `selectOrg` can
        // never produce one. So a refresh that arrives without a usable org id must force a re-login
        // rather than mint a fresh org-less token — otherwise the caller silently ends up with a
        // valid 1h session in which every org-scoped route 404s, refreshable indefinitely and never
        // self-healing. This is reachable whenever the incoming `org` claim does not parse (e.g. a
        // token minted before the org id became a collection-qualified `_id`).
        if (orgPolicy is OrgPolicy.Required && currentOrgId == null) {
            throw AuthError.noOrganisationAccess()
        }

        // Re-derive the session's org slice from the DB (picks up membership/plan changes). Refresh
        // keeps the SAME org — it never changes which org is active.
        val memberships = if (currentOrgId != null) getMemberships(user) else emptySet()
        val selected = currentOrgId?.let {
            resolveSelectedOrg(it, memberships) ?: throw AuthError.noOrganisationAccess()
        }
        val org = currentOrgId?.let { oid -> getAccessibleOrgs(memberships).firstOrNull { it.id == oid } }

        val (response, payload) = successWithPayload(user, selected, org)

        // Validate that the refreshed token's user type matches the original JWT's user type.
        // This prevents cross-realm escalation when realms share a user store with overlapping IDs.
        if (expectedUserType != null) {
            val newToken = deps.jwtGenerator.extractUserData(payload)

            if (newToken.type != expectedUserType) {
                throw AuthError("Token refresh denied")
            }
        }

        return response
    }

    /**
     * Resolves the org-aware sign-in response for a just-authenticated [user] (the 0/1/n flow).
     *
     * - [OrgPolicy.None] → immediate success (org-less session).
     * - [OrgPolicy.Required]: 0 accessible orgs → [AuthError.noOrganisationAccess]; 1 → auto-select;
     *   many → [AuthSignInResponse.OrgSelectionRequired] backed by a single-use selection token.
     */
    suspend fun issueSignIn(user: Stored<USER>): AuthSignInResponse {
        return when (orgPolicy) {
            is OrgPolicy.None -> successFor(user, selectedOrg = null, org = null)

            is OrgPolicy.Required -> {
                val memberships = getMemberships(user)
                val orgs = getAccessibleOrgs(memberships)

                when (orgs.size) {
                    0 -> throw AuthError.noOrganisationAccess()

                    1 -> {
                        val ref = orgs.single()
                        val selected = resolveSelectedOrg(ref.id, memberships)
                            ?: throw AuthError.noOrganisationAccess()
                        successFor(user, selected, ref)
                    }

                    else -> {
                        val token = deps.random.getTokenAsBase64(tokenConfig.randomTokenByteLength)

                        deps.storage.authRecords.create {
                            AuthRecord.OrgSelectionToken(
                                realm = id,
                                ownerId = UserId(user._id),
                                token = token,
                                expiresAt = deps.kronos.instantNow()
                                    .plus(tokenConfig.orgSelectionTokenLifetime).toEpochSeconds(),
                            )
                        }

                        AuthSignInResponse.OrgSelectionRequired(
                            realm = asApiModel(),
                            selectionToken = token,
                            organisations = orgs,
                        )
                    }
                }
            }
        }
    }

    /**
     * Completes an org-selection sign-in: validates + consumes the single-use [selectionToken] and
     * issues a session for the chosen [orgId].
     */
    suspend fun selectOrg(selectionToken: String, orgId: OrgId): AuthSignInResponse {
        val record = deps.storage.authRecords
            .findByToken(AuthRecord.OrgSelectionToken, realm = id, token = selectionToken)
            ?: throw AuthError.noOrganisationAccess()

        // Single-use: consume the token.
        deps.storage.authRecords.removeById(record._id)

        val user = users.loadById(record.value().ownerId)
            ?: throw AuthError.noOrganisationAccess()

        val memberships = getMemberships(user)
        val selected = resolveSelectedOrg(orgId, memberships)
            ?: throw AuthError.noOrganisationAccess()
        val ref = getAccessibleOrgs(memberships).firstOrNull { it.id == orgId }
            ?: throw AuthError.noOrganisationAccess()

        return successFor(user, selected, ref)
    }

    /**
     * Builds a [AuthSignInResponse.Success] for the given [user] and [selectedOrg].
     */
    private suspend fun successFor(
        user: Stored<USER>,
        selectedOrg: SelectedOrg?,
        org: AuthOrgRef?,
    ): AuthSignInResponse.Success = successWithPayload(user, selectedOrg, org).first

    /**
     * As [successFor], but also returns the minted token's verified payload.
     *
     * Exists so [refreshToken] can inspect the new token's claims without verifying it a second time.
     */
    private suspend fun successWithPayload(
        user: Stored<USER>,
        selectedOrg: SelectedOrg?,
        org: AuthOrgRef?,
    ): Pair<AuthSignInResponse.Success, JwtPayload> {

        val token = generateJwt(user, selectedOrg)

        // Verify our own freshly minted token, and read the response's permissions, expiry and user id
        // back out of it. That is the point: the client is told exactly what the token carries, so the
        // two cannot disagree. Threading them out of `generateJwt` instead would create a second source.
        // Wrapped: `verify` raises JwtVerificationException, which is NOT an AuthError, so it would
        // escape AuthLoginApi's `catch (e: AuthError)` and surface as a 500 -- on the sign-up path that
        // happens AFTER the account, password record and PendingActivation marker are committed, leaving
        // an account that can never finish signing up. Same failure shape AuthRealm already defends
        // against for sendAuthEmail. Reachable via a JwtGenerator clock ahead of the realm's Kronos.
        val payload = try {
            deps.jwtGenerator.verify(token)
        } catch (e: JwtVerificationException) {
            deps.log.error("The realm minted a token its own verifier rejected", e)
            throw AuthError("Could not establish a session")
        }

        val success = AuthSignInResponse.Success(
            session = AuthSignInResponse.Session.Bearer(token),
            permissions = deps.jwtGenerator.extractPermissions(payload),
            expiresAt = payload.expiresAt?.let { MpInstant.fromEpochSeconds(it) },
            // extractUserData, not `sub` directly: this is the function the SERVER resolves identity
            // with (it prefers the userNs id claim and only falls back to `sub`), so the response
            // states the identity the request path will actually authorize as.
            userId = deps.jwtGenerator.extractUserData(payload).id.takeIf { it != UserRecord.ANONYMOUS_ID },
            realm = asApiModel(),
            user = users.serialize(user),
            org = org,
        )

        return success to payload
    }

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
