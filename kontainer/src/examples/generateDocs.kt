package de.peekandpoke.ultra.kontainer.examples

import de.peekandpoke.ultra.common.docs.examplesToDocs
import de.peekandpoke.ultra.kontainer.examples._01_the_basics._01_TheBasics

fun main() {
    examplesToDocs(
        title = "Examples for ultra::kontainer",
        chapters = chapters
    )
}

val chapters = listOf(
    _01_TheBasics()
)
