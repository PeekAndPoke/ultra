package de.peekandpoke.ultra.playground.lib

class DataClassMutator<V>(value: V) : Mutator.Base<V>(value) {
    operator fun invoke(block: DataClassMutator<V>.() -> Unit) = apply { block() }
}
