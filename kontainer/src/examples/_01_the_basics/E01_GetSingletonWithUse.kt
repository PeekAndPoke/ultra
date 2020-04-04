package de.peekandpoke.ultra.kontainer.examples._01_the_basics

import de.peekandpoke.ultra.common.docs.SimpleExample
import de.peekandpoke.ultra.kontainer.kontainer

@Suppress("ClassName")
class E01_GetSingletonWithUse : SimpleExample() {

    override val title = "Simple Singleton Example"

    override val description = """
        This example show how to register and retrieve a simple singleton service.
        
        Services can be retrieved by:
        1. kontainer.get(...)
        2. kontainer.use(...)
    """.trimIndent()

    override fun run() {
        // !BEGIN! //

        // 1. We define a service class
        class Greeter {
            fun sayHello() = "Hello you!"
        }

        // 2. We create a kontainer blueprint
        val blueprint = kontainer {
            singleton(Greeter::class)
        }

        // 3. We get the kontainer instance
        val kontainer = blueprint.create()

        // 4. We can retrieve our service by kontainer.use(...).
        //    And we will have the service as "this" inside of the closure.
        //
        // NOTICE: If the service was not present in the kontainer, the closure would not be executed.
        //         This is useful, when we only want to execute some code, when a service exists.
        kontainer.use(Greeter::class) {
            println("Kontainer.use() says: ${sayHello()}")
        }

        // 5. We can also retrieve the service by kontainer.get(...)
        //
        // NOTICE: If the service was not present in the kontainer, an exception would be thrown.
        println(
            "Kontainer.get() says: ${kontainer.get(Greeter::class).sayHello()}"
        )

        // !END! //
    }
}

