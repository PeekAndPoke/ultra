package de.peekandpoke.ultra.kontainer.examples.injecting_services

import de.peekandpoke.ultra.common.docs.SimpleExample
import de.peekandpoke.ultra.kontainer.kontainer

class InjectPrototypeIntoMultipleServicesExample : SimpleExample() {

    override val title = "Injecting a prototype into multiple services"

    override val description = """
        This example shows how a prototype service is injected into multiple services.
    """.trimIndent()

    override fun run() {
        // !BEGIN! //

        // 1. We define a service that will be injected
        class Counter {
            private var count = 0
            fun next() = ++count
        }

        // 2. We define two services that inject the counter service
        class One(val counter: Counter)
        class Two(val counter: Counter)

        // 3. We define the kontainer blueprint
        val blueprint = kontainer {
            // defining the injected service as a prototype
            prototype(Counter::class)
            // defining the consuming services
            singleton(One::class)
            singleton(Two::class)
        }

        // 3. We get the kontainer instance
        val kontainer = blueprint.create()

        // 4. We use the services and access the injected service
        val one = kontainer.get(One::class)
        val two = kontainer.get(Two::class)

        println("One: " + one.counter.next())
        println("Two: " + two.counter.next())

        // !END! //
    }
}
