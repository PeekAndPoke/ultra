@file:Suppress("PropertyName", "detekt:VariableNaming")

package io.peekandpoke.ultra.vault

import com.fasterxml.jackson.annotation.JsonIgnore
import io.peekandpoke.ultra.vault.lang.VaultDslMarker
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerialName

/**
 * Base sealed class for all document wrappers in the Vault persistence layer.
 *
 * A [Storable] wraps a domain value of type [T] together with database metadata
 * (`_id`, `_key`, `_rev`). Subclasses represent different lifecycle stages:
 * - [Stored] -- a persisted entity loaded from the database
 * - [Ref] -- a lazy reference to a persisted entity, resolved via [resolve] / [invoke]
 * - [New] -- an entity that has not yet been persisted
 *
 * Access the wrapped value via [resolve] (suspend) or [invoke] (shorthand).
 * For [Stored] and [New], resolution is instant. For [Ref], it may suspend to load from the database.
 */
sealed class Storable<out T> {

    /** The database id in the form "collection/key" of the document */
    abstract val _id: String

    /** The database key of the document */
    abstract val _key: String

    /** The revision of the document */
    abstract val _rev: String

    /** Resolves the wrapped value. Instant for [Stored]/[New], may suspend for [Ref]. */
    abstract suspend fun resolve(): T

    /** Shorthand for [resolve]. */
    suspend operator fun invoke(): T = resolve()

    /** The name of the collection the document is stored in */
    @get:JsonIgnore
    val collection get() = _id.split("/").first()

    /** Converts to a [Ref] wrapping the already-resolved value. */
    @get:JsonIgnore
    val asRef: Ref<T>
        get() = Ref.eager(valueInternal, _id, _key, _rev)

    /** Converts to a [Stored] */
    @get:JsonIgnore
    val asStored: Stored<T>
        get() = Stored(valueInternal, _id, _key, _rev)

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

    /**
     * Internal non-suspend value access for subclasses that always have the value available.
     *
     * Must only be called on [Stored] and [New]. Calling on [Ref] that hasn't been resolved throws.
     */
    @PublishedApi
    internal abstract val valueInternal: T
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
    val value: T,
    override val _id: String,
    override val _key: String = _id.ensureKey,
    override val _rev: String = "",
) : Storable<T>() {

    companion object {
        const val SERIAL_NAME = "stored"
    }

    @PublishedApi
    override val valueInternal: T get() = value

    override suspend fun resolve(): T = value

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
 * The actual value is resolved on first [resolve] / [invoke] call via the suspend [resolver]
 * and then cached. This is useful for relationships between entities where eager loading
 * is undesirable.
 *
 * [Ref] is typically created during deserialization by [io.peekandpoke.ultra.vault.slumber.RefCodec].
 * Use [Ref.eager] for already-loaded values, and [Ref.lazy] for deferred resolution.
 *
 * Equality is based on [_id] only — two Refs with the same ID are equal regardless of
 * their resolution state.
 */
@SerialName(Ref.SERIAL_NAME)
class Ref<out T>(
    override val _id: String,
    private val resolver: suspend () -> Storable<T>,
) : Storable<T>() {

    companion object {
        const val SERIAL_NAME = "ref"

        /** Wraps an already-loaded value. Used by [Storable.asRef], tests, save paths. */
        fun <T> eager(value: T, _id: String, _key: String, _rev: String): Ref<T> {
            val stored = Stored(value, _id, _key, _rev)
            return Ref(_id) { stored }
        }

        /** Creates a lazy ref resolved on first access. Used by RefCodec during deserialization. */
        fun <T> lazy(_id: String, resolver: suspend () -> Storable<T>): Ref<T> =
            Ref(_id, resolver)
    }

    private val mutex = Mutex()

    @Volatile
    private var _cached: Storable<T>? = null

    @PublishedApi
    override val valueInternal: T
        get() = _cached?.valueInternal
            ?: throw IllegalStateException("Ref($_id) not yet resolved. Call resolve() first.")

    override suspend fun resolve(): T {
        // Fast path: already resolved
        _cached?.let { return it.resolve() }

        // Slow path: resolve under mutex to ensure single resolver call
        return mutex.withLock {
            _cached?.let { return it.resolve() }
            val result = resolver()
            _cached = result
            result.resolve()
        }
    }

    override val _key: String get() = _id.ensureKey

    override val _rev: String get() = _cached?._rev ?: ""

    override fun equals(other: Any?): Boolean =
        this === other || (other is Ref<*> && _id == other._id)

    override fun hashCode(): Int = _id.hashCode()

    override fun toString(): String = "Ref(_id=$_id)"

    override fun <X : @UnsafeVariance T> modify(fn: (oldValue: T) -> X): Ref<X> {
        val self = this
        return lazy(_id) {
            val resolved = self.resolve()
            Stored(fn(resolved), _id, _key, _rev)
        }
    }

    override fun <X : @UnsafeVariance T> withValue(newValue: X): Ref<X> =
        eager(newValue, _id, _key, _rev)

    override fun <N> transform(fn: (current: T) -> N): Ref<N> {
        val self = this
        return lazy(_id) {
            val resolved = self.resolve()
            Stored(fn(resolved), _id, _key, _rev)
        }
    }

    /**
     * Safely casts the resolved value to [X] where [X] must be a subtype of [T].
     *
     * NOTE: This resolves the Ref if not already cached.
     *
     * @return this instance retyped as `Ref<X>`, or `null` if the value is not an instance of [X].
     */
    suspend inline fun <reified X : @UnsafeVariance T> castTyped(): Ref<X>? {
        val resolved = resolve()
        @Suppress("UNCHECKED_CAST")
        return when (resolved) {
            is X -> this as Ref<X>
            else -> null
        }
    }

    /**
     * Casts the resolved value to an unrelated type [X].
     *
     * NOTE: This resolves the Ref if not already cached.
     *
     * @return this instance retyped as `Ref<X>`, or `null` if the value is not an instance of [X].
     */
    suspend inline fun <reified X> castUntyped(): Ref<X>? {
        val resolved = resolve()
        @Suppress("UNCHECKED_CAST")
        return when (resolved) {
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
    val value: T,
    override val _id: String = "",
    override val _key: String = "",
    override val _rev: String = "",
) : Storable<T>() {

    companion object {
        const val SERIAL_NAME = "new"
    }

    @PublishedApi
    override val valueInternal: T get() = value

    override suspend fun resolve(): T = value

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
