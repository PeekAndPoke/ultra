package de.peekandpoke.mutator

@MutatorDsl
class DataClassMutator<V>(value: V) : Mutator.Base<V>(value) {
    @MutatorDsl
    operator fun invoke(block: DataClassMutator<V>.() -> Unit) = apply { block() }
}
