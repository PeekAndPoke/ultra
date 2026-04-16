package io.peekandpoke.funktor.auth.domain

import io.peekandpoke.ultra.datetime.MpInstant
import io.peekandpoke.ultra.slumber.Polymorphic
import io.peekandpoke.ultra.vault.Vault
import io.peekandpoke.ultra.vault.hooks.Timestamped

@Vault
sealed interface AuthRecord : Timestamped {

    data class Password(
        override val realm: String,
        override val ownerId: String,
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
        override val realm: String,
        override val ownerId: String,
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
     * Single-use token sent to a user's email address to verify ownership at sign-up time.
     * Consumed by `AuthSystem.verifyEmail(realm, token)`.
     */
    data class EmailVerificationToken(
        override val realm: String,
        override val ownerId: String,
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
        override val realm: String,
        override val ownerId: String,
        override val expiresAt: Long,
        override val createdAt: MpInstant = MpInstant.Epoch,
        override val updatedAt: MpInstant = createdAt,
        /** Random secret token sent to the new email address. */
        override val token: String,
        /** The email the user wants to switch to. */
        val pendingEmail: String,
    ) : AuthRecord {
        companion object : Polymorphic.TypedChild<EmailChangeToken> {
            override val identifier = "email-change-token"
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
        override val realm: String,
        override val ownerId: String,
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
        val lastSeenAt: MpInstant,
    ) : AuthRecord {
        companion object : Polymorphic.TypedChild<Session> {
            override val identifier = "session"
        }

        override fun withCreatedAt(instant: MpInstant) = copy(createdAt = instant)
        override fun withUpdatedAt(instant: MpInstant) = copy(updatedAt = instant)
    }

    /** The realm that record belongs to */
    @Vault.Field
    val realm: String

    /** The id of the owner / user */
    @Vault.Field
    val ownerId: String

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
