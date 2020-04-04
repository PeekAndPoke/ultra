package de.peekandpoke.ultra.kontainer.examples._01_the_basics

import de.peekandpoke.ultra.common.docs.SimpleExample
import de.peekandpoke.ultra.kontainer.kontainer

@Suppress("ClassName")
class E02_GetSingletonWithGet : SimpleExample() {

    class Greeter {
        fun sayHello() = "Hello you!"
    }

    override val title = """
        Registering and retrieving a singleton service with kontainer.get(...)
    """.trimIndent()

    override fun run() {
        // !BEGIN! //

        // 1. we create the kontainer blueprint
        val blueprint = kontainer {
            singleton(Greeter::class)
        }

        // 2. we get the kontainer instance
        val kontainer = blueprint.create()

        // 3. we retrieve and use a service
        println(
            "The service says: ${kontainer.get(Greeter::class).sayHello()}"
        )

        // !END! //
    }
}

