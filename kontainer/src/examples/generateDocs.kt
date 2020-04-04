package de.peekandpoke.ultra.kontainer.examples

import de.peekandpoke.ultra.common.docs.ExamplesToDocs
import de.peekandpoke.ultra.kontainer.examples._01_the_basics._01_TheBasics

fun main() {
    ExamplesToDocs(
        chapters = chapters
    ).run()
}

val chapters = listOf(
    _01_TheBasics()
)
