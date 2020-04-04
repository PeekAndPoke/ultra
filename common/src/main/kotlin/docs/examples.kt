package de.peekandpoke.ultra.common.docs

interface Example {
    val title: String

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
        builder?.append(message)
    }

    fun println(message: Any) {
        builder?.appendln(message)
    }
}

interface ExampleChapter {
    val name: String
    val packageLocation: String
    val examples: List<Example>
}
