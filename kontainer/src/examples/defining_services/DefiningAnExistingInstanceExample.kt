package de.peekandpoke.ultra.kontainer.examples.defining_services

import de.peekandpoke.ultra.common.docs.SimpleExample
import de.peekandpoke.ultra.kontainer.kontainer

@Suppress("ComplexRedundantLet")
class DefiningAnExistingInstanceExample : SimpleExample() {

    override val title = "Defining an existing object as a Singleton service"

    override val description = """
        This example shows how to register an existing object as a singleton service.
    """.trimIndent()

    // !BEGIN! //

    // 1. Let's say we have some existing object
    object Greeter {
        fun sayHello() = "Hello you!"
    }

    // !END! //

    override fun run() {
        // !BEGIN! //

        // 2. We create a kontainer blueprint
        val blueprint = kontainer {
            instance(Greeter)
        }

        // 3. We get the kontainer instance
        val kontainer = blueprint.create()

        // 4. We can retrieve our service by kontainer.use(...).
        //    And we will have the service as "this" inside of the closure.
        kontainer.use(Greeter::class) {
            println("Kontainer.use() says: ${sayHello()}")
        }

        // !END! //
    }
}
