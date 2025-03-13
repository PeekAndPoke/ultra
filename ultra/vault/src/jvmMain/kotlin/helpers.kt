package de.peekandpoke.ultra.vault

import kotlin.reflect.KClass

/**
 * Helper function that extracts an arangodb key from an id
 */
val String.ensureKey get() = if (contains('/')) split('/')[1] else this

/**
 * Filters all Storable where the value is an instance of [T].
 */
inline fun <X : Any, reified T : X> Iterable<Storable<X>>.filterValueIsInstanceOf(
    @Suppress("UNUSED_PARAMETER") cls: KClass<T>,
): List<Storable<T>> {
    @Suppress("UNCHECKED_CAST")
    return filter { it.value is T } as List<Storable<T>>
}
