package de.peekandpoke.ultra.kontainer.examples

import de.peekandpoke.ultra.common.docs.ExampleRunner
import de.peekandpoke.ultra.kontainer.examples._01_the_basics.BasicSingletonExample
import de.peekandpoke.ultra.kontainer.examples._01_the_basics.SharedSingletonExample
import de.peekandpoke.ultra.kontainer.examples._01_the_basics.SingletonVsDynamicVsPrototypeExample

fun main() {
    ExampleRunner().run(examples)
}

val examples = listOf(
    BasicSingletonExample(),
    SharedSingletonExample(),
    SingletonVsDynamicVsPrototypeExample()
)
