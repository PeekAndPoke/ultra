package de.peekandpoke.mutator.examples.basic_mutations.basic_mutations

import de.peekandpoke.mutator.Mutable
import de.peekandpoke.ultra.common.docs.SimpleExample

class MutatingNestedDataClasses : SimpleExample() {

    // !BEGIN! //

    // We defined the data class
    @Mutable
    data class Person(
        val name: String,
        val age: Int,
        val address: Address,
    )

    @Mutable
    data class Address(
        val street: String,
        val city: String,
    )

    // !END! //

    override val title = "Mutating nested data classes"

    override val description = """
        This example shows how to mutate nested data classes.
    """.trimIndent()

    override fun run() {
        // !BEGIN! //

        // Get an instance
        val person = Person(name = "John", age = 42, address = Address(street = "street", city = "city"))

        // We mutate the instance directly
        val result = person.mutate {
            // Mutate the nested object directly
            address.street = "newStreet"
            // Or mutate it in the context of it's mutator
            address {
                city = "newCity"
            }
        }

        // Results in
        println("Original: $person")
        println()
        println("Mutated:  $result")

        // !END! //
    }
}
