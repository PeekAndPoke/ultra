package de.peekandpoke.ultra.common.model

import kotlinx.serialization.Serializable

/**
 * Combination of a unique [id] with additional metadata.
 *
 * The [type] is an arbitrary value that helps to interpret the [id].
 */
@Serializable
data class ObjectKey(
    val id: String,
    val type: String,
) {
    companion object {
        val empty = ObjectKey(id = "", type = "")
    }

    fun encode(): String = "$type${"::"}$id"
}
