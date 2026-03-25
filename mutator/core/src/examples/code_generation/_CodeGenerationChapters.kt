package de.peekandpoke.mutator.examples.code_generation

import de.peekandpoke.ultra.tooling.ExampleChapter

@Suppress("ClassName")
class _CodeGenerationChapters : ExampleChapter {

    override val title = "Code Generation"

    override val examples = listOf(
        CodeGeneratedForDataClasses(),
    )
}
