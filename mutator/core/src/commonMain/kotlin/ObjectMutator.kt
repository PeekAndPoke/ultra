package io.peekandpoke.mutator

@MutatorDsl
class ObjectMutator<V>(value: V) : Mutator.Base<V>(value) {
    operator fun invoke(block: ObjectMutator<V>.() -> Unit) = apply { block() }
}
