package de.peekandpoke.mutator.examples.basic_mutations

import de.peekandpoke.mutator.Mutable
import de.peekandpoke.ultra.common.docs.SimpleExample

class MutatingDataClassViaMutator : SimpleExample() {

    // !BEGIN! //

    // We defined the data class
    @Mutable
    data class Person(
        val name: String,
        val age: Int,
    )

    // !END! //

    override val title = "Mutating data classes using a Mutator"

    override val description = """
        This example shows how to apply the @Mutate annotation to a data class.
        
        We will then mutate the data class by creating and using a `mutator()`.
    """.trimIndent()

    override fun run() {
        // !BEGIN! //


        // Get an instance
        val person = Person("John", 42)

        // We mutate the instance directly
        val result = person.mutate {
            name = "Jane"
            age -= 10
        }

        // Results in
        println("Original: $person")
        println()
        println("Mutated:  $result")

        // !END! //
    }
}
