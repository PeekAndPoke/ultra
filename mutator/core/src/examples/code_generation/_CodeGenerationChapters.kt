package io.peekandpoke.mutator.examples.code_generation

import io.peekandpoke.ultra.tooling.ExampleChapter

@Suppress("ClassName")
class _CodeGenerationChapters : ExampleChapter {

    override val title = "Code Generation"

    override val examples = listOf(
        CodeGeneratedForDataClasses(),
    )
}
