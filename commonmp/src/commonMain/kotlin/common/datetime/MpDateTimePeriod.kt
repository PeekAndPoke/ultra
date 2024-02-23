package de.peekandpoke.ultra.common.datetime

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DateTimePeriod
import kotlinx.datetime.toDateTimePeriod
import kotlinx.serialization.Serializable
import kotlin.time.Duration

@Serializable
// TODO: Test me
data class MpDateTimePeriod(
    override val years: Int = 0,
    override val months: Int = 0,
    override val days: Int = 0,
    override val hours: Int = 0,
    override val minutes: Int = 0,
    override val seconds: Int = 0,
    override val milliseconds: Int = 0,
) : MpTemporalPeriod {
    companion object {
        val Zero: MpDateTimePeriod = MpDateTimePeriod()

        fun parse(text: String): MpDateTimePeriod {
            return DatePeriod.parse(text).let {
                MpDateTimePeriod(years = it.years, months = it.months, days = it.days)
            }
        }

        fun of(duration: Duration): MpDateTimePeriod {
            return of(duration.toDateTimePeriod())
        }

        fun of(period: DateTimePeriod): MpDateTimePeriod {
            return MpDateTimePeriod(
                years = period.years,
                months = period.months,
                days = period.days,
                hours = period.hours,
                minutes = period.minutes,
                seconds = period.seconds,
                milliseconds = period.nanoseconds / 1_000_000,
            )
        }
    }

    override operator fun unaryMinus(): MpDateTimePeriod {
        return MpDateTimePeriod(
            years = -years,
            months = -months,
            days = -days,
            hours = -hours,
            minutes = -minutes,
            seconds = -seconds,
            milliseconds = -milliseconds,
        )
    }

    fun toDatePeriod(): MpDatePeriod {
        return MpDatePeriod(
            years = years,
            months = months,
            days = days,
        )
    }
}
