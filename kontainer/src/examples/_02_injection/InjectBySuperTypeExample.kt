package de.peekandpoke.ultra.kontainer.examples._02_injection

import de.peekandpoke.ultra.common.docs.SimpleExample
import de.peekandpoke.ultra.kontainer.kontainer

class InjectBySuperTypeExample : SimpleExample() {

    override val title = "Injection By SuperType"

    override val description = """
        This example shows how a service can be injected by one of its supertypes.
    """.trimIndent()

    // !BEGIN! //

    // We define a service that extends or implements a super type
    interface CounterInterface {
        fun next(): Int
    }

    class Counter : CounterInterface {
        private var count = 0
        override fun next() = ++count
    }

    // !END! //

    override fun run() {
        // !BEGIN! //

        // We inject a service by it's interface
        class MyService(val counter: CounterInterface)

        // We define the kontainer blueprint
        val blueprint = kontainer {
            singleton(Counter::class)
            singleton(MyService::class)
        }

        // We get the kontainer instance
        val kontainer = blueprint.create()

        // We use the service and access the injected service
        val myService = kontainer.get(MyService::class)

        println("Next: " + myService.counter.next())

        // !END! //
    }
}

