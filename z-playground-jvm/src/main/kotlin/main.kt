package de.peekandpoke.ultra.playground

import de.peekandpoke.ultra.kontainer.kontainer

fun main() {

    // We define a service that will be injected
    class Counter {
        private var count = 0
        fun next() = ++count
    }

    // We define a service that injects another service in it's constructor.
    // But this time this service also expects a second parameter that cannot be provided by the kontainer.
    class MyService(private val counter: Counter, private val offset: Int) {
        fun next() = counter.next() + offset
    }

    // We define the kontainer blueprint
    val blueprint = kontainer {
        // We define the service using a factory method.
        // Injection is now only done for all parameters of the factory method.
        singleton { counter: Counter ->
            MyService(counter, 100)
        }

        singleton(Counter::class)
    }

    // We get the kontainer instance
    val kontainer = blueprint.create()

    // We use the service
    val myService = kontainer.get(MyService::class)

    println("Next: " + myService.next())
    println("Next: " + myService.next())

}
