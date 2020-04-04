package de.peekandpoke.ultra.kontainer.examples._01_the_basics

import de.peekandpoke.ultra.common.docs.SimpleExample
import de.peekandpoke.ultra.kontainer.kontainer

@Suppress("ClassName", "ComplexRedundantLet")
class E04_SingletonVsDynamic : SimpleExample() {

    class SingletonCounter {
        private var count = 0

        fun next() = ++count
    }

    class DynamicCounter {
        private var count = 0

        fun next() = ++count
    }

    override val title = """
        This example demonstrates the difference between a singleton and a dynamic service.

        Singleton are instantiated only once. They are then shared across all kontainer instances.  
        Dynamic services are instantiated for each kontainer instance.

        You will see that the **SingletonCounter** is always increasing.  
        The **DynamicCounter** is created for each kontainer instance.
    """.trimIndent()

    override fun run() {
        // !BEGIN! //

        // 1. we create the kontainer blueprint
        val blueprint = kontainer {
            singleton(SingletonCounter::class)
            dynamic(DynamicCounter::class)
        }

        // 2. We get a kontainer instance and use the singleton
        blueprint.create().let { kontainer ->

            println("First kontainer instance:")
            println()

            // We are getting each service multiple times
            repeat(3) {
                println(
                    "SingletonCounter: ${kontainer.get(SingletonCounter::class).next()} -" +
                            "DynamicCounter: ${kontainer.get(DynamicCounter::class).next()}"
                )
            }
        }

        // 3. We get a another kontainer instance and use the singleton
        blueprint.create().let { kontainer ->

            println("Second kontainer instance:")
            println()

            // We are getting each service multiple times
            repeat(3) {
                println(
                    "SingletonCounter: ${kontainer.get(SingletonCounter::class).next()} - " +
                            "DynamicCounter: ${kontainer.get(DynamicCounter::class).next()}"
                )
            }
        }

        // !END! //
    }
}

