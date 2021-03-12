package de.peekandpoke.ultra.kontainer.examples.injecting_services

import de.peekandpoke.ultra.common.docs.SimpleExample
import de.peekandpoke.ultra.kontainer.KontainerInconsistent
import de.peekandpoke.ultra.kontainer.kontainer

@Suppress("unused")
class InjectBySuperTypeFailsToAmbiguityExample : SimpleExample() {

    override val title = "Injection By SuperType can fail due to ambiguity"

    override val description = """
        This example shows that injecting by supertype can fail, when there is an ambiguity.
    """.trimIndent()

    // !BEGIN! //

    // We define a supertype
    interface CounterInterface {
        fun next(): Int
    }

    // We create two implementations of the interface
    class CounterOne : CounterInterface {
        private var count = 0
        override fun next() = ++count
    }

    class CounterTwo : CounterInterface {
        private var count = 0
        override fun next() = ++count
    }

    // We try to inject the supertype CounterInterface
    class MyService(val counter: CounterInterface)

    // !END! //

    override fun run() {
        // !BEGIN! //

        val blueprint = kontainer {
            // We register our service
            singleton(MyService::class)
            // We register both implementations of the CounterInterface
            singleton(CounterOne::class)
            singleton(CounterTwo::class)
        }

        // When we try to create a kontainer instance we will get an error
        try {
            blueprint.create()
        } catch (e: KontainerInconsistent) {
            println(e)
        }

        // !END! //
    }
}
