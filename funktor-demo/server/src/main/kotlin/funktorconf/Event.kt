package io.peekandpoke.funktor.demo.server.funktorconf

import io.peekandpoke.funktor.demo.common.funktorconf.EventStatus
import io.peekandpoke.ultra.datetime.MpInstant
import io.peekandpoke.ultra.vault.Vault
import io.peekandpoke.ultra.vault.hooks.Timestamped

@Vault
data class Event(
    val name: String,
    val description: String = "",
    val venue: String = "",
    val status: EventStatus = EventStatus.Draft,
    val startDate: String = "",
    val endDate: String = "",
    override val createdAt: MpInstant = MpInstant.Epoch,
    override val updatedAt: MpInstant = createdAt,
) : Timestamped {
    override fun withCreatedAt(instant: MpInstant) = copy(createdAt = instant)
    override fun withUpdatedAt(instant: MpInstant) = copy(updatedAt = instant)
}
