package de.peekandpoke.ultra.kontainer.examples._01_the_basics

import de.peekandpoke.ultra.common.docs.SimpleExample
import de.peekandpoke.ultra.kontainer.kontainer

@Suppress("ClassName")
class E01_GetSingletonWithUse : SimpleExample() {

    class Greeter {
        fun sayHello() = "Hello you!"
    }

    override val title = """
        Registering and retrieving a singleton service with kontainer.use(...)
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
        kontainer.use(Greeter::class) {
            println("The service says: ${sayHello()}")
        }

        // !END! //
    }
}

