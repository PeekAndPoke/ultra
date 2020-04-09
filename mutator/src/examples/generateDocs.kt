package de.peekandpoke.ultra.mutator.examples

import de.peekandpoke.ultra.common.docs.ExamplesToDocs
import de.peekandpoke.ultra.mutator.examples.introduction._Introduction

fun main() {
    val generator = object : ExamplesToDocs(
        title = "Examples for ultra::mutator",
        chapters = chapters
    ) {}

    generator.run()
}

val chapters = listOf(
    _Introduction()
)
