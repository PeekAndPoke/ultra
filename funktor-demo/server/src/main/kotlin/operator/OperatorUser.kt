package io.peekandpoke.funktor.demo.server.operator

import io.peekandpoke.ultra.datetime.MpInstant
import io.peekandpoke.ultra.vault.Vault
import io.peekandpoke.ultra.vault.hooks.Timestamped

@Vault
data class OperatorUser(
    val name: String,
    val email: String,
    val isSuperUser: Boolean = true,
    override val createdAt: MpInstant = MpInstant.Epoch,
    override val updatedAt: MpInstant = createdAt,
) : Timestamped {
    override fun withCreatedAt(instant: MpInstant) = copy(createdAt = instant)
    override fun withUpdatedAt(instant: MpInstant) = copy(updatedAt = instant)
}
