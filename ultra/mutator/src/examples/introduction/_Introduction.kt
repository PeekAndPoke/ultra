package de.peekandpoke.ultra.mutator.examples.introduction

import de.peekandpoke.ultra.common.docs.ExampleChapter

@Suppress("ClassName")
class _Introduction : ExampleChapter {

    override val title = "Introduction"

    override val examples = listOf(
        SimpleExample(),
        MoreComplexExample(),
        EmptyMutationExample(),
        MutationWithoutChangeExample()
    )
}
