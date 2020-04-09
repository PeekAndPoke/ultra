package de.peekandpoke.ultra.kontainer.examples

import de.peekandpoke.ultra.common.docs.ExamplesToDocs
import de.peekandpoke.ultra.kontainer.examples._01_defining_services._01_DefiningServices
import de.peekandpoke.ultra.kontainer.examples._02_injecting_services._02_InjectingServices

fun main() {
    val generator = object : ExamplesToDocs(
        title = "Examples for ultra::kontainer",
        chapters = chapters
    ) {}

    generator.run()
}

val chapters = listOf(
    _01_DefiningServices(),
    _02_InjectingServices()
    // TODO: kontainer modules
    // TODO: definition order / overwriting services
    // TODO: dynamic promotion / preventing degradation
    // TODO: meta information / debug
    // TODO: advanced topics / inject Kontainer, Blueprint, InjectionContext
)
