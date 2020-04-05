package de.peekandpoke.ultra.common.docs

interface Example {
    val title: String
    val description: String

    fun run()

    fun runAndRecordOutput(): String
}

abstract class SimpleExample : Example {

    private var builder: StringBuilder? = null

    override fun runAndRecordOutput(): String {
        builder = StringBuilder()

        run()

        return builder.toString()
    }

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
            else -> b.appendln(message)
        }
    }
}

interface ExampleChapter {
    val title: String
    val packageLocation: String
    val examples: List<Example>
}
