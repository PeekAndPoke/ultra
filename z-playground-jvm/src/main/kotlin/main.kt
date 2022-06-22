package de.peekandpoke.ultra.playground

import de.peekandpoke.ultra.common.reflection.kType
import de.peekandpoke.ultra.slumber.Codec
import kotlinx.coroutines.delay

suspend fun main() {


//    println(Json.decodeFromString(Int.serializer().nullable, "null"))
//
//    println(Json.decodeFromString(Int.serializer(), "null"))

    data class ExampleClass<P1, P2>(
        val p1: P1,
        val p2: P2,
        val children: List<ExampleClass<P1, P2>> = emptyList(),
    )

    val codec = Codec.default

    repeat(100000) { round ->

        println("next round $round")

        val example = ExampleClass(
            p1 = 1,
            p2 = "a",
            children = (1..10).map {
                ExampleClass(it, "b$it")
            },
        )

        repeat(100000) {

            val type = kType<ExampleClass<Int, String>>()

            val reified = type.reified

            val slumbered = codec.slumber(example)

            val awoken: ExampleClass<Int, String>? = codec.awake(kType(), slumbered)

            requireNotNull(awoken)
        }

        delay(1000)
    }
}
