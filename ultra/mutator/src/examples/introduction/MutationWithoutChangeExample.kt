package de.peekandpoke.ultra.mutator.examples.introduction

import de.peekandpoke.ultra.common.docs.SimpleExample
import de.peekandpoke.ultra.mutator.Mutable

@Mutable
class MutationWithoutChangeExample : SimpleExample() {

    override val title = "Mutating an object without changing it"

    override val description = """
        This example shows what happens when we mutate an object without changing any of it's fields.
        
        Re-assign fields with the exact same values, behaves as if no mutation is done. This means:
        - the result will be the original object.
        - there is no new object created.
        
        The same behavior is implemented for other kinds of mutations as well, like:
        - List mutation
        - Map mutation
        - ...
        
        Only real changes lead to the creation of object copies.
    """.trimIndent()

    // !BEGIN! //

    @Mutable
    data class Person(val name: String, val age: Int)

    // !END! //

    override fun run() {
        // !BEGIN! //

        val before = Person("Angelina", 35)

        // We call mutate but we do not do anything
        val after = before.mutate {
            name = "Angelina"
            age = 35
        }

        println("Before:")
        println(before)
        println()

        println("After:")
        println(after)
        println()

        println("It is the exact same object (before === after): " + (before === after))

        // !END! //
    }
}
