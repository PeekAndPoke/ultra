package de.peekandpoke.ultra.kontainer.examples._01_defining_services

import de.peekandpoke.ultra.common.docs.SimpleExample
import de.peekandpoke.ultra.kontainer.ServiceNotFound
import de.peekandpoke.ultra.kontainer.kontainer

@Suppress("ComplexRedundantLet")
class HidingTheConcreteImplementationOfAServiceExample : SimpleExample() {

    override val title = "Hiding the concrete implementation of a service"

    override val description = """
        This example shows how to hide the concrete implementation of a service.
        
        The same mechanism is available for all types of service registration:
        - singleton
        - dynamic
        - prototype
        - instance
        - factory methods
    """.trimIndent()

    // !BEGIN! //

    // Let's say we have an interface that defines one of our services.
    interface GreeterInterface {
        fun sayHello(): String
    }

    // And we have an implementation.
    class Greeter : GreeterInterface {
        override fun sayHello() = "Hello you!"
    }

    // !END! //

    override fun run() {
        // !BEGIN! //

        val blueprint = kontainer {
            // Then we can define the service the following way, which means:
            // The kontainer will only know that there is a service of type GreeterInterface.
            singleton(GreeterInterface::class, Greeter::class)
        }

        val kontainer = blueprint.create()

        // We can now retrieve the GreeterInterface service and use it.
        println(
            "It says: ${kontainer.get(GreeterInterface::class).sayHello()}"
        )

        // But when we try to retrieve the concrete implementation we will get an error.
        try {
            println(
                "It says: ${kontainer.get(Greeter::class).sayHello()}"
            )
        } catch (e: ServiceNotFound) {
            println(e)
        }

        // !END! //
    }
}

