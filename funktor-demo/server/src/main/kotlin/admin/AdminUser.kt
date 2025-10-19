package io.peekandpoke.funktor.demo.server.admin

import de.peekandpoke.karango.Karango
import de.peekandpoke.ultra.common.datetime.MpInstant
import de.peekandpoke.ultra.vault.hooks.Timestamped

@Karango
data class AdminUser(
    val name: String,
    val email: String,
    override val createdAt: MpInstant = MpInstant.Epoch,
    override val updatedAt: MpInstant = createdAt,
) : Timestamped {
    override fun withCreatedAt(instant: MpInstant) = copy(createdAt = instant)
    override fun withUpdatedAt(instant: MpInstant) = copy(updatedAt = instant)
}
