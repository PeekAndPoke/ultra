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
 * Filters a collection of [Storable] items, keeping only those whose [Storable.value] is an instance of [T].
 *
 * This is useful when working with a repository of a base type and you need to narrow
 * down to a specific subtype.
 *
 * @param cls the target [KClass] -- only used for reification, the parameter itself is unused at runtime.
 * @return a list of [Storable] retyped to the narrower type [T].
 */
inline fun <X : Any, reified T : X> Iterable<Storable<X>>.filterValueIsInstanceOf(
    @Suppress("UNUSED_PARAMETER") cls: KClass<T>,
): List<Storable<T>> {
    @Suppress("UNCHECKED_CAST")
    return filter { it.value is T } as List<Storable<T>>
}
