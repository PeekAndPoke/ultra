package de.peekandpoke.ultra.kontainer.examples._02_injection

import de.peekandpoke.ultra.common.docs.SimpleExample
import de.peekandpoke.ultra.kontainer.kontainer

fun main() {
    LazyInjectionExample().run()
}

class LazyInjectionExample : SimpleExample() {

    override val title = "Lazy Injection Example"

    override val description = """
        This example shows how a to lazily inject a service.
        
        This means that the injected service will only be instantiated when it used for the first time. 
    """.trimIndent()

    override fun run() {

        // !BEGIN! //

        // We define a service that will be injected
        class LazilyInjected {
            fun sayHello() = "Here I am!"
        }

        // We define a service that lazily injects another service in it's constructor
        class MyService(private val lazy: Lazy<LazilyInjected>) {
            fun sayHello() = lazy.value.sayHello()
        }

        // We define the kontainer blueprint
        val blueprint = kontainer {
            singleton(MyService::class)
            singleton(LazilyInjected::class)
        }

        // We get the kontainer instance
        val kontainer = blueprint.create()

        // We use the service and access the injected service
        val myService = kontainer.get(MyService::class)

        println("We got MyService from the kontainer")
        println("# instances of the LazilyInjected:   ${kontainer.getProvider(LazilyInjected::class).instances.size}")

        println("We used the lazily injected service: ${myService.sayHello()}")
        println("# instances of the LazilyInjected:   ${kontainer.getProvider(LazilyInjected::class).instances.size}")

        // !END! //
    }
}

