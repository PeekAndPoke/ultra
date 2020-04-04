package de.peekandpoke.ultra.kontainer.examples

import de.peekandpoke.ultra.common.docs.Example
import de.peekandpoke.ultra.common.docs.ExampleRunner
import de.peekandpoke.ultra.kontainer.examples._01_the_basics.E01_GetSingletonWithUse
import de.peekandpoke.ultra.kontainer.examples._01_the_basics.E02_GetSingletonWithGet
import de.peekandpoke.ultra.kontainer.examples._01_the_basics.E03_SharedSingleton
import de.peekandpoke.ultra.kontainer.examples._01_the_basics.E04_SingletonVsDynamic

fun main() {
    ExampleRunner().run(examples)
}

val examples = listOf(
    E01_GetSingletonWithUse(),
    E02_GetSingletonWithGet(),
    E03_SharedSingleton(),
    E04_SingletonVsDynamic()
)

fun Example.codeUrl() =
    "https://github.com/PeekAndPoke/ultra/tree/master/kontainer/src/examples/kotlin/${this::class.simpleName}.kt"
