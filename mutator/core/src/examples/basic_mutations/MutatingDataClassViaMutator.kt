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

        // Create the mutator
        val mutator = person.mutator()

        // We can invoke the mutator ...
        mutator {
            name = "Jane"
        }

        // Or we can change properties directly ...
        mutator.age -= 10

        // Get the result
        val result = mutator()
        val isModified = mutator.isModified()

        // Results in
        println("Is modified: $isModified")
        println()
        println("Original: $person")
        println()
        println("Mutated:  $result")

        // !END! //
    }
}
