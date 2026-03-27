package io.peekandpoke.ultra.datetime

/**
 * A simple date/time formatter that supports common format tokens.
 *
 * Supported tokens:
 * - `yyyy` — 4-digit year
 * - `yy` — 2-digit year
 * - `MM` — zero-padded month (01-12)
 * - `MMM` — abbreviated month name (Jan-Dec, English)
 * - `dd` — zero-padded day (01-31)
 * - `HH` — zero-padded hour 0-23
 * - `mm` — zero-padded minute (00-59)
 * - `ss` — zero-padded second (00-59)
 * - `SSS` — zero-padded milliseconds (000-999)
 * - `Z` — timezone offset string (e.g., "+0000" or "Z")
 * - `'...'` — quoted literal text
 * - Any other character — passed through as literal
 *
 * Unknown or malformed tokens are passed through as literals. This formatter never throws.
 */
object MpDateTimeFormatter {

    private val monthNames = arrayOf(
        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
    )

    /**
     * Formats the given date/time components according to [pattern].
     *
     * All numeric parameters default to 0 so callers can omit irrelevant components
     * (e.g., MpLocalDate can pass only year/month/day).
     */
    fun format(
        pattern: String,
        year: Int = 0,
        month: Int = 0,
        day: Int = 0,
        hour: Int = 0,
        minute: Int = 0,
        second: Int = 0,
        millis: Int = 0,
        offsetString: String = "",
    ): String {
        val result = StringBuilder(pattern.length + 16)
        var i = 0

        while (i < pattern.length) {
            val c = pattern[i]

            when {
                // Quoted literal: '...'
                c == '\'' -> {
                    val end = pattern.indexOf('\'', i + 1)
                    if (end == -1) {
                        // Unclosed quote — pass through remainder as literal
                        result.append(pattern, i + 1, pattern.length)
                        i = pattern.length
                    } else {
                        result.append(pattern, i + 1, end)
                        i = end + 1
                    }
                }

                // yyyy or yy
                c == 'y' -> {
                    val len = consumeRun(pattern, i, 'y')
                    when {
                        len >= 4 -> result.append(year.coerceAtLeast(0).toString().padStart(4, '0'))
                        len >= 2 -> result.append((year % 100).toString().padStart(2, '0'))
                        else -> result.append('y')
                    }
                    i += len
                }

                // MMM or MM
                c == 'M' -> {
                    val len = consumeRun(pattern, i, 'M')
                    when {
                        len >= 3 -> result.append(monthName(month))
                        len >= 2 -> result.append(month.coerceIn(0, 99).toString().padStart(2, '0'))
                        else -> result.append('M')
                    }
                    i += len
                }

                // dd
                c == 'd' -> {
                    val len = consumeRun(pattern, i, 'd')
                    if (len >= 2) {
                        result.append(day.coerceIn(0, 99).toString().padStart(2, '0'))
                    } else {
                        result.append('d')
                    }
                    i += len
                }

                // HH
                c == 'H' -> {
                    val len = consumeRun(pattern, i, 'H')
                    if (len >= 2) {
                        result.append(hour.coerceIn(0, 99).toString().padStart(2, '0'))
                    } else {
                        result.append('H')
                    }
                    i += len
                }

                // mm (minutes) — only when not preceded by 'M' context
                // Since we already consumed 'M' runs above, standalone 'm' here is minutes
                c == 'm' -> {
                    val len = consumeRun(pattern, i, 'm')
                    if (len >= 2) {
                        result.append(minute.coerceIn(0, 99).toString().padStart(2, '0'))
                    } else {
                        result.append('m')
                    }
                    i += len
                }

                // ss
                c == 's' -> {
                    val len = consumeRun(pattern, i, 's')
                    if (len >= 2) {
                        result.append(second.coerceIn(0, 99).toString().padStart(2, '0'))
                    } else {
                        result.append('s')
                    }
                    i += len
                }

                // SSS (milliseconds)
                c == 'S' -> {
                    val len = consumeRun(pattern, i, 'S')
                    if (len >= 3) {
                        result.append(millis.coerceIn(0, 999).toString().padStart(3, '0'))
                    } else {
                        // Pass through as literal
                        repeat(len) { result.append('S') }
                    }
                    i += len
                }

                // Z (timezone offset)
                c == 'Z' -> {
                    result.append(offsetString.ifEmpty { "Z" })
                    i++
                }

                // Any other character — literal
                else -> {
                    result.append(c)
                    i++
                }
            }
        }

        return result.toString()
    }

    private fun consumeRun(pattern: String, start: Int, char: Char): Int {
        var count = 0
        while (start + count < pattern.length && pattern[start + count] == char) {
            count++
        }
        return count
    }

    private fun monthName(month: Int): String {
        val idx = month - 1
        return if (idx in monthNames.indices) monthNames[idx] else "???"
    }
}
