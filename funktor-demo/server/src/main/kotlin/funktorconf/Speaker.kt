package io.peekandpoke.funktor.demo.server.funktorconf

import io.peekandpoke.ultra.datetime.MpInstant
import io.peekandpoke.ultra.vault.Vault
import io.peekandpoke.ultra.vault.hooks.Timestamped

@Vault
data class Speaker(
    val name: String,
    val bio: String = "",
    val photoUrl: String = "",
    val talkTitle: String = "",
    val talkAbstract: String = "",
    override val createdAt: MpInstant = MpInstant.Epoch,
    override val updatedAt: MpInstant = createdAt,
) : Timestamped {
    override fun withCreatedAt(instant: MpInstant) = copy(createdAt = instant)
    override fun withUpdatedAt(instant: MpInstant) = copy(updatedAt = instant)
}
