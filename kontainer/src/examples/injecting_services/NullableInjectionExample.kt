package de.peekandpoke.ultra.kontainer.examples.injecting_services

import de.peekandpoke.ultra.common.docs.SimpleExample
import de.peekandpoke.ultra.kontainer.kontainer

class NullableInjectionExample : SimpleExample() {

    override val title = "Nullable Injection"

    override val description = """
        This example shows how to inject a service only when it is present in the kontainer.
        
        This can be done by marking the injected parameter as nullable.
    """.trimIndent()

    override fun run() {
        // !BEGIN! //

        // We define a service that will NOT be registered in the kontainer
        class NotRegisteredInKontainer

        // We define a service that injects the other service as nullable
        class FirstService(val injected: NotRegisteredInKontainer?)

        // We define another one to demonstrate the same behaviour with a factory method
        class SecondService(val injected: NotRegisteredInKontainer?)

        // We define the kontainer blueprint
        val blueprint = kontainer {
            // We define the first service as a singleton
            singleton(FirstService::class)
            // We define the other service with a factory method (notice the nullable closure parameter)
            singleton { injected: NotRegisteredInKontainer? ->
                SecondService(injected)
            }
        }

        // We get the kontainer instance
        val kontainer = blueprint.create()

        // We use the service and access the injected service
        val firstService = kontainer.get(FirstService::class)
        println("FirstService.injected: " + firstService.injected)

        val secondService = kontainer.get(SecondService::class)
        println("SecondService.injected: " + secondService.injected)

        // !END! //
    }
}
