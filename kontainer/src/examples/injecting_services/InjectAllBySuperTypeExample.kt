package de.peekandpoke.ultra.kontainer.examples.injecting_services

import de.peekandpoke.ultra.common.docs.SimpleExample
import de.peekandpoke.ultra.kontainer.kontainer

class InjectAllBySuperTypeExample : SimpleExample() {

    override val title = "Injection of all Services By SuperType"

    override val description = """
        This example shows how to inject all services that implement or extend a given super type.
        
        This is very useful when we design systems for extensibility.
        
        For Example:  
        Let's say we have a Database service that injects all registered Repositories.
        Repositories can then even by added by code that is not maintained by us. 
    """.trimIndent()

    // !BEGIN! //

    // We define an interface for all repositories
    interface Repository {
        val name: String
    }

    // We create some implementations
    class UserRepository : Repository {
        override val name = "users"
    }

    class OrderRepository : Repository {
        override val name = "orders"
    }

    // !END! //

    override fun run() {
        // !BEGIN! //

        // We inject all Repositories into our service
        class MyService(val repos: List<Repository>)

        // We define the kontainer blueprint
        val blueprint = kontainer {
            singleton(MyService::class)

            singleton(UserRepository::class)
            singleton(OrderRepository::class)
        }

        // We get the kontainer instance
        val kontainer = blueprint.create()

        // We use the service
        val myService = kontainer.get(MyService::class)

        // We get all injected Repos
        myService.repos.forEach {
            println(it.name)
        }

        // !END! //
    }
}

