package de.peekandpoke.mutator.examples.basic_mutations

import de.peekandpoke.ultra.common.docs.ExampleChapter

@Suppress("ClassName")
class _BasicMutationsChapters : ExampleChapter {

    override val title = "Basic Mutator Concepts"

    override val examples = listOf(
        MutatingDataClassDirectly(),
        MutatingDataClassViaMutator(),
    )
}
