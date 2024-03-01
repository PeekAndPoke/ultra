package de.peekandpoke.ultra.common.model

import kotlinx.serialization.Serializable

/**
 * Represents an empty object.
 *
 * NOTICE: This cannot be an object, as some json-serializer have problems with this (like Jackson)
 */
@Serializable
class EmptyObject {
    companion object {
        val default = EmptyObject()
    }
}
