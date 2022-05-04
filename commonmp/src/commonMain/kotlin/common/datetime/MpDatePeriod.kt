package de.peekandpoke.ultra.common.datetime

import kotlinx.datetime.DatePeriod
import kotlinx.serialization.Serializable

@Serializable
// TODO: Test me
data class MpDatePeriod(
    override val years: Int = 0,
    override val months: Int = 0,
    override val days: Int = 0,
) : MpTemporalPeriod {
    companion object {
        fun parse(text: String): MpDatePeriod {
            return DatePeriod.parse(text).let {
                MpDatePeriod(years = it.years, months = it.months, days = it.days)
            }
        }
    }

    override val hours: Int = 0
    override val minutes: Int = 0
    override val seconds: Int = 0
    override val milliseconds: Int = 0

    override operator fun unaryMinus(): MpDatePeriod {
        return MpDatePeriod(
            years = -years,
            months = -months,
            days = -days,
        )
    }
}
