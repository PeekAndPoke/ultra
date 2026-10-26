package io.peekandpoke.ultra.maths

import kotlin.math.floor

/**
 * Snaps the value to the nearest `offset + k * size` (ULTRA-00242). The [offset] shifts the grid, not the value; a
 * value halfway between two grid points goes up.
 *
 * A [size] of 0 or less, a [size] or [offset] that is not finite, a value that is not finite, and a result that would
 * not be finite return the value as it is. The result carries the usual floating-point error: `0.3.quantize(0.1)`
 * may be `0.30000000000000004`.
 */
fun Double.quantize(size: Double, offset: Double = 0.0): Double {
    // NaN-guard: `!(size > 0.0)` is also true for a NaN
    if (!(size > 0.0) || !size.isFinite() || !offset.isFinite() || !isFinite()) return this

    val snapped = offset + floor((this - offset) / size + 0.5) * size

    // A grid too fine for the value overflows: then the value as it is
    return if (snapped.isFinite()) snapped else this
}
