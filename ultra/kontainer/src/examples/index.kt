package de.peekandpoke.ultra.kontainer.examples

import de.peekandpoke.ultra.kontainer.examples.defining_services.DefiningASingletonExample
import de.peekandpoke.ultra.kontainer.examples.defining_services.SharedSingletonExample
import de.peekandpoke.ultra.kontainer.examples.defining_services.SingletonVsDynamicVsPrototypeExample
import de.peekandpoke.ultra.tooling.ExampleRunner

fun main() {
    ExampleRunner().run(examples)
}

val examples = listOf(
    DefiningASingletonExample(),
    SharedSingletonExample(),
    SingletonVsDynamicVsPrototypeExample()
)
