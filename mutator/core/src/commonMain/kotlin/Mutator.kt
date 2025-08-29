package de.peekandpoke.mutator

import de.peekandpoke.ultra.common.GetAndSet
import de.peekandpoke.ultra.common.Observable
import de.peekandpoke.ultra.common.Observer
import de.peekandpoke.ultra.common.OnChange

@MutatorDsl
interface Mutator<V> : GetAndSet<V>, Observer {

    abstract class Base<V>(
        private val initial: V,
        private val subscriptions: Observable.Subscriptions<V> = Observable.Subscriptions(),
    ) : Mutator<V>, Observable<V> by subscriptions {
        private var _value = initial

        override fun get(): V = _value

        override fun isModified(): Boolean = _value != initial

        override fun modifyValue(block: (V) -> V) {
            val modified = block(_value)
            _value = if (modified != initial) modified else initial
            commit()
        }

        fun <X> X.commit(): X {
            subscriptions.emit(_value)
            return this
        }
    }

    class Null<V>(value: V) : Base<V>(value)

    fun modifyValue(block: (V) -> V)

    fun isModified(): Boolean

    fun <X> modifyIfChanged(previous: X, next: X, block: (V) -> V) {
        if (previous != next) {
            modifyValue(block)
        }
    }

    override fun get(): V
    override fun set(input: V): V = input.also { modifyValue { input } }

    override operator fun invoke(): V = get()
    override operator fun invoke(input: V): V = set(input)

    @MutatorDsl
    fun <T : V> cast(value: T, block: Mutator<T>.(T) -> Unit) {
        @Suppress("UNCHECKED_CAST")
        (this as Mutator<T>).block(value)
    }
}

fun <V, M : Mutator<V>> M.onChange(block: OnChange<V>): M = apply { observe(this, block) }

