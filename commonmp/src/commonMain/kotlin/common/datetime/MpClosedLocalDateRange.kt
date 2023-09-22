package de.peekandpoke.ultra.common.datetime

import common.datetime.DateTimeRangeConverter
import de.peekandpoke.ultra.common.ComparableTo
import kotlinx.serialization.Serializable

@Serializable
// TODO: test all of me
data class MpClosedLocalDateRange(
    val from: MpLocalDate,
    val to: MpLocalDate,
) : ComparableTo<MpDatePeriod> {
    companion object {
        /**
         * A range from [MpLocalDate.Genesis] until [MpLocalDate.Doomsday]
         */
        val forever: MpClosedLocalDateRange =
            MpClosedLocalDateRange(MpLocalDate.Genesis, MpLocalDate.Doomsday)

        /**
         * Creates a range for the given [from] and [to].
         */
        fun of(from: MpLocalDate, to: MpLocalDate): MpClosedLocalDateRange =
            MpClosedLocalDateRange(from = from, to = to)

        /**
         * Creates a range from [MpLocalDate.Genesis] until including [to]
         */
        fun endingAt(to: MpLocalDate): MpClosedLocalDateRange =
            MpClosedLocalDateRange(from = forever.from, to = to)

        /**
         * Creates a DateRange from including [from] until [MpLocalDate.Doomsday]
         */
        fun beginningAt(from: MpLocalDate): MpClosedLocalDateRange =
            MpClosedLocalDateRange(from = from, to = forever.to)
    }

    @Serializable
    data class Partial(
        val from: MpLocalDate?,
        val to: MpLocalDate?,
    ) {
        companion object {
            val empty = Partial(from = null, to = null)
        }

        fun asValidRange(): MpClosedLocalDateRange? = when (from != null && to != null) {
            true -> MpClosedLocalDateRange(from = from, to = to).takeIf { it.isValid }
            false -> null
        }
    }

    val asOpenRange: MpLocalDateRange by lazy {
        MpLocalDateRange(from = from, to = to.plusDays(1))
    }

    val asDatePeriod: MpDatePeriod by lazy { asOpenRange.asDatePeriod }

    val asWholeDays: Int by lazy { asOpenRange.asWholeDays }

    val hasStart: Boolean get() = asOpenRange.hasStart

    val hasEnd: Boolean get() = asOpenRange.hasEnd

    val isOpen: Boolean get() = asOpenRange.isOpen

    val isNotOpen: Boolean get() = !isOpen

    val isValid: Boolean get() = asOpenRange.isValid

    val isNotValid: Boolean get() = !isValid

    fun asPartialRange(): Partial {
        return Partial(from = from, to = to)
    }

    override fun compareTo(other: MpDatePeriod): Int {
        return asOpenRange.compareTo(other)
    }

    fun toZonedTimeRange(timezone: MpTimezone) = DateTimeRangeConverter(
        timezone = timezone,
        from = from,
        to = to,
    )
}
