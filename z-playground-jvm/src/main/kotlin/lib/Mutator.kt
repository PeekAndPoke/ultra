package de.peekandpoke.ultra.playground.lib

import de.peekandpoke.ultra.common.GetAndSet
import de.peekandpoke.ultra.common.Observable

interface Mutator<V> : GetAndSet<V> {

    abstract class Base<V>(
        private val initial: V,
        private val subscriptions: Observable.Subscriptions<V> = Observable.Subscriptions(),
    ) : Mutator<V>, Observable<V> by subscriptions {
        private var _value = initial

        override val isModified: Boolean get() = value != initial
        override val value get() = _value

        fun <X> X.commit(): X {
            subscriptions.emit(value)
            return this
        }

        override fun modify(block: (V) -> V) {
            _value = block(_value)
            commit()
        }
    }

    class Null<V>(value: V) : Base<V>(value)

    val value: V
    val isModified: Boolean
    fun modify(block: (V) -> V)

    fun <X> modifyIfChanged(previous: X, next: X, block: (V) -> V) {
        if (previous != next) {
            modify(block)
        }
    }

    override fun get(): V = value
    override fun set(input: V): V = input.also { modify { input } }

    override operator fun invoke(): V = get()
    override operator fun invoke(input: V): V = set(input)
}
