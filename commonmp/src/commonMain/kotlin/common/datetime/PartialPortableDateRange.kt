package de.peekandpoke.ultra.common.datetime

import kotlinx.serialization.Serializable

@Serializable
data class PartialPortableDateRange(
    val from: PortableDate?,
    val to: PortableDate?,
) {
    companion object {
        val empty = PartialPortableDateRange(from = null, to = null)
    }

    fun asValidDateRange(): PortableDateRange? = when (from != null && to != null) {
        true -> PortableDateRange(from = from, to = to).takeIf { it.isValid }
        false -> null
    }
}
