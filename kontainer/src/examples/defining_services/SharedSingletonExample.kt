package de.peekandpoke.ultra.kontainer.examples.defining_services

import de.peekandpoke.ultra.common.docs.SimpleExample
import de.peekandpoke.ultra.kontainer.kontainer

@Suppress("ComplexRedundantLet")
class SharedSingletonExample : SimpleExample() {

    override val title = "Singletons are shared"

    override val description = """
        This example shows that singleton services are shared across all kontainers, 
        that are created from the same blueprint. 
    """.trimIndent()

    override fun run() {
        // !BEGIN! //

        // 1. We define our service
        class Counter {
            private var count = 0
            fun next() = ++count
        }

        // 2. we create the kontainer blueprint
        val blueprint = kontainer {
            singleton(Counter::class)
        }

        // 3. We get a kontainer instance and use the singleton
        blueprint.create().let { kontainer ->
            println("Counter: ${kontainer.get(Counter::class).next()}")
        }

        // 4. We get another kontainer instance and use the singleton
        blueprint.create().let { kontainer ->
            println("Counter: ${kontainer.get(Counter::class).next()}")
        }

        // !END! //
    }
}

