package io.peekandpoke.ultra.kontainer.examples

import io.peekandpoke.ultra.kontainer.examples.defining_modules._DefiningModules
import io.peekandpoke.ultra.kontainer.examples.defining_services._DefiningServices
import io.peekandpoke.ultra.kontainer.examples.injecting_services._InjectingServices
import io.peekandpoke.ultra.tooling.ExamplesToDocs
import java.io.File

fun main() {
    val generator = object : ExamplesToDocs(
        title = "Examples for ultra::kontainer",
        chapters = chapters,
        sourceLocation = File("src/examples"),
        outputLocation = File("common/docs/ultra::docs"),
    ) {}

    generator.run()
}

val chapters = listOf(
    _DefiningServices(),
    _InjectingServices(),
    _DefiningModules()
    // TODO: definition order / overwriting services
    // TODO: dynamic promotion / preventing degradation
    // TODO: meta information / debug
    // TODO: advanced topics / inject Kontainer, Blueprint, InjectionContext
)
