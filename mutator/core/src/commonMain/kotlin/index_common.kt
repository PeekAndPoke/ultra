package io.peekandpoke.mutator

@DslMarker
annotation class MutatorDsl

annotation class Mutable(val deep: Boolean = true)

annotation class NotMutable
