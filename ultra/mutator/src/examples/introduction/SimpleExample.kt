package de.peekandpoke.ultra.mutator.examples.introduction

import de.peekandpoke.ultra.common.docs.SimpleExample
import de.peekandpoke.ultra.mutator.Mutable

@Mutable
class SimpleExample : SimpleExample() {

    override val title = "A simple example"

    override val description = """
        Let us start with a very simple example.
    """.trimIndent()

    // !BEGIN! //

    // Let's say we have a data class that has some fields.
    // We also annotate the type, so the mutator annotation processing will generate some code for us.
    @Mutable
    data class Person(val name: String, val age: Int)

    // !END! //

    override fun run() {
        // !BEGIN! //

        // We create an instance of our data class.
        val angelina = Person("Angelina", 35)

        // Through annotation processing we get the "mutate" extension method created.
        // The result of a mutation is a new object, while keeping the old one as it is.
        val olderAngelina = angelina.mutate {
            // Inside of the callback we can directly manipulate properties of the object
            age += 1
        }

        // Angelina did not change.
        println("The original:")
        println(angelina)
        // But we got another version with increased age.
        println("The new version with modified age:")
        println(olderAngelina)

        // We could also change the name or both.
        val noMoreAngelina = angelina.mutate {
            age = 47
            name = "Brad"
        }

        // And so we got another mutation of the original object.
        println("The renamed version:")
        println(noMoreAngelina)

        // !END! //
    }
}
