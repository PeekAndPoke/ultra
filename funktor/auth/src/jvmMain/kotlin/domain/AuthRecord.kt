package io.peekandpoke.funktor.auth.domain

import io.peekandpoke.funktor.auth.model.RealmId
import io.peekandpoke.ultra.datetime.MpInstant
import io.peekandpoke.ultra.security.user.EmailAddress
import io.peekandpoke.ultra.security.user.UserId
import io.peekandpoke.ultra.slumber.Polymorphic
import io.peekandpoke.ultra.vault.Vault
import io.peekandpoke.ultra.vault.hooks.Timestamped

@Vault
sealed interface AuthRecord : Timestamped {

    data class Password(
        override val realm: RealmId,
        override val ownerId: UserId,
        override val createdAt: MpInstant = MpInstant.Epoch,
        override val updatedAt: MpInstant = createdAt,
        /** The hashed password */
        override val token: String,
    ) : AuthRecord {
        companion object : Polymorphic.TypedChild<Password> {
            override val identifier = "password"
        }

        override val expiresAt: Long? = null

        override fun withCreatedAt(instant: MpInstant) = copy(createdAt = instant)
        override fun withUpdatedAt(instant: MpInstant) = copy(updatedAt = instant)
    }

    data class PasswordRecoveryToken(
        override val realm: RealmId,
        override val ownerId: UserId,
        override val expiresAt: Long,
        override val createdAt: MpInstant = MpInstant.Epoch,
        override val updatedAt: MpInstant = createdAt,
        /** Very long secret token */
        override val token: String,
    ) : AuthRecord {
        companion object : Polymorphic.TypedChild<PasswordRecoveryToken> {
            override val identifier = "password-recovery-token"
        }

        override fun withCreatedAt(instant: MpInstant) = copy(createdAt = instant)
        override fun withUpdatedAt(instant: MpInstant) = copy(updatedAt = instant)
    }

    /**
     * The account has not yet proven it owns its email address. PRESENCE is the state — there is no
     * "activated" flag anywhere, and no row means activated.
     *
     * Deliberately separate from [EmailVerificationToken], and deliberately non-expiring. Using "an
     * unexpired verification token exists" as the state would fail OPEN: `findLatestRecordBy` filters
     * expired records out, so every account that let its activation link lapse would silently become
     * activated. Keeping the state here lets the token expire on its own schedule.
     *
     * Written by `EmailAndPasswordAuth.signUp`, removed by `activateAccount` — and also by a completed
     * password reset.
     *
     * **The rule for clearing it: an operation must BOTH prove control of the mailbox AND invalidate
     * every password set before it.** A password reset does both — the token was mailed to the
     * address, and `findLatestPasswordRecord` only ever validates the NEWEST password record, so the
     * password chosen at sign-up stops working the moment the reset writes a new one.
     *
     * That rule is why two neighbouring operations deliberately do NOT clear it, even though both look
     * like they prove enough:
     * - **SSO sign-in for the same address.** It proves the mailbox but leaves the sign-up password
     *   intact. Clearing here would REOPEN the attack this whole feature closes: an attacker registers
     *   `victim@corp.com` with a password of their choosing, the victim later signs in with Google,
     *   and the attacker's password would silently start working. The victim's own way back is
     *   "forgot password", which satisfies both halves.
     * - **Authenticated `setPassword`.** It proves knowledge of the current password but says nothing
     *   about the mailbox.
     *
     * The cost is that an account which signed up by password and never activated cannot use its
     * password until it goes through a reset, even if it signs in via SSO. That is the intended
     * trade: a stuck credential, not a shared account.
     */
    data class PendingActivation(
        override val realm: RealmId,
        override val ownerId: UserId,
        override val createdAt: MpInstant = MpInstant.Epoch,
        override val updatedAt: MpInstant = createdAt,
    ) : AuthRecord {
        companion object : Polymorphic.TypedChild<PendingActivation> {
            override val identifier = "pending-activation"
        }

        /** Never expires: it is state, not a secret. */
        override val expiresAt: Long? = null

        /** Carries no secret. */
        override val token: String? = null

        override fun withCreatedAt(instant: MpInstant) = copy(createdAt = instant)
        override fun withUpdatedAt(instant: MpInstant) = copy(updatedAt = instant)
    }

