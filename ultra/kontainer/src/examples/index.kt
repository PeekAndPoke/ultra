package io.peekandpoke.ultra.kontainer.examples

import io.peekandpoke.ultra.kontainer.examples.defining_services.DefiningASingletonExample
import io.peekandpoke.ultra.kontainer.examples.defining_services.SharedSingletonExample
import io.peekandpoke.ultra.kontainer.examples.defining_services.SingletonVsDynamicVsPrototypeExample
import io.peekandpoke.ultra.tooling.ExampleRunner

fun main() {
    ExampleRunner().run(examples)
}

val examples = listOf(
    DefiningASingletonExample(),
    SharedSingletonExample(),
    SingletonVsDynamicVsPrototypeExample()
)
