package io.peekandpoke.ultra.datetime

import kotlinx.serialization.Serializable

/**
 * A month of the year, from [JANUARY] (`1`) to [DECEMBER] (`12`).
 *
 * Same entries and order as kotlinx-datetime's and java.time's `Month`; converters: `kotlinx` / `mp`, and on the
 * JVM `jvm` / `mp`.
 */
@Serializable
enum class MpMonth(
    /** The month number, where January is `1`. */
    val number: Int,
) {
    JANUARY(1),
    FEBRUARY(2),
    MARCH(3),
    APRIL(4),
    MAY(5),
    JUNE(6),
    JULY(7),
    AUGUST(8),
    SEPTEMBER(9),
    OCTOBER(10),
    NOVEMBER(11),
    DECEMBER(12);

    companion object {
        /** The month with the given [number] (1-12); throws [IllegalArgumentException] for any other number. */
        fun of(number: Int): MpMonth {
            return ofOrNull(number) ?: throw IllegalArgumentException("Invalid month number: $number")
        }

        /** The month with the given [number] (1-12), or null for any other number. */
        fun ofOrNull(number: Int): MpMonth? {
            return if (number in 1..12) entries[number - 1] else null
        }

        /** The month with the given [number] (1-12), or [default] for any other number. */
        fun ofOrDefault(number: Int, default: MpMonth): MpMonth {
            return ofOrNull(number) ?: default
        }
    }
}
