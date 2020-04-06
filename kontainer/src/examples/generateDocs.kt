package de.peekandpoke.ultra.kontainer.examples

import de.peekandpoke.ultra.common.docs.examplesToDocs
import de.peekandpoke.ultra.kontainer.examples._01_the_basics._01_TheBasics
import de.peekandpoke.ultra.kontainer.examples._02_injection._02_Injection

fun main() {
    examplesToDocs(
        title = "Examples for ultra::kontainer",
        chapters = chapters
    )
}

val chapters = listOf(
    _01_TheBasics(),
    _02_Injection()
    // TODO: kontainer modules
    // TODO: definition order / overwriting services
    // TODO: dynamic promotion / preventing degradation
    // TODO: meta information / debug
    // TODO: advanced topics / inject Kontainer, Blueprint, InjectionContext
)
