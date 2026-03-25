package io.peekandpoke.mutator.examples

import io.peekandpoke.mutator.examples.basic_mutations._BasicMutationsChapters
import io.peekandpoke.mutator.examples.code_generation._CodeGenerationChapters
import io.peekandpoke.ultra.tooling.ExamplesToDocs
import java.io.File

fun main() {
    val generator = object : ExamplesToDocs(
        title = "Examples for Mutator",
        chapters = chapters,
        sourceLocation = File("src/examples"),
        outputLocation = File("docs/mutator::docs"),
    ) {}

    generator.run()
}

val chapters = listOf(
    _BasicMutationsChapters(),
    _CodeGenerationChapters(),
)
