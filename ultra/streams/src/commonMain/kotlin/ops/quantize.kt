package io.peekandpoke.ultra.streams.ops

import io.peekandpoke.ultra.maths.quantize
import io.peekandpoke.ultra.streams.Stream
import io.peekandpoke.ultra.streams.steady
import kotlin.jvm.JvmName

/**
 * Snaps every value to the nearest `offset + k * size` (ULTRA-00242); the [offset] shifts the grid, not the value.
 * See [quantize] on `Double` for the edge cases.
 */
fun Stream<Double>.quantize(size: Double, offset: Double = 0.0): Stream<Double> =
    quantize(size = steady(size), offset = steady(offset))

/**
 * Snaps every value to the nearest `offset + k * size`, on a grid that may change (ULTRA-00271): a new [size] or
 * [offset] re-snaps the current value at once. Both are listened to only while someone listens here.
 */
fun Stream<Double>.quantize(size: Stream<Double>, offset: Stream<Double> = steady(0.0)): Stream<Double> =
    combinedWith(size.pairedWith(offset)) { value, (s, o) -> value.quantize(size = s, offset = o) }

/** Snaps every value to the nearest `offset + k * size`; `null` passes as it is. */
@JvmName("quantizeNullable")
fun Stream<Double?>.quantize(size: Double, offset: Double = 0.0): Stream<Double?> =
    quantize(size = steady(size), offset = steady(offset))

/** Snaps every value to the nearest `offset + k * size`, on a grid that may change; `null` passes as it is. */
@JvmName("quantizeNullableByStreams")
fun Stream<Double?>.quantize(size: Stream<Double>, offset: Stream<Double> = steady(0.0)): Stream<Double?> =
    combinedWith(size.pairedWith(offset)) { value, (s, o) -> value?.quantize(size = s, offset = o) }
