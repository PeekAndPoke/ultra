package de.peekandpoke.ultra.common.datetime

import kotlinx.serialization.Serializable

@Serializable
data class PartialPortableDateRange(
    val from: PortableDate?,
    val to: PortableDate?,
) {
    val asValidDateRange: PortableDateRange?
        get() = when (from != null && to != null) {
            true -> PortableDateRange(from = from, to = to).takeIf { it.isValid }
            false -> null
        }
}
