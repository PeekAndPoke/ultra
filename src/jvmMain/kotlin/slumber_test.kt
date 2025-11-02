package de.peekandpoke.ultra.playground

import de.peekandpoke.ultra.slumber.Codec
import de.peekandpoke.ultra.slumber.SlumberConfig
import de.peekandpoke.ultra.slumber.builtin.objects.DataClassSlumberer.Companion.withSlumberCache
import de.peekandpoke.ultra.slumber.slumber
import io.github.serpro69.kfaker.faker
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.supervisorScope
import kotlin.time.Duration.Companion.seconds
import kotlin.time.measureTime

suspend fun main() {

    data class TestAddress(
        val street: String,
        val city: String,
    )

    data class TestClass(
        val name: String,
        val addresses: List<TestAddress> = emptyList(),
    )

    // Get max available heap
    val maxMemory = Runtime.getRuntime().maxMemory()
    println("Max available memory: $maxMemory bytes")

    val faker = faker { }

    val rounds = 1_000_000
    val warmUpRounds = 1_000

    val subject = TestClass(
        name = faker.name.name(),
        addresses = List(50) {
            TestAddress(
                street = faker.address.streetName(),
                city = faker.address.city(),
            )
        })

    // Warm up
    measureTime {
        val default = Codec.default
        val results = mutableListOf<Any?>()

        repeat(warmUpRounds) {
            results.add(default.slumber(subject))
        }
    }.let {
        println("$warmUpRounds warm up rounds without cache took: $it")
    }

    measureTime {
        val default = Codec.default
        val results = mutableListOf<Any?>()

        repeat(rounds) {
            results.add(default.slumber(subject))
        }
    }.let {
        println("$rounds rounds without cache took: $it")
    }

    // Warm up
    measureTime {
        val default = SlumberConfig.default.withSlumberCache {
            maxMemoryUsage(10 * 1034 * 1024) // 10 MB
        }.codec()

        val results = mutableListOf<Any?>()

        repeat(warmUpRounds) {
            results.add(default.slumber(subject))
        }
    }.let {
        println("$warmUpRounds warm up rounds WITH cache took: $it")
    }

    measureTime {
        val codec = SlumberConfig.default.withSlumberCache {
            maxMemoryUsage(10 * 1034 * 1024) // 10 MB
        }.codec()

        val results = mutableListOf<Any?>()

        repeat(rounds) {
            results.add(codec.slumber(subject))
        }
    }.let {
        println("$rounds rounds WITH cache took: $it")
    }

    // Checking if the FastCache.Loops are cleaned up when no longer referenced
    System.gc()

    supervisorScope {
        async {
            repeat(3) {
                println("GC run $it")
                System.gc()
                delay(1.seconds)
            }
        }.await()
    }
}
