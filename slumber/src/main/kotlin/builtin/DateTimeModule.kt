package de.peekandpoke.ultra.slumber.builtin

import de.peekandpoke.common.datetime.PortableDate
import de.peekandpoke.common.datetime.PortableDateTime
import de.peekandpoke.ultra.slumber.Awaker
import de.peekandpoke.ultra.slumber.SlumberModule
import de.peekandpoke.ultra.slumber.Slumberer
import de.peekandpoke.ultra.slumber.builtin.datetime.*
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZonedDateTime
import kotlin.reflect.KType

object DateTimeModule : SlumberModule {

    override fun getAwaker(type: KType): Awaker? {
        return when (type.classifier) {
            // Java util
            java.util.Date::class -> type.wrapIfNonNull(DateAwaker)

            // Java time
            LocalDate::class -> type.wrapIfNonNull(LocalDateAwaker)
            LocalDateTime::class -> type.wrapIfNonNull(LocalDateTimeAwaker)
            Instant::class -> type.wrapIfNonNull(InstantAwaker)
            ZonedDateTime::class -> type.wrapIfNonNull(ZonedDateTimeAwaker)

            // Portable Dates
            PortableDate::class -> type.wrapIfNonNull(PortableDateAwaker)
            PortableDateTime::class -> type.wrapIfNonNull(PortableDateTimeAwaker)

            else -> null
        }
    }

    override fun getSlumberer(type: KType): Slumberer? {
        return when (type.classifier) {
            // Java util
            java.util.Date::class -> type.wrapIfNonNull(DateSlumberer)

            // Java time
            LocalDate::class -> type.wrapIfNonNull(LocalDateSlumberer)
            LocalDateTime::class -> type.wrapIfNonNull(LocalDateTimeSlumberer)
            Instant::class -> type.wrapIfNonNull(InstantSlumberer)
            ZonedDateTime::class -> type.wrapIfNonNull(ZonedDateTimeSlumberer)

            // Portable Dates
            PortableDate::class -> type.wrapIfNonNull(PortableDateSlumberer)
            PortableDateTime::class -> type.wrapIfNonNull(PortableDateTimeSlumberer)

            else -> null
        }
    }
}
