package de.peekandpoke.ultra.kontainer.examples.defining_services

import de.peekandpoke.ultra.common.docs.SimpleExample
import de.peekandpoke.ultra.kontainer.kontainer

@Suppress("ComplexRedundantLet")
class DefiningAPrototypeExample : SimpleExample() {

    override val title = "Defining a Prototype service"

    override val description = """
        This example shows how to register and retrieve a prototype service.
        
        Prototype services create a new instance whenever they are requested from the kontainer.  
        Be it through direct retrieval from the kontainer or through injection.
    """.trimIndent()

    override fun run() {
        // !BEGIN! //

        // 1. We define a service class
        class Greeter {
            fun sayHello() = "Hello you!"
        }

        // 2. We create a kontainer blueprint
        val blueprint = kontainer {
            prototype(Greeter::class)
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

