package de.peekandpoke.ultra.mutator.examples.introduction

import de.peekandpoke.ultra.common.docs.SimpleExample
import de.peekandpoke.ultra.mutator.Mutable

@Mutable
class SimpleExample : SimpleExample() {

    override val title = "A very simple"

    override val description = """
        Let us start with a very simple example.
    """.trimIndent()

    // !BEGIN! //

    // We have a data class that has some fields.
    // We also have the mutator annotation.
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

        // Angela did not change.
        println("The original:")
        println(angelina)
        // But we go the older version through mutation.
        println("The older version:")
        println(olderAngelina)

        // We could also change the name or both.
        val noMoreAngela = angelina.mutate {
            age = 47
            name = "Brad"
        }

        // And so we got another mutation of the original object.
        println("The renamed version:")
        println(noMoreAngela)

        // !END! //
    }
}

