package io.peekandpoke.ultra.datetime

import io.kotest.core.spec.style.StringSpec
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.TimeSource

/**
 * Timings for the browser's hot paths. Not an assertion: the figures are measured while the spec registers its tests and
 * end up in the TEST NAMES (`BENCH <name>: <ns> ns/op`, see the jsBrowserTest XML), because browser console output is
 * not captured. Compare them in `.claude/tasks-archive/2026-10/20261018-datetime-js-performance.md` before and after a change.
 */
@Suppress("unused")
class DatetimeJsBenchmarkSpec : StringSpec({

    val steps = 20_000
    val berlin = MpTimezone.of("Europe/Berlin")
    // Spans the spring-forward transition, so the cache sees one interval change.
    val start = MpInstant.parse("2022-03-20T00:00:00Z")

    var sink = 0L

    fun bench(name: String, ops: Int, block: () -> Unit) {
        block() // warm-up
        val mark = TimeSource.Monotonic.markNow()
        block()
        val ns = mark.elapsedNow().inWholeNanoseconds

        "BENCH $name: ${ns / ops} ns/op" { }
    }

    run {
        bench("instant.atZone(berlin).toLocalDateTime()", steps) {
            var t = start
            repeat(steps) {
                sink += t.atZone(berlin).toLocalDateTime().minute
                t += 1.minutes
            }
        }

        bench("local.toInstant(berlin)", steps) {
            var local = start.atZone(MpTimezone.UTC).toLocalDateTime()
            repeat(steps) {
                sink += local.toInstant(berlin).toEpochSeconds()
                local = local.toInstant(MpTimezone.UTC).plus(1.minutes).atZone(MpTimezone.UTC).toLocalDateTime()
            }
        }

        bench("atZone, daily stride", steps) {
            var t = start
            repeat(steps) {
                sink += t.atZone(berlin).toLocalDateTime().minute
                t += 1.days
            }
        }

        bench("atZone, scattered over 50 years (cold-ish)", steps) {
            var seconds = start.toEpochSeconds()
            repeat(steps) {
                // a large odd stride: every call lands far from the previous ones
                seconds = 1_000_000_000L + (seconds * 7_919L + 104_729L).mod(1_600_000_000L)
                sink += MpInstant.fromEpochSeconds(seconds).atZone(berlin).toLocalDateTime().minute
            }
        }

        bench("berlin.offsetSecondsAt(instant)", steps) {
            var t = start
            repeat(steps) {
                sink += berlin.offsetSecondsAt(t)
                t += 1.minutes
            }
        }

        bench("instant compare / minus / plus", steps) {
            var last = start
            var t = start
            repeat(steps) {
                if (t - last >= 1.hours) last = t
                if (t > last) sink++
                t += 1.minutes
            }
        }

        bench("MpLocalTime plus / hour / minute", steps) {
            var time = MpLocalTime.Min
            repeat(steps) {
                time = time.plus(1.minutes)
                sink += time.hour + time.minute
            }
        }

        "BENCH sink $sink" { }
    }
})
