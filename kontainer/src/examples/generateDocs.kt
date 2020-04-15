package de.peekandpoke.ultra.kontainer.examples

import de.peekandpoke.ultra.common.docs.ExamplesToDocs
import de.peekandpoke.ultra.kontainer.examples.defining_modules._DefiningModules
import de.peekandpoke.ultra.kontainer.examples.defining_services._DefiningServices
import de.peekandpoke.ultra.kontainer.examples.injecting_services._InjectingServices

fun main() {
    val generator = object : ExamplesToDocs(
        title = "Examples for ultra::kontainer",
        chapters = chapters
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
