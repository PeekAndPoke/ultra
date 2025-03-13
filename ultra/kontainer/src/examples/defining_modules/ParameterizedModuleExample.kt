package de.peekandpoke.ultra.kontainer.examples.defining_modules

import de.peekandpoke.ultra.common.docs.SimpleExample
import de.peekandpoke.ultra.kontainer.kontainer
import de.peekandpoke.ultra.kontainer.module

class ParameterizedModuleExample : SimpleExample() {

    override val title = "Parameterized modules"

    override val description = """
        This example shows how to define a parameterized kontainer module.
        
        A module can take up to three parameters.
    """.trimIndent()

    override fun run() {
        // !BEGIN! //

        class Service(val sum: Int)

        // We can now define a kontainer module with up to three parameters like this
        // The parameters can be of any type. For simplicity of the example we only use Ints here.
        val ourModule = module { a: Int, b: Int, c: Int ->
            instance(Service(a + b + c))
        }

        // Now we can use the module when defining a kontainer blueprint
        val blueprint = kontainer {
            module(ourModule, 1, 10, 100)
        }

        val kontainer = blueprint.create()

        println("Sum: " + kontainer.get(Service::class).sum)

        // !END! //
    }
}
