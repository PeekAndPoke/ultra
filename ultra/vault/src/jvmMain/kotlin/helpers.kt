package io.peekandpoke.ultra.vault

import kotlin.reflect.KClass

/**
 * Extracts the document key from an id of the form `"collection/key"`.
 *
 * A string without a slash is returned unchanged, assuming it is already a bare key. Document keys
 * must not contain slashes — ArangoDB rejects them and the id format would be ambiguous — so this
 * splits at the FIRST slash rather than returning a middle segment if a malformed id shows up.
 */
val String.ensureKey get() = substringAfter('/')

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

/**
 * Resolves all items in this collection, returning a list of their wrapped values.
 *
 * This is a convenience for `map { it.resolve() }` that avoids the need for a suspend lambda
 * in regular [List.map] / [Iterable.map] calls.
 *
 * Works for any [Storable] subtype ([Stored], [New], [Ref]).
 */
suspend fun <T> Iterable<Storable<T>>.resolveAll(): List<T> = buildList {
    for (item in this@resolveAll) add(item.resolve())
}

/**
 * Provides non-suspend access to the value of a [Stored] entity.
 *
 * Unlike [Storable.resolve], this does NOT suspend because [Stored] always holds
 * the value eagerly in memory. Use this when you need the value in a non-suspend
 * context such as [List.map], [List.filter], [List.sortedBy], etc.
 */
@Suppress("NOTHING_TO_INLINE")
inline val <T> Stored<T>.value: T get() = valueInternal

/**
 * Provides non-suspend access to the value of a [New] entity.
 *
 * Unlike [Storable.resolve], this does NOT suspend because [New] always holds
 * the value eagerly in memory.
 */
@Suppress("NOTHING_TO_INLINE")
inline val <T> New<T>.value: T get() = valueInternal
