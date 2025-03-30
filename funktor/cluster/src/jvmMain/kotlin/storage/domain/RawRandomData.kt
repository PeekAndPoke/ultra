package de.peekandpoke.ktorfx.cluster.storage.domain

import de.peekandpoke.karango.Karango
import de.peekandpoke.ultra.common.datetime.MpInstant
import de.peekandpoke.ultra.vault.hooks.Timestamped

@Karango
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
