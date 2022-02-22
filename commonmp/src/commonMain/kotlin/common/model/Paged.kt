package de.peekandpoke.ultra.common.model

import kotlinx.serialization.Serializable
import kotlin.math.ceil

@Serializable
data class Paged<T>(
    val items: List<T>,
    val page: Int,
    val epp: Int,
    val fullItemCount: Long?,
) {
    val fullPageCount: Long?
        get() = fullItemCount?.let {
            ceil(it.toDouble() / epp.toDouble()).toLong()
        }

    companion object {
        fun <T> empty() = Paged<T>(items = emptyList(), page = 1, epp = 20, fullItemCount = null)
    }
}
