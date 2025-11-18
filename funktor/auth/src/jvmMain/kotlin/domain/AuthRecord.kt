package de.peekandpoke.funktor.auth.domain

import de.peekandpoke.ultra.common.datetime.MpInstant
import de.peekandpoke.ultra.slumber.Polymorphic
import de.peekandpoke.ultra.vault.Vault
import de.peekandpoke.ultra.vault.hooks.Timestamped

@Vault
sealed interface AuthRecord : Timestamped {

    data class Password(
        override val realm: String,
        override val ownerId: String,
        override val expiresAt: Long? = null,
        override val createdAt: MpInstant = MpInstant.Epoch,
        override val updatedAt: MpInstant = createdAt,
        /** The hashed password */
        override val token: String,
    ) : AuthRecord {
        companion object : Polymorphic.TypedChild<Password> {
            override val identifier = "password"
        }

        override fun withCreatedAt(instant: MpInstant) = copy(createdAt = instant)
        override fun withUpdatedAt(instant: MpInstant) = copy(updatedAt = instant)
    }

    data class PasswordRecovery(
        override val realm: String,
        override val ownerId: String,
        override val expiresAt: Long,
        override val createdAt: MpInstant = MpInstant.Epoch,
        override val updatedAt: MpInstant = createdAt,
        /** Very long secret token */
        override val token: String,
    ) : AuthRecord {
        companion object : Polymorphic.TypedChild<PasswordRecovery> {
            override val identifier = "password-recovery"
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
