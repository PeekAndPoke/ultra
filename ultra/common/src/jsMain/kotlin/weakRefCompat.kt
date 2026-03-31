package io.peekandpoke.ultra.common

/**
 * Checks whether a value can be used with JS `WeakRef`, `WeakSet`, or `WeakMap`.
 *
 * In JavaScript, only objects and non-registered symbols are eligible for weak references.
 * Primitive types (number, string, boolean, bigint, null, undefined) are rejected
 * with "Invalid value used in weak set / weak map".
 *
 * This function returns `true` for objects and functions, `false` for everything else.
 */
internal fun isWeakRefCompatible(value: Any?): Boolean {
    if (value == null) return false

    val t = jsTypeOf(value)
    return t == "object" || t == "function"
}
