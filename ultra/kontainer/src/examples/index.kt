package de.peekandpoke.ultra.kontainer.examples

import de.peekandpoke.ultra.extras.docs.ExampleRunner
import de.peekandpoke.ultra.kontainer.examples.defining_services.DefiningASingletonExample
import de.peekandpoke.ultra.kontainer.examples.defining_services.SharedSingletonExample
import de.peekandpoke.ultra.kontainer.examples.defining_services.SingletonVsDynamicVsPrototypeExample

fun main() {
    ExampleRunner().run(examples)
}

val examples = listOf(
    DefiningASingletonExample(),
    SharedSingletonExample(),
    SingletonVsDynamicVsPrototypeExample()
)
