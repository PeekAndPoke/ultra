package de.peekandpoke.ultra.kontainer.examples

import de.peekandpoke.ultra.common.examples.Example
import de.peekandpoke.ultra.kontainer.kontainer

class Example002 : Example {

    class Greeter {
        fun sayHello() = "Hello you!"
    }

    override val title = """
        Registering and retrieving a singleton service with kontainer.get(...)
        
        Code: ${codeUrl()}
    """.trimIndent()

    override fun run() {

        // 1. we create the kontainer blueprint
        val blueprint = kontainer {
            singleton(Greeter::class)
        }

        // 2. we get the kontainer instance
        val kontainer = blueprint.create()

        // 3. we retrieve and use a service
        println(
            kontainer.get(Greeter::class).sayHello()
        )
    }
}

