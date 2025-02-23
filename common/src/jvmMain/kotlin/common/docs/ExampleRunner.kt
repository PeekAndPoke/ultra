package de.peekandpoke.ultra.common.docs

class ExampleRunner {

    fun run(examples: List<Example>) {

        examples.forEachIndexed { idx, it ->

            repeat(120) { print("=") }
            println()
            println("Example #${idx + 1}")
            println()
            println(it.title)
            repeat(120) { print("=") }
            println()

            println()
            it.run()
            println()

            if (idx < examples.size - 1) {
                println("[Press [ENTER] to continue]")
                readLine()
                println()
            }
        }
    }
}
