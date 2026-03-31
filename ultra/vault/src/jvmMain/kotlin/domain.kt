@file:Suppress("PropertyName", "detekt:VariableNaming")

package io.peekandpoke.ultra.vault

import com.fasterxml.jackson.annotation.JsonIgnore
import io.peekandpoke.ultra.vault.lang.VaultDslMarker
import kotlinx.serialization.SerialName

/**
 * Base sealed class for all document wrappers in the Vault persistence layer.
 *
 * A [Storable] wraps a domain value of type [T] together with database metadata
 * (`_id`, `_key`, `_rev`). Subclasses represent different lifecycle stages:
 * - [Stored] -- a persisted entity loaded from the database
 * - [Ref] -- an eager reference to a persisted entity
 * - [LazyRef] -- a lazy reference that resolves its value on first access
 * - [New] -- an entity that has not yet been persisted
 *
 * All subclasses support functional transformations via [modify], [withValue], and [transform],
 * as well as identity comparisons via [hasSameIdAs], [hasOtherIdThan], and [hasIdIn].
 */
sealed class Storable<out T> {
    /** The stored value of the document */
    abstract val value: T

    /** The database id in the form "collection/key" of the document */
    abstract val _id: String

    /** The database key of the document */
    abstract val _key: String

    /** The revision of the document */
    abstract val _rev: String

    /** The name of the collection the document is stored in */
    @get:JsonIgnore
    val collection get() = _id.split("/").first()

    /** Converts to a [Ref] */
    @get:JsonIgnore
    val asRef: Ref<T> get() = Ref(value, _id, _key, _rev)

    /** Converts to a [LazyRef] */
    @get:JsonIgnore
    val asLazyRef: LazyRef<T> get() = LazyRef(_id) { this }

    /** Converts to a [Stored] */
    @get:JsonIgnore
    val asStored: Stored<T> get() = Stored(value, _id, _key, _rev)

    /**
     * Converts to Stored<X> where [X] : [T] by mapping the value with [fn].
     */
    abstract fun <X : @UnsafeVariance T> modify(fn: (oldValue: T) -> X): Storable<X>

    /**
     * Converts to Stored<X> where [X] : [T] by setting the [newValue].
     */
    abstract fun <X : @UnsafeVariance T> withValue(newValue: X): Storable<X>

    /**
     * Transforms to Storable<N> where [N] has no relation to [T]
     */
    abstract fun <N> transform(fn: (current: T) -> N): Storable<N>

    /**
     * Checks if this [Storable] has the same id as the [other]
     */
    @VaultDslMarker
    infix fun hasSameIdAs(other: Storable<@UnsafeVariance T>?) = other != null && _id == other._id

    /**
     * Checks if this [Storable] has another id as the [other]
     */
    @VaultDslMarker
    infix fun hasOtherIdThan(other: Storable<@UnsafeVariance T>?) = !hasSameIdAs(other)

    /**
     * Checks if this [Storable] has an id that is equal to one of the [others]
     */
    @VaultDslMarker
    infix fun hasIdIn(others: List<Storable<@UnsafeVariance T>>) = others.any { it._id == _id }
}

/**
 * Represents a persisted entity that has been loaded from the database.
 *
 * A [Stored] carries the full database metadata ([_id], [_key], [_rev]) alongside
 * the domain [value]. It is the most common wrapper returned by repository queries.
 *
 * Use [modify] or [withValue] to create a copy with an updated value while preserving
 * the database identity, and [castTyped] / [castUntyped] for safe downcasting of the value.
 */
@SerialName(Stored.SERIAL_NAME)
data class Stored<out T>(
    override val value: T,
    override val _id: String,
    override val _key: String = _id.ensureKey,
    override val _rev: String = "",
) : Storable<T>() {

    companion object {
        const val SERIAL_NAME = "stored"
    }

    override fun <X : @UnsafeVariance T> modify(fn: (oldValue: T) -> X): Stored<X> {
        return withValue(fn(value))
    }

    /**
     * Suspending variant of [modify] that allows the mapping function to perform async work.
     */
    suspend fun <X : @UnsafeVariance T> modifyAsync(fn: suspend (oldValue: T) -> X): Stored<X> {
        return withValue(fn(value))
    }

    override fun <X : @UnsafeVariance T> withValue(newValue: X): Stored<X> {
        return Stored(
            value = newValue,
            _id = _id,
            _key = _key,
            _rev = _rev,
        )
    }

    override fun <N> transform(fn: (current: T) -> N): Stored<N> {
        return Stored(
            value = fn(value),
            _id = _id,
            _key = _key,
            _rev = _rev,
        )
    }

    /**
     * Safely casts the value to [X] where [X] must be a subtype of [T].
     *
     * @return this instance retyped as `Stored<X>`, or `null` if the value is not an instance of [X].
     */
    inline fun <reified X : @UnsafeVariance T> castTyped(): Stored<X>? {
        @Suppress("UNCHECKED_CAST")
        return when (value) {
            is X -> this as Stored<X>
            else -> null
        }
    }

    /**
     * Casts the value to an unrelated type [X].
     *
     * Unlike [castTyped], there is no compile-time relationship required between [T] and [X].
     *
     * @return this instance retyped as `Stored<X>`, or `null` if the value is not an instance of [X].
     */
    inline fun <reified X> castUntyped(): Stored<X>? {
        @Suppress("UNCHECKED_CAST")
        return when (value) {
            is X -> this as Stored<X>
            else -> null
        }
    }
}

/**
 * A lazy reference to a persisted entity, identified by its [_id].
 *
 * The actual [value] is resolved on first access via the [provider] function and then cached.
 * This is useful for relationships between entities where eager loading is undesirable.
 *
 * [LazyRef] is typically created during deserialization by [io.peekandpoke.ultra.vault.slumber.LazyRefCodec].
 */
