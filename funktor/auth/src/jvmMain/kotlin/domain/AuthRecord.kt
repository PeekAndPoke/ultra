package de.peekandpoke.funktor.auth.domain

import de.peekandpoke.karango.Karango
import de.peekandpoke.ultra.common.datetime.MpInstant
import de.peekandpoke.ultra.slumber.Polymorphic
import de.peekandpoke.ultra.vault.hooks.Timestamped

@Karango
sealed interface AuthRecord : Timestamped {

    data class Password(
        override val realm: String,
        override val ownerId: String,
        override val expiresAt: Long? = null,
        override val createdAt: MpInstant = MpInstant.Epoch,
        override val updatedAt: MpInstant = createdAt,
        /** The hashed password */
        val hash: String,
    ) : AuthRecord {
        companion object : Polymorphic.TypedChild<Password> {
            override val identifier = "password"
        }

        override fun withCreatedAt(instant: MpInstant) = copy(createdAt = instant)
        override fun withUpdatedAt(instant: MpInstant) = copy(updatedAt = instant)
    }

    /** The realm that record belongs to */
    @Karango.Field
    val realm: String

    /** The id of the owner / user */
    @Karango.Field
    val ownerId: String

    /** Epoch seconds timestamp, when this entry expires, or NULL if it never expires */
    @Karango.Field
    val expiresAt: Long?

    @Karango.Field
    override val createdAt: MpInstant

    @Karango.Field
    override val updatedAt: MpInstant
}
