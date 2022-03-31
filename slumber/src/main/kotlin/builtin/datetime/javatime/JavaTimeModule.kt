package de.peekandpoke.ultra.slumber.builtin.datetime.javatime

import de.peekandpoke.ultra.slumber.Awaker
import de.peekandpoke.ultra.slumber.SlumberModule
import de.peekandpoke.ultra.slumber.Slumberer
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.isSuperclassOf

object JavaTimeModule : SlumberModule {

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

            else -> null
        }
    }
}
