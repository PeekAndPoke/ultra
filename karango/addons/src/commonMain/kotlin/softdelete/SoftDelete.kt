package de.peekandpoke.karango.addons.softdelete

import de.peekandpoke.ultra.common.datetime.MpInstant
import kotlinx.serialization.Serializable

@Serializable
data class SoftDelete(
    val deletedAt: MpInstant,
)
