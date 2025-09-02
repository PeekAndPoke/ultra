package de.peekandpoke.mutator.examples.code_generation

import de.peekandpoke.mutator.Mutable
import de.peekandpoke.ultra.common.docs.SimpleExample
import java.io.File

@Suppress("unused")
class CodeGeneratedForDataClasses : SimpleExample() {

    // !BEGIN! //

    // We defined the data class
    @Mutable
    data class Person(
        val name: String,
        val age: Int,
        val address: Address? = null,
    )

    @Mutable
    data class Address(
        val street: String,
        val city: String,
    )

    @Mutable
    data class AddressBook(
        val persons: List<Person>,
    )

    // !END! //

    override val title = "Generated code for data classes"

    override val description = """
        This example shows how to apply the @Mutate annotation to a data class.
        
        We will then mutate the data class by creating and using a `mutator()`.
    """.trimIndent()

    override fun run() {
        val dir = File("build/generated/ksp/jvm/jvmTest/kotlin/de/peekandpoke/mutator/examples/code_generation/")

        val personCode = File(dir, "CodeGeneratedForDataClasses.Person${"$$"}mutator.kt").readText()

        addAdditionalOutput("Code generated for the Person class")
        addAdditionalOutput(markdownKotlinCode(personCode))

        val addressCode = File(dir, "CodeGeneratedForDataClasses.Address${"$$"}mutator.kt").readText()

        addAdditionalOutput("Code generated for the Address class")
        addAdditionalOutput(markdownKotlinCode(addressCode))

        val addressBookCode = File(dir, "CodeGeneratedForDataClasses.AddressBook${"$$"}mutator.kt").readText()

        addAdditionalOutput("Code generated for the AddressBook class")
        addAdditionalOutput(markdownKotlinCode(addressBookCode))
    }
}
