package io.peekandpoke.ultra.vault

import kotlin.reflect.KClass

/**
 * Extracts the document key from a database identifier string.
 *
 * If the string contains a slash (e.g. `"collection/key"`), returns the part after the slash.
 * Otherwise returns the string unchanged, assuming it is already a bare key.
 */
val String.ensureKey get() = if (contains('/')) split('/')[1] else this

/**
 * Filters a collection of [Storable] items, keeping only those whose value is an instance of [T].
 *
 * This only works on already-materialized collections of [Stored] or [New] items.
 *
 * @param cls the target [KClass] -- only used for reification, the parameter itself is unused at runtime.
 * @return a list of [Storable] retyped to the narrower type [T].
 */
inline fun <X : Any, reified T : X> Iterable<Storable<X>>.filterValueIsInstanceOf(
    @Suppress("UNUSED_PARAMETER") cls: KClass<T>,
): List<Storable<T>> {
    @Suppress("UNCHECKED_CAST")
    return filter { it.valueInternal is T } as List<Storable<T>>
}
