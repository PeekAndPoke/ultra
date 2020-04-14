package de.peekandpoke.ultra.mutator.examples.dataclasses

import de.peekandpoke.ultra.mutator.Mutable
import de.peekandpoke.ultra.mutator.examples.MutatorExample

// !BEGIN! //

// Here is out data class
@Mutable
data class ExampleClassWithNullableScalars(
    val anInt: Int?,
    val aFloat: Float?,
    val aBoolean: Boolean?,
    val aString: String?
)

// !END! //

class NullableScalarAndStringPropertiesExample : MutatorExample() {

    override val title = "Nullable Scalar and String properties"

    private val mutatorCode = ExampleClassWithNullableScalars::class.loadMutatorCode()

    override val description = """
        This examples shows how we can mutate nullable scalar (Int, Boolean, Float, ...) and String properties.
    """.trimIndent()

    override val additionalInfo = """
        The generated mutator code for our data class looks like this:
    """.trimIndent() + markdownKotlinCode(mutatorCode)

    override fun run() {
        // !BEGIN! //

        // We create an instance
        val original = ExampleClassWithNullableScalars(
            anInt = 10,
            aFloat = 3.14f,
            aBoolean = false,
            aString = "Hello World!"
        )

        // Now we can mutate our object
        val result = original.mutate {
            anInt = null
            aFloat = null
            aBoolean = aBoolean ?: true
            aString = null
        }

        println("The original:")
        println(original)

        println("The result:")
        println(result)

        // !END! //
    }
}
