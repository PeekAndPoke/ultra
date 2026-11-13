package io.peekandpoke.ultra.datetime

import kotlinx.serialization.Serializable

/**
 * A day of the week, from [MONDAY] (`1`) to [SUNDAY] (`7`), as ISO 8601 numbers them.
 *
 * Same entries and order as kotlinx-datetime's and java.time's `DayOfWeek`; converters: `kotlinx` / `mp`, and on
 * the JVM `jvm` / `mp`.
 */
@Serializable
enum class MpDayOfWeek(
    /** The ISO 8601 day number, where Monday is `1` and Sunday is `7`. */
    val isoDayNumber: Int,
) {
    MONDAY(1),
    TUESDAY(2),
    WEDNESDAY(3),
    THURSDAY(4),
    FRIDAY(5),
    SATURDAY(6),
    SUNDAY(7);

    companion object {
        /** The day with the given [isoDayNumber] (1-7); throws [IllegalArgumentException] for any other number. */
        fun of(isoDayNumber: Int): MpDayOfWeek {
            return ofOrNull(isoDayNumber) ?: throw IllegalArgumentException("Invalid ISO day number: $isoDayNumber")
        }

        /** The day with the given [isoDayNumber] (1-7), or null for any other number. */
        fun ofOrNull(isoDayNumber: Int): MpDayOfWeek? {
            return if (isoDayNumber in 1..7) entries[isoDayNumber - 1] else null
        }

        /** The day with the given [isoDayNumber] (1-7), or [default] for any other number. */
        fun ofOrDefault(isoDayNumber: Int, default: MpDayOfWeek): MpDayOfWeek {
            return ofOrNull(isoDayNumber) ?: default
        }
    }
}
