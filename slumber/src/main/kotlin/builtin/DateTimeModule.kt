package de.peekandpoke.ultra.slumber.builtin

import de.peekandpoke.ultra.common.datetime.PortableDate
import de.peekandpoke.ultra.common.datetime.PortableDateTime
import de.peekandpoke.ultra.common.datetime.PortableTime
import de.peekandpoke.ultra.common.datetime.PortableTimezone
import de.peekandpoke.ultra.slumber.Awaker
import de.peekandpoke.ultra.slumber.SlumberModule
import de.peekandpoke.ultra.slumber.Slumberer
import de.peekandpoke.ultra.slumber.builtin.datetime.DateAwaker
import de.peekandpoke.ultra.slumber.builtin.datetime.DateSlumberer
import de.peekandpoke.ultra.slumber.builtin.datetime.InstantAwaker
import de.peekandpoke.ultra.slumber.builtin.datetime.InstantSlumberer
import de.peekandpoke.ultra.slumber.builtin.datetime.LocalDateAwaker
import de.peekandpoke.ultra.slumber.builtin.datetime.LocalDateSlumberer
import de.peekandpoke.ultra.slumber.builtin.datetime.LocalDateTimeAwaker
import de.peekandpoke.ultra.slumber.builtin.datetime.LocalDateTimeSlumberer
import de.peekandpoke.ultra.slumber.builtin.datetime.LocalTimeAwaker
import de.peekandpoke.ultra.slumber.builtin.datetime.LocalTimeSlumberer
import de.peekandpoke.ultra.slumber.builtin.datetime.PortableDateAwaker
import de.peekandpoke.ultra.slumber.builtin.datetime.PortableDateSlumberer
import de.peekandpoke.ultra.slumber.builtin.datetime.PortableDateTimeAwaker
import de.peekandpoke.ultra.slumber.builtin.datetime.PortableDateTimeSlumberer
import de.peekandpoke.ultra.slumber.builtin.datetime.PortableTimeAwaker
import de.peekandpoke.ultra.slumber.builtin.datetime.PortableTimeSlumberer
import de.peekandpoke.ultra.slumber.builtin.datetime.PortableTimezoneAwaker
import de.peekandpoke.ultra.slumber.builtin.datetime.PortableTimezoneSlumberer
import de.peekandpoke.ultra.slumber.builtin.datetime.ZoneIdAwaker
import de.peekandpoke.ultra.slumber.builtin.datetime.ZoneIdSlumberer
import de.peekandpoke.ultra.slumber.builtin.datetime.ZonedDateTimeAwaker
import de.peekandpoke.ultra.slumber.builtin.datetime.ZonedDateTimeSlumberer
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.isSuperclassOf

object DateTimeModule : SlumberModule {

    override fun getAwaker(type: KType): Awaker? {
        return when (type.classifier) {
            // Java util
            java.util.Date::class ->
                type.wrapIfNonNull(DateAwaker)

            // Java time
            LocalDate::class ->
                type.wrapIfNonNull(LocalDateAwaker)

            LocalDateTime::class ->
                type.wrapIfNonNull(LocalDateTimeAwaker)

            LocalTime::class ->
                type.wrapIfNonNull(LocalTimeAwaker)

            Instant::class ->
                type.wrapIfNonNull(InstantAwaker)

            ZonedDateTime::class ->
                type.wrapIfNonNull(ZonedDateTimeAwaker)

            ZoneId::class ->
                type.wrapIfNonNull(ZoneIdAwaker)

            // Portable Dates
            PortableTime::class ->
                type.wrapIfNonNull(PortableTimeAwaker)

            PortableDate::class ->
                type.wrapIfNonNull(PortableDateAwaker)

            PortableDateTime::class ->
                type.wrapIfNonNull(PortableDateTimeAwaker)

            PortableTimezone::class ->
                type.wrapIfNonNull(PortableTimezoneAwaker)

            else -> null
        }
    }

    override fun getSlumberer(type: KType): Slumberer? {

        val cls = type.classifier

        return when {
            // Java util
            cls == java.util.Date::class ->
                type.wrapIfNonNull(DateSlumberer)

            // Java time
            cls == LocalDate::class ->
                type.wrapIfNonNull(LocalDateSlumberer)

            cls == LocalDateTime::class ->
                type.wrapIfNonNull(LocalDateTimeSlumberer)

            cls == LocalTime::class ->
                type.wrapIfNonNull(LocalTimeSlumberer)

            cls == Instant::class ->
                type.wrapIfNonNull(InstantSlumberer)

            cls == ZonedDateTime::class ->
                type.wrapIfNonNull(ZonedDateTimeSlumberer)

            cls is KClass<*> && ZoneId::class.isSuperclassOf(cls) ->
                type.wrapIfNonNull(ZoneIdSlumberer)

            // Portable Dates
            cls == PortableTime::class ->
                type.wrapIfNonNull(PortableTimeSlumberer)

            cls == PortableDate::class ->
                type.wrapIfNonNull(PortableDateSlumberer)

            cls == PortableDateTime::class ->
                type.wrapIfNonNull(PortableDateTimeSlumberer)

            cls == PortableTimezone::class ->
                type.wrapIfNonNull(PortableTimezoneSlumberer)

            else -> null
        }
    }
}
