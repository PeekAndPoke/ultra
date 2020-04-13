package de.peekandpoke.ultra.mutator.examples.dataclasses

import de.peekandpoke.ultra.common.docs.ExampleChapter

@Suppress("ClassName")
class _DataClassMutation : ExampleChapter {

    override val title = "Data Class Mutation"

    override val description = """
        Mutator supports the mutation of data classes.
        
        This chapter shows how to mutate immutable data classes and nested data classes.
    """.trimIndent()

    override val examples = listOf(
        ScalarAndStringPropertiesExample()
    )
}
