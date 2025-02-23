package de.peekandpoke.ultra.common.datetime

// TODO: Test me
interface MpTemporalPeriod {
    val years: Int
    val months: Int
    val days: Int
    val hours: Int
    val minutes: Int
    val seconds: Int
    val milliseconds: Int

    operator fun unaryMinus(): MpTemporalPeriod

    operator fun plus(other: MpTemporalPeriod): MpDateTimePeriod {
        return MpDateTimePeriod(
            years = years + other.years,
            months = months + other.months,
            days = days + other.days,
            hours = hours + other.hours,
            minutes = minutes + other.minutes,
            seconds = seconds + other.seconds,
            milliseconds = milliseconds + other.milliseconds,
        )
    }

    operator fun minus(other: MpTemporalPeriod): MpDateTimePeriod {
        return plus(-other)
    }
}
