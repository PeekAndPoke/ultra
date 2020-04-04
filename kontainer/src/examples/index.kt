package de.peekandpoke.ultra.kontainer.examples

import de.peekandpoke.ultra.common.docs.ExampleRunner
import de.peekandpoke.ultra.kontainer.examples._01_the_basics.E01_GetSingletonWithUse
import de.peekandpoke.ultra.kontainer.examples._01_the_basics.E02_SharedSingleton
import de.peekandpoke.ultra.kontainer.examples._01_the_basics.E03_SingletonVsDynamic

fun main() {
    ExampleRunner().run(examples)
}

val examples = listOf(
    E01_GetSingletonWithUse(),
    E02_SharedSingleton(),
    E03_SingletonVsDynamic()
)
