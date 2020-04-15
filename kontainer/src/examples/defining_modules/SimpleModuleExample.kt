package de.peekandpoke.ultra.kontainer.examples.defining_modules

import de.peekandpoke.ultra.common.docs.SimpleExample
import de.peekandpoke.ultra.kontainer.kontainer
import de.peekandpoke.ultra.kontainer.module

class SimpleModuleExample : SimpleExample() {

    override val title = "Defining a simple module"

    override val description = """
        This example shows how to define kontainer modules.
        
        Modules are very useful when we develop libraries.
        
        A user of a library can then simply include our module into the kontainer definition.
         
        A module can also give a nice high level documentation of what the library does and how to customize it.
        For example there could be some comments in the module code, that explain which service can be 
        overridden to achieve different behaviours of the library.
    """.trimIndent()

    // !BEGIN! //

    // Let's say we have some services

    // A database service
    class Database(val storage: Storage)

    // A Storage interface
    interface Storage {
        val name: String
    }

    // And by default we only deliver our module with a FileStorage implementation
    class FileStorage : Storage {
        override val name = "FileStorage"
    }

    // !END! //

    override fun run() {
        // !BEGIN! //

        // We can now define a kontainer module like this
        val ourModule = module {
            singleton(Database::class)

            // The storage service can be overridden by the user of the module
            singleton(Storage::class, FileStorage::class)
        }

        // Now we can use the module when defining a kontainer blueprint
        val blueprint = kontainer {
            module(ourModule)
        }

        val kontainer = blueprint.create()

        println("Storage service: " + kontainer.get(Database::class).storage.name)

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        // Let's create another implementation of Storage and override the default service
        class MemoryStorage : Storage {
            override val name = "MemoryStorage"
        }

        val blueprintEx = kontainer {
            module(ourModule)

            // Here we override the pre-defined Storage implementation
            singleton(Storage::class, MemoryStorage::class)
        }

        val kontainerEx = blueprintEx.create()

        println("Storage service now is: " + kontainerEx.get(Database::class).storage.name)

        // !END! //
    }
}
