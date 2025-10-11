package de.peekandpoke.ultra.playground

import coroutines.measureCoroutine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

suspend fun main() {

    fun cpuWork(n: Long) = (1..n).sumOf { it }

    suspend fun ioWork(duration: Duration) {
        coroutineScope {
            val job1 = async(Dispatchers.IO) {
                delay(duration)
            }

            val job2 = async(Dispatchers.IO) {
                delay(duration * 0.5)
            }

            println("Started IO work in parallel")

            listOf(job1, job2).awaitAll()

            println("Finished IO work in parallel")
        }
    }

    val result = measureCoroutine {

        val totalSum = coroutineScope {

            launch(Dispatchers.Default) {
                ioWork(duration = 200.milliseconds)
            }

            val numCoroutines = 16

            val jobs = (0..<numCoroutines).map { job ->
                async(Dispatchers.Default) {
                    delay(1.milliseconds)
                    println("started job $job")

                    val result = cpuWork(1_000_000_000)

                    println("finished job $job with result $result")

//                        delay(1.seconds)

                    result
                }
            }

            jobs.awaitAll().sum()
        }

        "Total sum: $totalSum"
    }

    println("value      = ${result.value}")
    println("total time = ${"%.2f".format(result.timing.totalMs)} ms")
    println("cpu time   = ${"%.2f".format(result.timing.cpuMs)} ms")
    println("cpu pct    = ${"%.2f".format(result.timing.cpuPct * 100)} %")
    println("idle time  = ${"%.2f".format(result.timing.idleMs)} ms")
    println("idle pct   = ${"%.2f".format(result.timing.idlePct * 100)} %")
}
