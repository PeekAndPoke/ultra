package de.peekandpoke.ultra.mutator.examples.dataclasses

import de.peekandpoke.ultra.mutator.Mutable
import de.peekandpoke.ultra.mutator.examples.MutatorExample
import java.time.LocalDate

// !BEGIN! //

// Here is out data class
@Mutable
data class ExampleClassWithAny(
    val any: Any,
    val nullableAny: Any?,
    val aDate: LocalDate
)

// !END! //

class AnyPropertiesExample : MutatorExample() {

    override val title = "'Any' properties and unknown types"

    private val mutatorCode = ExampleClassWithAny::class.loadMutatorCode()

    override val description = """
        This examples shows how we can mutate 'Any" properties.  
        It also shows how mutations behave for types of properties that are unknown to Mutator.
        
        In short, the behaviour is exactly the same as with scalar types.
         
        When we have a property in your class of type 'Any" or 'Any?' we can read it and re-assign it.
        
        When we have properties of types, for which Mutator does not support any special mutation mechanism, e.g. 
        java.time.LocalDate, we can do the same: read the value and re-assign the value. 
    """.trimIndent()

    override val additionalInfo = """
        The generated mutator code for our data class looks like this:
    """.trimIndent() + markdownKotlinCode(mutatorCode)

    override fun run() {
        // !BEGIN! //

        // We create an instance
        val original = ExampleClassWithAny(
            any = "any",
            nullableAny = null,
            aDate = LocalDate.now()
        )

        // Now we can mutate our object
        val result = original.mutate {
            any = 10
            nullableAny = 4.669f
            aDate = aDate.plusDays(1)
        }

        println("The original:")
        println(original)

        println("The result:")
        println(result)

        // !END! //
    }
}
