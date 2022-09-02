package de.peekandpoke.ultra.common

import kotlin.reflect.KMutableProperty0

interface GetAndSet<P> {

    companion object {
        fun <P> of(getter: () -> P, setter: (P) -> Unit): GetAndSet<P> = Impl(
            getter = getter,
            setter = { input -> input.also { setter(it) } },
        )

        fun <P> of(property: KMutableProperty0<P>): GetAndSet<P> = of(
            getter = { property.get() },
            setter = { property.set(it) }
        )
    }

    private class Impl<P>(
        private val getter: () -> P,
        private val setter: (P) -> P,
    ) : GetAndSet<P> {

        override fun hashCode(): Int {
            return get().hashCode()
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true

            if (other !is GetAndSet<*>) return false

            return this.get() == other.get()
        }

        override fun invoke(): P = getter()
        override fun invoke(input: P): P = setter(input)
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
