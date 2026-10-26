package io.peekandpoke.ultra.streams.ops

import io.peekandpoke.ultra.maths.quantize
import io.peekandpoke.ultra.streams.Stream
import kotlin.jvm.JvmName

/**
 * Snaps every value to the nearest `offset + k * size` (ULTRA-00242); the [offset] shifts the grid, not the value.
 * See [quantize] on `Double` for the edge cases.
 */
fun Stream<Double>.quantize(size: Double, offset: Double = 0.0): Stream<Double> =
    map { it.quantize(size = size, offset = offset) }

/** Snaps every value to the nearest `offset + k * size`; `null` passes as it is. */
@JvmName("quantizeNullable")
fun Stream<Double?>.quantize(size: Double, offset: Double = 0.0): Stream<Double?> =
    map { it?.quantize(size = size, offset = offset) }
