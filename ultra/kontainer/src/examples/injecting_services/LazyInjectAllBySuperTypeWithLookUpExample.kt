package de.peekandpoke.ultra.kontainer.examples.injecting_services

import de.peekandpoke.ultra.common.Lookup
import de.peekandpoke.ultra.common.docs.SimpleExample
import de.peekandpoke.ultra.kontainer.kontainer

class LazyInjectAllBySuperTypeWithLookUpExample : SimpleExample() {

    override val title = "Lazily inject all Services By SuperType with a Lookup"

    override val description = """
        This example shows how to lazily inject all services of a given super type using a lookup.
        
        What is this good for? 
        
        By using a lazy lookup we can inject many services without instantiating them.  
        We can get individual services from the LookUp by their class.
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

        // We inject all Repositories into our service as a Lookup
        class MyService(val repos: Lookup<Repository>)

        // We define the kontainer blueprint
        val blueprint = kontainer {
            singleton(MyService::class)

            dynamic(UserRepository::class)
            dynamic(OrderRepository::class)
        }

        // We get the kontainer instance
        val kontainer = blueprint.create()

        // We use the service
        val myService = kontainer.get(MyService::class)

        // UserRepository is not yet instantiated
        println("# instances of UserRepository ${kontainer.getProvider(UserRepository::class).instances.size}")
        // We get is from the Lookup
        println("Getting it from the Lookup: " + myService.repos.get(UserRepository::class).name)
        // It is now instantiated
        println("# instances of UserRepository ${kontainer.getProvider(UserRepository::class).instances.size}")

        // OrderRepository is not yet instantiated
        println("# instances of OrderRepository ${kontainer.getProvider(OrderRepository::class).instances.size}")
        // We get is from the Lookup
        println("Getting it from the Lookup: " + myService.repos.get(OrderRepository::class).name)
        // It is now instantiated
        println("# instances of OrderRepository ${kontainer.getProvider(OrderRepository::class).instances.size}")

        // !END! //
    }
}
