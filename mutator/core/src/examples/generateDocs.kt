package de.peekandpoke.mutator.examples

import de.peekandpoke.mutator.examples.basic_mutations._BasicMutationsChapters
import de.peekandpoke.mutator.examples.code_generation._CodeGenerationChapters
import de.peekandpoke.ultra.common.docs.ExamplesToDocs
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
