package de.peekandpoke.ultra.playground

import de.peekandpoke.ultra.common.reflection.kType
import de.peekandpoke.ultra.slumber.Codec
import kotlinx.coroutines.delay

suspend fun main() {

    data class ExampleClass<P1, P2>(
        val p1: P1,
        val p2: P2,
    )

    val codec = Codec.default

    while (true) {

        repeat(100000) {

            val type = kType<ExampleClass<Int, String>>()

            val reified = type.reified

//            val slumbered = codec.slumber(
//                ExampleClass(1, "a")
//            )
//
//            val awoken: ExampleClass<Int, String>? = codec.awake(kType(), slumbered)
        }

        delay(1000)

        println("next round")
    }
}