@SerialName(LazyRef.SERIAL_NAME)
data class LazyRef<out T>(
    override val _id: String,
    internal val provider: (id: String) -> Storable<T>,
) : Storable<T>() {

    companion object {
        const val SERIAL_NAME = "lazy-ref"
    }

    private val provided by lazy { provider(_id) }

    override val value: T get() = provided.value

    override val _key: String = _id.ensureKey

    override val _rev: String get() = provided._rev

    override fun <X : @UnsafeVariance T> modify(fn: (oldValue: T) -> X): LazyRef<X> {
        return withValue(fn(value))
    }

    override fun <X : @UnsafeVariance T> withValue(newValue: X): LazyRef<X> {
        return LazyRef(
            _id = _id,
            provider = { provided.asStored.withValue(newValue) },
        )
    }

    override fun <N> transform(fn: (current: T) -> N): LazyRef<N> {
        return LazyRef(
            _id = _id,
            provider = { provided.asStored.transform(fn) },
        )
    }

    /**
     * Safely casts the value to [X] where [X] must be a subtype of [T].
     *
     * @return this instance retyped as `LazyRef<X>`, or `null` if the value is not an instance of [X].
     */
    inline fun <reified X : @UnsafeVariance T> castTyped(): LazyRef<X>? {
        @Suppress("UNCHECKED_CAST")
        return when (value) {
            is X -> this as LazyRef<X>
            else -> null
        }
    }

    /**
     * Casts the value to an unrelated type [X].
     *
     * @return this instance retyped as `LazyRef<X>`, or `null` if the value is not an instance of [X].
     */
    inline fun <reified X> castUntyped(): LazyRef<X>? {
        @Suppress("UNCHECKED_CAST")
        return when (value) {
            is X -> this as LazyRef<X>
            else -> null
        }
    }
}

/**
 * An eager reference to a persisted entity.
 *
 * Unlike [Stored], a [Ref] is typically used to represent a cross-reference to another entity
 * that has already been resolved. It carries the same metadata and value but serves a semantic
 * role as a "reference" rather than a "primary result."
 *
 * [Ref] is typically created during deserialization by [io.peekandpoke.ultra.vault.slumber.RefCodec].
 */
@SerialName(Ref.SERIAL_NAME)
data class Ref<out T>(
    override val value: T,
    override val _id: String,
    override val _key: String,
    override val _rev: String,
) : Storable<T>() {

    companion object {
        const val SERIAL_NAME = "ref"
    }

    override fun <X : @UnsafeVariance T> modify(fn: (oldValue: T) -> X): Ref<X> {
        return withValue(fn(value))
    }

    override fun <X : @UnsafeVariance T> withValue(newValue: X): Ref<X> {
        return Ref(
            value = newValue,
            _id = _id,
            _key = _key,
            _rev = _rev,
        )
    }

    override fun <N> transform(fn: (current: T) -> N): Ref<N> {
        return Ref(
            value = fn(value),
            _id = _id,
            _key = _key,
            _rev = _rev,
        )
    }

    /**
     * Safely casts the value to [X] where [X] must be a subtype of [T].
     *
     * @return this instance retyped as `Ref<X>`, or `null` if the value is not an instance of [X].
     */
    inline fun <reified X : @UnsafeVariance T> castTyped(): Ref<X>? {
        @Suppress("UNCHECKED_CAST")
        return when (value) {
            is X -> this as Ref<X>
            else -> null
        }
    }

    /**
     * Casts the value to an unrelated type [X].
     *
     * @return this instance retyped as `Ref<X>`, or `null` if the value is not an instance of [X].
     */
    inline fun <reified X> castUntyped(): Ref<X>? {
        @Suppress("UNCHECKED_CAST")
        return when (value) {
            is X -> this as Ref<X>
            else -> null
        }
    }
}

/**
 * Represents a new, unsaved entity that has not yet been persisted to the database.
 *
 * A [New] wraps a domain value with empty metadata by default. It is passed to
 * repository insert methods and becomes a [Stored] once persisted.
 */
@SerialName(New.SERIAL_NAME)
data class New<out T>(
    override val value: T,
    override val _id: String = "",
    override val _key: String = "",
    override val _rev: String = "",
) : Storable<T>() {

    companion object {
        const val SERIAL_NAME = "new"
    }

    override fun <X : @UnsafeVariance T> modify(fn: (oldValue: T) -> X): New<X> {
        return withValue(fn(value))
    }

    override fun <X : @UnsafeVariance T> withValue(newValue: X): New<X> {
        return New(
            value = newValue,
            _id = _id,
            _key = _key,
            _rev = _rev,
        )
    }

    override fun <N> transform(fn: (current: T) -> N): New<N> {
        return New(
            value = fn(value),
            _id = _id,
            _key = _key,
            _rev = _rev,
        )
    }

    /**
     * Safely casts the value to [X] where [X] must be a subtype of [T].
     *
     * @return this instance retyped as `New<X>`, or `null` if the value is not an instance of [X].
     */
    inline fun <reified X : @UnsafeVariance T> castTyped(): New<X>? {
        @Suppress("UNCHECKED_CAST")
        return when (value) {
            is X -> this as New<X>
            else -> null
        }
    }

    /**
     * Casts the value to an unrelated type [X].
     *
     * @return this instance retyped as `New<X>`, or `null` if the value is not an instance of [X].
     */
    inline fun <reified X> castUntyped(): New<X>? {
        @Suppress("UNCHECKED_CAST")
        return when (value) {
            is X -> this as New<X>
            else -> null
        }
    }
}
