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

        You will see that:  
        Rhe **SingletonCounter** is globally created once and is always increasing.  
        The **DynamicCounter** is created once for each kontainer instance.  
        The **PrototypeCounter** is created every time it is requested from the kontainer.  
    """.trimIndent()

    override fun run() {
        // !BEGIN! //

        // 1. We define our services
        abstract class Counter {
            private var count = 0
            fun next() = ++count
        }

        class SingletonCounter : Counter()
        class DynamicCounter : Counter()
        class PrototypeCounter : Counter()

        // 2. We create the kontainer blueprint
        val blueprint = kontainer {
            // We register a singleton service
            singleton(SingletonCounter::class)
            // We register a dynamic service
            dynamic(DynamicCounter::class)
            // We register a prototype service
            prototype(PrototypeCounter::class)
        }

        // Let's create three kontainer instances
        (1..3).forEach { round ->

            println("Round #$round")

            val kontainer = blueprint.create()

            // We are getting each service multiple times
            repeat(3) {
                val singleton = kontainer.get(SingletonCounter::class).next()
                val dynamic = kontainer.get(DynamicCounter::class).next()
                val prototype = kontainer.get(PrototypeCounter::class).next()

                println("singleton: $singleton - dynamic $dynamic - prototype: $prototype")
            }
        }

        // !END! //
    }
}

