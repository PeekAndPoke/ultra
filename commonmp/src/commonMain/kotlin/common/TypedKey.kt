package de.peekandpoke.ultra.common

/**
 * Defines a typed key to be used with [TypedAttributes]
 */
class TypedKey<T>(val name: String = "") {
    override fun toString() = name
}
