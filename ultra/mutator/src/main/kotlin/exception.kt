package de.peekandpoke.ultra.mutator

abstract class MutatorException(message: String) : Throwable(message = message)

class CouldNotCloneException(message: String) : MutatorException(message = message)
