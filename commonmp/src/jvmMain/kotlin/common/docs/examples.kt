package de.peekandpoke.ultra.common.docs

interface Example {
    /** The title of the example */
    val title: String

    /** The description of the example that is shown after the title */
    val description: String

    /** Additional information that is shown after the example code and output */
    val additionalInfo: String get() = ""

    /**
     * Runs the example code
     */
    fun run()

    /**
     * Runs the example code while trapping all console output
     */
    fun runAndRecordOutput(): String
}

abstract class SimpleExample : Example {

    private var builder: StringBuilder? = null

    override fun runAndRecordOutput(): String {
        builder = StringBuilder()

        run()

        return builder.toString()
    }

    fun markdownKotlinCode(code: String) = """
```kotlin
$code
```
    """

    fun print(message: Any) {
        when (val b = builder) {
            null -> kotlin.io.print(message)
            else -> b.append(message)
        }
    }

    fun println() {
        println("")
    }

    fun println(message: Any) {
        when (val b = builder) {
            null -> kotlin.io.println(message)
            else -> b.appendLine(message)
        }
    }
}

interface ExampleChapter {
    val title: String
    val description: String get() = ""
    val examples: List<Example>
}
