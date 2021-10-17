package de.peekandpoke.ultra.common.model

import kotlinx.serialization.Serializable

@Serializable
data class Paged<T>(
    val items: List<T>,
    val page: Int,
    val epp: Int,
    val fullItemCount: Long?,
) {
    val fullPageCount: Long? get() = fullItemCount?.let { (it.toDouble() / epp.toDouble()).toLong() + 1 }

    companion object {
        fun <T> empty() = Paged<T>(items = emptyList(), page = 1, epp = 20, fullItemCount = null)
    }
}
