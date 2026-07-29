package io.peekandpoke.ultra.common

import kotlin.reflect.KMutableProperty0

/**
 * An observable property accessor that combines a getter and setter into a single interface.
 *
 * Implements [Observable] so observers can be notified of value changes.
 *
 * @param P The type of the property value.
 */
interface GetAndSet<P> : Observable<P> {

    companion object {
        /**
         * Creates a [GetAndSet] from the given [getter] and [setter] functions.
         */
        fun <P> of(getter: () -> P, setter: (P) -> Unit): GetAndSet<P> = Impl(
            getter = getter,
            setter = { input -> input.also { setter(it) } },
        )

        /**
         * Creates a [GetAndSet] backed by the given mutable [property].
         */
        fun <P> of(property: KMutableProperty0<P>): GetAndSet<P> = of(
            getter = { property.get() },
            setter = { property.set(it) }
        )
    }

    /**
     * Default [GetAndSet] implementation backed by a getter/setter pair.
     *
     * Has value semantics: two instances are equal when their current values are equal. Both
     * [equals] and [hashCode] therefore change whenever the underlying value changes, so instances
     * must not be used as hash-based collection keys.
     */
    private class Impl<P>(
        private val getter: () -> P,
        private val setter: (P) -> P,
        private val subscriptions: Observable.Subscriptions<P> = Observable.Subscriptions(),
    ) : GetAndSet<P>, Observable<P> by subscriptions {

        /**
         * Hash of the CURRENT value, which moves as the value does.
         *
         * That rules this out as a key in a hash-based collection: storing it and then setting a new
         * value leaves an entry that can no longer be looked up. Inherent to comparing by value.
         */
        override fun hashCode(): Int {
            return get().hashCode()
        }

        /**
         * Compares by current value against another [Impl].
         *
         * Deliberately NOT against any [GetAndSet]: other implementors use
         * identity (`Mutator` among them), so accepting them here would make `a == b` and `b == a`
         * disagree.
         */
        override fun equals(other: Any?): Boolean {
            if (this === other) return true

            if (other !is Impl<*>) return false

            return this.get() == other.get()
        }

        override fun invoke(): P = getter()

        override fun invoke(input: P): P = setter(input).also {
            subscriptions.emit(it)
        }
    }

    /** Get the value */
    operator fun invoke(): P

    /** Get the value */
    fun get(): P = invoke()

    /** Set the value */
    operator fun invoke(input: P): P

    /** Set the value */
    fun set(input: P): P = invoke(input)
}
