package de.peekandpoke.ultra.kontainer.examples._02_injecting_services

import de.peekandpoke.ultra.common.docs.SimpleExample
import de.peekandpoke.ultra.kontainer.kontainer

class LazyInjectionCycleBreakerExample : SimpleExample() {

    override val title = "Breaking Cyclic Dependencies with Lazy Injection"

    override val description = """
        In some cases we have services that need to injected each other.
         
        This cyclic dependency can be broken with lazy injection. 
    """.trimIndent()

    // !BEGIN! //

    // We define two services that inject each other
    class ServiceOne(private val two: ServiceTwo) {
        val name = "one"
        fun sayHello() = "I am ServiceOne and I know '${two.name}'"
    }

    class ServiceTwo(private val one: Lazy<ServiceOne>) {
        val name = "two"
        fun sayHello() = "I am ServiceTwo and I know '${one.value.name}'"
    }

    // !END! //

    override fun run() {

        // !BEGIN! //

        // We define the kontainer blueprint
        val blueprint = kontainer {
            singleton(ServiceOne::class)
            singleton(ServiceTwo::class)
        }

        // We get the kontainer instance
        val kontainer = blueprint.create()

        // We use both services
        val one = kontainer.get(ServiceOne::class)
        val two = kontainer.get(ServiceTwo::class)

        println(one.sayHello())
        println(two.sayHello())

        // !END! //
    }
}

