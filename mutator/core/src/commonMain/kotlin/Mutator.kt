package de.peekandpoke.mutator

import de.peekandpoke.ultra.common.GetAndSet
import de.peekandpoke.ultra.common.Observable
import de.peekandpoke.ultra.common.OnChange

@MutatorDsl
interface Mutator<V> : GetAndSet<V> {

    /**
     * Base class for [Mutator] implementations.
     */
    abstract class Base<V>(
        initial: V,
        private val subscriptions: Observable.Subscriptions<V> = Observable.Subscriptions(),
    ) : Mutator<V>, Observable<V> by subscriptions {
        private var _initial = initial
        private var _value = _initial

        override fun get(): V = _value

        override fun isModified(): Boolean = _value != _initial

        override fun getInitialValue(): V = _initial

        override fun modifyValue(block: (V) -> V) {
            val modified = block(_value)
            _value = if (modified != _initial) modified else _initial
            notifyObservers()
        }

        override fun commit() {
            _initial = _value
            notifyObservers()
        }

        fun <X> X.notifyObservers(): X {
            subscriptions.emit(_value)
            return this
        }
    }

    /**
     * Returns the current value.
     */
    override fun get(): V

    /**
     * Sets the value and returns the new value.
     */
    override fun set(input: V): V = input.also { modifyValue { input } }

    /**
     * Returns the current value.
     */
    override operator fun invoke(): V = get()

    /**
     * Sets the value and returns the new value.
     */
    override operator fun invoke(input: V): V = set(input)

    /**
     * Returns 'true' if the value has been modified since the last commit.
     */
    fun isModified(): Boolean

    /**
     * Returns the initial value.
     */
    fun getInitialValue(): V

    /**
     * Commits the current value, making it the initial value.
     */
    fun commit()

    /**
     * Modifies the value directly without the [Mutator].
     */
    fun modifyValue(block: (V) -> V)

    /**
     * Executes the given [block] if [previous] and [next] differ.
     */
    fun <X> modifyIfChanged(previous: X, next: X, block: (V) -> V) {
        if (previous != next) {
            modifyValue(block)
        }
    }

    fun <T : V> cast(value: T, block: Mutator<T>.(T) -> Unit) {
        @Suppress("UNCHECKED_CAST")
        (this as Mutator<T>).block(value)
    }
}

fun <V, M : Mutator<V>> M.onChange(block: OnChange<V>): M = apply { observe(this, block) }
