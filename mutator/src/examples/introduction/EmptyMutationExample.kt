package de.peekandpoke.ultra.mutator.examples.introduction

import de.peekandpoke.ultra.common.docs.SimpleExample
import de.peekandpoke.ultra.mutator.Mutable

@Mutable
class EmptyMutationExample : SimpleExample() {

    override val title = "Empty Mutation"

    override val description = """
        This example shows what happens when we apply an empty mutation.
        
        When we call **mutate** on an object without changing anything, the result will be 
        the exact same object.
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

