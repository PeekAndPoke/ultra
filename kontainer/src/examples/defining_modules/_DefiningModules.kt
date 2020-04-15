package de.peekandpoke.ultra.kontainer.examples.defining_modules

import de.peekandpoke.ultra.common.docs.Example
import de.peekandpoke.ultra.common.docs.ExampleChapter

@Suppress("ClassName")
class _DefiningModules : ExampleChapter {

    override val title = "Defining Modules"

    override val description = """
        Kontainer Modules are a very useful and simple way to group services together.

        Library developers can use them to bundle up their library and to provide easy ways to:
        - integrate the library into the kontainer of an application
        - document the library and customization options on a high level

        A user of a kontainer module can then simply include the module into the kontainer definition.
         
        A module can also give a nice high level documentation of what the library does and how to customize it.
        
        For example there could be some comments in the module definition code, that explain which services can be 
        overridden to achieve different behaviours of the library.
    """.trimIndent()

    override val examples: List<Example> = listOf(
        SimpleModuleExample()
    )
}
