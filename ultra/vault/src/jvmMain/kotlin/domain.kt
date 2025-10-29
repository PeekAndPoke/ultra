@file:Suppress("PropertyName", "detekt:VariableNaming")

package de.peekandpoke.ultra.vault

import com.fasterxml.jackson.annotation.JsonIgnore
import de.peekandpoke.ultra.vault.lang.VaultDslMarker
import kotlinx.serialization.SerialName

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
    infix fun hasIdIn(others: List<Storable<@UnsafeVariance T>>) = _id in others.map { it._id }
}

@SerialName(Stored.serialName)
data class Stored<out T>(
    override val value: T,
    override val _id: String,
    override val _key: String,
    override val _rev: String,
) : Storable<T>() {

    companion object {
        const val serialName = "stored"
    }

    override fun <X : @UnsafeVariance T> modify(fn: (oldValue: T) -> X): Stored<X> {
        return withValue(fn(value))
    }

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

    inline fun <reified X : @UnsafeVariance T> castTyped(): Stored<X>? {
        @Suppress("UNCHECKED_CAST")
        return when (value) {
            is X -> this as Stored<X>
            else -> null
        }
    }

    inline fun <reified X> castUntyped(): Stored<X>? {
        @Suppress("UNCHECKED_CAST")
        return when (value) {
            is X -> this as Stored<X>
            else -> null
        }
    }
}

@SerialName(LazyRef.serialName)
data class LazyRef<out T>(
    override val _id: String,
    internal val provider: (id: String) -> Storable<T>,
) : Storable<T>() {

    companion object {
        const val serialName = "lazy-ref"
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

    inline fun <reified X : @UnsafeVariance T> castTyped(): LazyRef<X>? {
        @Suppress("UNCHECKED_CAST")
        return when (value) {
            is X -> this as LazyRef<X>
            else -> null
        }
    }

    inline fun <reified X> castUntyped(): LazyRef<X>? {
        @Suppress("UNCHECKED_CAST")
        return when (value) {
            is X -> this as LazyRef<X>
            else -> null
        }
    }
}

@SerialName(Ref.serialName)
data class Ref<out T>(
    override val value: T,
    override val _id: String,
    override val _key: String,
    override val _rev: String,
) : Storable<T>() {

    companion object {
        const val serialName = "ref"
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

    inline fun <reified X : @UnsafeVariance T> castTyped(): Ref<X>? {
        @Suppress("UNCHECKED_CAST")
        return when (value) {
            is X -> this as Ref<X>
            else -> null
        }
    }

    inline fun <reified X> castUntyped(): Ref<X>? {
        @Suppress("UNCHECKED_CAST")
        return when (value) {
            is X -> this as Ref<X>
            else -> null
        }
    }
}

@SerialName(New.serialName)
data class New<out T>(
    override val value: T,
    override val _id: String = "",
    override val _key: String = "",
    override val _rev: String = "",
) : Storable<T>() {

    companion object {
        const val serialName = "new"
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

    inline fun <reified X : @UnsafeVariance T> castTyped(): New<X>? {
        @Suppress("UNCHECKED_CAST")
        return when (value) {
            is X -> this as New<X>
            else -> null
        }
    }

    inline fun <reified X> castUntyped(): New<X>? {
        @Suppress("UNCHECKED_CAST")
        return when (value) {
            is X -> this as New<X>
            else -> null
        }
    }
}