    /**
     * Single-use token sent to a user's email address to verify ownership at sign-up time.
     * Consumed by `EmailAndPasswordAuth.activateAccount`, which also removes the [PendingActivation]
     * marker.
     */
    data class EmailVerificationToken(
        override val realm: RealmId,
        override val ownerId: UserId,
        override val expiresAt: Long,
        override val createdAt: MpInstant = MpInstant.Epoch,
        override val updatedAt: MpInstant = createdAt,
        /** Random secret token sent to the user's email. */
        override val token: String,
    ) : AuthRecord {
        companion object : Polymorphic.TypedChild<EmailVerificationToken> {
            override val identifier = "email-verification-token"
        }

        override fun withCreatedAt(instant: MpInstant) = copy(createdAt = instant)
        override fun withUpdatedAt(instant: MpInstant) = copy(updatedAt = instant)
    }

    /**
     * Single-use token sent to the user's *new* email address when they request an email change.
     * Carries the pending-new-email so it can be applied on confirmation. Consumed by
     * `AuthSystem.confirmEmailChange(realm, token)`.
     */
    data class EmailChangeToken(
        override val realm: RealmId,
        override val ownerId: UserId,
        override val expiresAt: Long,
        override val createdAt: MpInstant = MpInstant.Epoch,
        override val updatedAt: MpInstant = createdAt,
        /** Random secret token sent to the new email address. */
        override val token: String,
        /** The email the user wants to switch to. Canonical — see [EmailAddress]. */
        val pendingEmail: EmailAddress,
    ) : AuthRecord {
        companion object : Polymorphic.TypedChild<EmailChangeToken> {
            override val identifier = "email-change-token"
        }

        override fun withCreatedAt(instant: MpInstant) = copy(createdAt = instant)
        override fun withUpdatedAt(instant: MpInstant) = copy(updatedAt = instant)
    }

    /**
     * Single-use, short-lived token issued when a sign-in resolves to multiple organisations.
     * The user exchanges it (plus a chosen org id) at `select-org` for a full session.
     */
    data class OrgSelectionToken(
        override val realm: RealmId,
        override val ownerId: UserId,
        override val expiresAt: Long,
        override val createdAt: MpInstant = MpInstant.Epoch,
        override val updatedAt: MpInstant = createdAt,
        /** Random secret token handed to the client between credential-check and org-selection. */
        override val token: String,
    ) : AuthRecord {
        companion object : Polymorphic.TypedChild<OrgSelectionToken> {
            override val identifier = "org-selection-token"
        }

        override fun withCreatedAt(instant: MpInstant) = copy(createdAt = instant)
        override fun withUpdatedAt(instant: MpInstant) = copy(updatedAt = instant)
    }

    /**
     * An active login session. Each successful sign-in creates one row; the JWT issued to the
     * client carries the row's `_id` as a `sessionId` claim and the auth middleware validates
     * the session still exists on each request (cached). Revoking the row logs the user out.
     */
    data class Session(
        override val realm: RealmId,
        override val ownerId: UserId,
        override val expiresAt: Long,
        override val createdAt: MpInstant = MpInstant.Epoch,
        override val updatedAt: MpInstant = createdAt,
        /**
         * Random secret token (also the session id). Mirrored as the row's `_key` so the
         * primary index is the lookup path.
         */
        override val token: String,
        /** Hash of (userAgent + ipAddress). Used to detect new-device logins. */
        val deviceFingerprint: String,
        val userAgent: String?,
        val ipAddress: String?,
        /** Last time this session was observed making a request. Debounced write. */
        val lastSeenAt: MpInstant = MpInstant.Epoch,
    ) : AuthRecord {
        companion object : Polymorphic.TypedChild<Session> {
            override val identifier = "session"
        }

        override fun withCreatedAt(instant: MpInstant) = copy(createdAt = instant)
        override fun withUpdatedAt(instant: MpInstant) = copy(updatedAt = instant)
    }

    /** The realm that record belongs to */
    @Vault.Field
    val realm: RealmId

    /**
     * The id of the owner / user.
     *
     * Always the realm-qualified Vault `_id` (`<user collection>/<key>`), which is what makes it
     * globally unique across the per-realm user stores.
     */
    @Vault.Field
    val ownerId: UserId

    /** Epoch seconds timestamp, when this entry expires, or NULL if it never expires */
    @Vault.Field
    val expiresAt: Long?

    /** The token, f.e. the password-hash or the password-reset-token */
    @Vault.Field
    val token: String?

    @Vault.Field
    override val createdAt: MpInstant

    @Vault.Field
    override val updatedAt: MpInstant
}
