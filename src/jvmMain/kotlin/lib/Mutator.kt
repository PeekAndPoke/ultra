package de.peekandpoke.ultra.playground.lib

import de.peekandpoke.ultra.common.GetAndSet
import de.peekandpoke.ultra.common.Observable
import de.peekandpoke.ultra.common.Observer
import de.peekandpoke.ultra.common.OnChange

interface Mutator<V> : GetAndSet<V>, Observer {

    companion object {

    }

    abstract class Base<V>(
        private val initial: V,
        private val subscriptions: Observable.Subscriptions<V> = Observable.Subscriptions(),
    ) : Mutator<V>, Observable<V> by subscriptions {
        private var _value = initial

        override fun get(): V = _value

        override fun isModified(): Boolean = _value != initial

        override fun modifyValue(block: (V) -> V) {
            _value = block(_value)
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
}

fun <V, M : Mutator<V>> M.onChange(block: OnChange<V>): M = apply { observe(this, block) }

