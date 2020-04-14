package de.peekandpoke.ultra.mutator.examples.dataclasses

import de.peekandpoke.ultra.common.docs.ExampleChapter

@Suppress("ClassName")
class _DataClassMutation : ExampleChapter {

    override val title = "Data Class Mutation"

    override val description = """
        **ultra::mutator** supports the mutation of data classes.
        
        This chapter shows how to mutate immutable data classes and nested data classes.
        
        Notice that inside all the mutate { } closures, we are not working on our objects directly. We rather work on
        wrappers. The code for these wrappers is generated for us, as we put the @Mutable annotation on our classes.
    """.trimIndent()

    override val examples = listOf(
        ScalarAndStringPropertiesExample(),
        NullableScalarAndStringPropertiesExample(),
        // TODO: nested mutable data classes
        AnyPropertiesExample()
    )
}
