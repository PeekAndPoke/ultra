package de.peekandpoke.ultra.kontainer.examples

import de.peekandpoke.ultra.common.examples.Example
import de.peekandpoke.ultra.kontainer.kontainer

class Example001 : Example {

    class Greeter {
        fun sayHello() = "Hello you!"
    }

    override val title = """
        Registering and retrieving a singleton service with kontainer.use(...)
        
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
        kontainer.use(Greeter::class) {
            println(sayHello())
        }
    }
}

