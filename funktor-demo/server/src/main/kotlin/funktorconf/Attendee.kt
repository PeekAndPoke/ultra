package io.peekandpoke.funktor.demo.server.funktorconf

import io.peekandpoke.funktor.demo.common.funktorconf.TicketType
import io.peekandpoke.ultra.datetime.MpInstant
import io.peekandpoke.ultra.vault.Vault
import io.peekandpoke.ultra.vault.hooks.Timestamped

@Vault
data class Attendee(
    val name: String,
    val email: String = "",
    val ticketType: TicketType = TicketType.Standard,
    val checkedIn: Boolean = false,
    override val createdAt: MpInstant = MpInstant.Epoch,
    override val updatedAt: MpInstant = createdAt,
) : Timestamped {
    override fun withCreatedAt(instant: MpInstant) = copy(createdAt = instant)
    override fun withUpdatedAt(instant: MpInstant) = copy(updatedAt = instant)
}
