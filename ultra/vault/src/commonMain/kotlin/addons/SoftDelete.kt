package io.peekandpoke.ultra.vault.addons

import io.peekandpoke.ultra.datetime.MpInstant
import io.peekandpoke.ultra.vault.Vault
import kotlinx.serialization.Serializable

@Vault
@Serializable
data class SoftDelete(
    val deletedAt: MpInstant,
)
