package io.peekandpoke.funktor.cluster.storage.domain

import io.peekandpoke.ultra.datetime.MpInstant
import io.peekandpoke.ultra.vault.Vault
import io.peekandpoke.ultra.vault.hooks.Timestamped

@Vault
data class RawRandomData(
    val category: String,
    val dataId: String,
    val data: Any?,
    override val createdAt: MpInstant = MpInstant.Epoch,
    override val updatedAt: MpInstant = MpInstant.Epoch,
) : Timestamped {
    override fun withCreatedAt(instant: MpInstant): Timestamped = copy(createdAt = instant)

    override fun withUpdatedAt(instant: MpInstant): Timestamped = copy(updatedAt = instant)
}
