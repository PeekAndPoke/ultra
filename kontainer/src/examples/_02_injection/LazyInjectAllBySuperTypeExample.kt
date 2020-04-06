package de.peekandpoke.ultra.kontainer.examples._02_injection

import de.peekandpoke.ultra.common.docs.SimpleExample
import de.peekandpoke.ultra.kontainer.kontainer

class LazyInjectAllBySuperTypeExample : SimpleExample() {

    override val title = "Lazily inject all Services By SuperType"

    override val description = """
        This example shows how to lazily inject all services of a given super type.
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
        class MyService(val repos: Lazy<List<Repository>>)

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
        myService.repos.value.forEach {
            println(it.name)
        }

        // !END! //
    }
}

