package de.peekandpoke.ultra.playground

import coroutines.measureCoroutine
import kotlinx.coroutines.CoroutineName
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

        launch(Dispatchers.Default + CoroutineName("ioWork")) {
            ioWork(duration = 50.milliseconds)
        }

        val totalSum = run {

            val numCoroutines = Runtime.getRuntime().availableProcessors()

            val jobs = (0..<numCoroutines).mapIndexed { idx, job ->
                async(Dispatchers.Default + CoroutineName("cpuWork-$idx")) {
                    delay(1.milliseconds)
                    println("started job $job")

                    val result = cpuWork(100_000_000)

                    println("finished job $job with result $result")

//                        delay(1.seconds)

                    result
                }
            }

            jobs.awaitAll().sum()
        }

        "Total sum: $totalSum"
    }

    println("value           = ${result.value}")
    println("total time      = ${"%.2f".format(result.profile.timeMs)} ms")
    println("total cpu time  = ${"%.2f".format(result.profile.totalCpuMs)} ms")
    println("total cpu usage = ${"%.2f".format(result.profile.totalCpuUsage)}")
    println("total cpu pct   = ${"%.2f".format(result.profile.totalCpuUsagePct)} %")

    println()
    println(result.profile.plot())
}
