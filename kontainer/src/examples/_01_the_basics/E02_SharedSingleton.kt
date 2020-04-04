package de.peekandpoke.ultra.kontainer.examples._01_the_basics

import de.peekandpoke.ultra.common.docs.SimpleExample
import de.peekandpoke.ultra.kontainer.kontainer

@Suppress("ClassName", "ComplexRedundantLet")
class E02_SharedSingleton : SimpleExample() {

    class Counter {
        private var count = 0

        fun next() = ++count
    }

    override val title = "Shared Singleton Services"

    override val description = """
        This example demonstrates that singleton services are shared across all Kontainers, that where
        created from the same Blueprint. 
    """.trimIndent()

    override fun run() {
        // !BEGIN! //

        // 1. we create the kontainer blueprint
        val blueprint = kontainer {
            singleton(Counter::class)
        }

        // 2. We get a kontainer instance and use the singleton
        blueprint.create().let { kontainer ->
            println(
                "Counter: ${kontainer.get(Counter::class).next()}"
            )
        }

        // 3. We get a another kontainer instance and use the singleton
        blueprint.create().let { kontainer ->
            println(
                "Counter: ${kontainer.get(Counter::class).next()}"
            )
        }

        // !END! //
    }
}

