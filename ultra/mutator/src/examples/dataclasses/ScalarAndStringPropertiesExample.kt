package de.peekandpoke.ultra.mutator.examples.dataclasses

import de.peekandpoke.ultra.mutator.Mutable
import de.peekandpoke.ultra.mutator.examples.MutatorExample

// !BEGIN! //

// Here is out data class
@Mutable
data class ExampleClassWithScalars(
    val anInt: Int,
    val aFloat: Float,
    val aBoolean: Boolean,
    val aString: String
)

// !END! //

class ScalarAndStringPropertiesExample : MutatorExample() {

    override val title = "Scalar and String properties"

    private val mutatorCode = ExampleClassWithScalars::class.loadMutatorCode()

    override val description = """
        This examples shows how we can mutate scalar (Int, Boolean, Float, ...) and String properties.
        
        Notice that inside of the mutate { } closure, we are not working on our objects itself, but rather on
        a wrapper. The code for this wrapper is generated for us, as we put the @Mutable annotation on our class.
    """.trimIndent()

    override val additionalInfo = """
        The generated mutator code for our data class looks like this:
    """.trimIndent() + markdownKotlinCode(mutatorCode)

    override fun run() {
        // !BEGIN! //

        // We create an instance
        val original = ExampleClassWithScalars(
            anInt = 10,
            aFloat = 3.14f,
            aBoolean = false,
            aString = "Hello World!"
        )

        // Now we can mutate our object
        val result = original.mutate {
            anInt += 1
            aFloat = 4.669f
            aBoolean = !aBoolean
            aString = "Hey, $aString"
        }

        println("The original:")
        println(original)

        println("The result:")
        println(result)

        // !END! //
    }
}
