package de.peekandpoke.ultra.kontainer.examples

import de.peekandpoke.ultra.common.docs.ExampleRunner
import de.peekandpoke.ultra.kontainer.examples._01_defining_services.DefiningASingletonExample
import de.peekandpoke.ultra.kontainer.examples._01_defining_services.SharedSingletonExample
import de.peekandpoke.ultra.kontainer.examples._01_defining_services.SingletonVsDynamicVsPrototypeExample

fun main() {
    ExampleRunner().run(examples)
}

val examples = listOf(
    DefiningASingletonExample(),
    SharedSingletonExample(),
    SingletonVsDynamicVsPrototypeExample()
)
