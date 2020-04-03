package de.peekandpoke.ultra.kontainer.examples

import de.peekandpoke.ultra.common.examples.Example
import de.peekandpoke.ultra.common.examples.Runner

fun main() {

    Runner(
        listOf(
            Example001(),
            Example002()
        )
    ).run()
}

fun Example.codeUrl() =
    "https://github.com/PeekAndPoke/ultra/tree/master/kontainer/src/examples/kotlin/${this::class.simpleName}.kt"
