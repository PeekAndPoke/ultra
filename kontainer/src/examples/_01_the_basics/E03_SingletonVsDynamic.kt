package de.peekandpoke.ultra.kontainer.examples._01_the_basics

import de.peekandpoke.ultra.common.docs.SimpleExample
import de.peekandpoke.ultra.kontainer.kontainer

@Suppress("ClassName", "ComplexRedundantLet")
class E03_SingletonVsDynamic : SimpleExample() {

    override val title = "Singletons vs Dynamic Services"

    override val description = """
        This example demonstrates the difference between a singleton and a dynamic service.

        Singleton are instantiated only once. They are then shared across all kontainer instances.  
        Dynamic services are instantiated for each kontainer instance.

        You will see that the **SingletonCounter** is always increasing.  
        The **DynamicCounter** is created for each kontainer instance.
    """.trimIndent()

    override fun run() {
        // !BEGIN! //

        // 1. We define our services
        class SingletonCounter {
            private var count = 0
            fun next() = ++count
        }

        class DynamicCounter {
            private var count = 0
            fun next() = ++count
        }

        // 2. we create the kontainer blueprint
        val blueprint = kontainer {
            singleton(SingletonCounter::class)
            dynamic(DynamicCounter::class)
        }

        // 3. We get a kontainer instance and use the singleton
        blueprint.create().let { kontainer ->

            println("First kontainer instance:")

            // We are getting each service multiple times
            repeat(3) {
                val singleton = kontainer.get(SingletonCounter::class).next()
                val dynamic = kontainer.get(DynamicCounter::class).next()

                println("singleton: $singleton - dynamic $dynamic")
            }
        }

        // 4. We get a another kontainer instance and use the singleton
        blueprint.create().let { kontainer ->

            println("Second kontainer instance:")

            // We are getting each service multiple times
            repeat(3) {
                val singleton = kontainer.get(SingletonCounter::class).next()
                val dynamic = kontainer.get(DynamicCounter::class).next()

                println("singleton: $singleton - dynamic $dynamic")
            }
        }

        // !END! //
    }
}

