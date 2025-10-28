package io.peekandpoke.funktor.demo.server.admin

import de.peekandpoke.ultra.common.datetime.MpInstant
import de.peekandpoke.ultra.vault.Vault
import de.peekandpoke.ultra.vault.hooks.Timestamped

@Vault
data class AdminUser(
    val name: String,
    val email: String,
    val isSuperUser: Boolean = false,
    override val createdAt: MpInstant = MpInstant.Epoch,
    override val updatedAt: MpInstant = createdAt,
) : Timestamped {
    override fun withCreatedAt(instant: MpInstant) = copy(createdAt = instant)
    override fun withUpdatedAt(instant: MpInstant) = copy(updatedAt = instant)
}
