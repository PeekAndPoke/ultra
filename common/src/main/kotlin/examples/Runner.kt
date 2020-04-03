package de.peekandpoke.ultra.common.examples

class Runner(private val examples: List<Example>) {

    fun run() {

        examples.forEachIndexed { idx, it ->

            println(
                "======================================================================================================"
            )
            println("Example #${idx + 1}")
            println()
            println(it.title)
            println(
                "======================================================================================================"
            )

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
