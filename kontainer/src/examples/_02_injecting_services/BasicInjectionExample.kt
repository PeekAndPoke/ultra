package de.peekandpoke.ultra.kontainer.examples._02_injecting_services

import de.peekandpoke.ultra.common.docs.SimpleExample
import de.peekandpoke.ultra.kontainer.kontainer

class BasicInjectionExample : SimpleExample() {

    override val title = "Basic Injection Example"

    override val description = """
        This example shows how a service can inject another service.
        
        For simplicity there are two ways of injection:
        1. Constructor injection.
        2. Factory method injection, which we will see next
    """.trimIndent()

    override fun run() {
        // !BEGIN! //

        // We define a service that will be injected
        class Counter {
            private var count = 0
            fun next() = ++count
        }

        // We define a service that injects another service in it's constructor
        class MyService(val counter: Counter)

        // We define the kontainer blueprint
        val blueprint = kontainer {
            singleton(MyService::class)
            singleton(Counter::class)
        }

        // We get the kontainer instance
        val kontainer = blueprint.create()

        // We use the service and access the injected service
        val myService = kontainer.get(MyService::class)

        println("Next: " + myService.counter.next())
        println("Next: " + myService.counter.next())

        // !END! //
    }
}

