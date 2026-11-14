package io.peekandpoke.ultra.datetime

/**
 * Genesis, 0001-01-01T00:00:00Z: the start that stands for "no start" in ranges (ULTRA-00343).
 *
 * It is not a limit of the date types: earlier dates stay valid plain values, and a range that starts before it counts
 * as open at its start. Seen in a zone west of UTC it falls on 0000-12-31.
 */
const val GENESIS_TIMESTAMP = -62_135_596_800_000L

/**
 * Doomsday, 9999-12-31T00:00:00Z: the end that stands for "no end" in ranges (ULTRA-00343).
 *
 * It is not a limit of the date types: later dates stay valid plain values, and a range that ends at or after it
 * counts as open at its end. Both sentinels lie inside the timestamp range of ArangoDB's date functions.
 */
const val DOOMSDAY_TIMESTAMP = 253_402_214_400_000L
