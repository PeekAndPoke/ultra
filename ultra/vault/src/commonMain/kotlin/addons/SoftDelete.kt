package de.peekandpoke.ultra.vault.addons

import de.peekandpoke.ultra.common.datetime.MpInstant
import de.peekandpoke.ultra.vault.Vault
import kotlinx.serialization.Serializable

@Vault
@Serializable
data class SoftDelete(
    val deletedAt: MpInstant,
)
