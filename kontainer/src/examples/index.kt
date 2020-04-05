package de.peekandpoke.ultra.kontainer.examples

import de.peekandpoke.ultra.common.docs.ExampleRunner
import de.peekandpoke.ultra.kontainer.examples._01_the_basics.E01_BasicSingleton
import de.peekandpoke.ultra.kontainer.examples._01_the_basics.E02_SharedSingleton
import de.peekandpoke.ultra.kontainer.examples._01_the_basics.E03_SingletonVsDynamicVsPrototype

fun main() {
    ExampleRunner().run(examples)
}

val examples = listOf(
    E01_BasicSingleton(),
    E02_SharedSingleton(),
    E03_SingletonVsDynamicVsPrototype()
)
