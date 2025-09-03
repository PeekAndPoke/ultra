package de.peekandpoke.mutator.examples.basic_mutations

import de.peekandpoke.mutator.Mutable
import de.peekandpoke.ultra.common.docs.SimpleExample

class MutatingListsOfDataClasses : SimpleExample() {

    // !BEGIN! //

    // We defined the data class
    @Mutable
    data class Person(
        val name: String,
        val age: Int,
    )

    // !END! //

    override val title = "Mutating lists of data classes"

    override val description = """
        This example shows how to mutate lists of data classes.
    """.trimIndent()

    override fun run() {
        // !BEGIN! //

        // Get an instance
        val persons = listOf(
            Person("John", 42),
            Person("Jane", 40),
            Person("Jill", 30),
            Person("Jack", 20),
        )

        // We mutate the instance directly
        val result = persons.mutate {
            // We only keep persons with age <= 30
            retainAll { it.age <= 30 }

            // And then we modify them
            forEach { it.name += "-changed" }
        }

        // Results in
        println("Original:")
        persons.forEach { println("  - $it") }
        println()
        println("Mutated:")
        result.forEach { println("  - $it") }

        // !END! //
    }
}
