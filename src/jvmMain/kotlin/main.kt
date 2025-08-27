package de.peekandpoke.ultra.playground

import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope

suspend fun main(args: Array<String>) {
    println("Hallo Bernd! ${args.toList()}")

    mutator()
}

fun mutator() {
//    MutatorExamples.dataClassExample()

    MutatorExamples.listExample()

//    MutatorExamples.setExample()
}

suspend fun coroutines() {

    try {
        supervisorScope {
            val a = async {
                delay(500)
                error("async: Should be silent")
            }

            launch {
                delay(1000)
                error("launch: Should be visible")
            }

            a.join()
        }
    } catch (e: Exception) {
        println("Caught: $e")
    }
}
