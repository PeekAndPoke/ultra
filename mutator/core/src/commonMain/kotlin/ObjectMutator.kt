package de.peekandpoke.mutator

@MutatorDsl
class ObjectMutator<V>(value: V) : Mutator.Base<V>(value) {
    @MutatorDsl
    operator fun invoke(block: ObjectMutator<V>.() -> Unit) = apply { block() }
}

