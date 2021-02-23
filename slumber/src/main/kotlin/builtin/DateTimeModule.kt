package de.peekandpoke.ultra.slumber.builtin

import de.peekandpoke.ultra.common.datetime.PortableDate
import de.peekandpoke.ultra.common.datetime.PortableDateTime
import de.peekandpoke.ultra.slumber.Awaker
import de.peekandpoke.ultra.slumber.SlumberModule
import de.peekandpoke.ultra.slumber.Slumberer
import de.peekandpoke.ultra.slumber.builtin.datetime.*
import java.time.*
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
            Instant::class ->
                type.wrapIfNonNull(InstantAwaker)
            ZonedDateTime::class ->
                type.wrapIfNonNull(ZonedDateTimeAwaker)
            ZoneId::class ->
                type.wrapIfNonNull(ZoneIdAwaker)

            // Portable Dates
            PortableDate::class ->
                type.wrapIfNonNull(PortableDateAwaker)

            PortableDateTime::class ->
                type.wrapIfNonNull(PortableDateTimeAwaker)

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

            cls == Instant::class ->
                type.wrapIfNonNull(InstantSlumberer)

            cls == ZonedDateTime::class ->
                type.wrapIfNonNull(ZonedDateTimeSlumberer)

            cls is KClass<*> && ZoneId::class.isSuperclassOf(cls) ->
                type.wrapIfNonNull(ZoneIdSlumberer)

            // Portable Dates
            cls == PortableDate::class ->
                type.wrapIfNonNull(PortableDateSlumberer)

            cls == PortableDateTime::class ->
                type.wrapIfNonNull(PortableDateTimeSlumberer)

            else -> null
        }
    }
}
