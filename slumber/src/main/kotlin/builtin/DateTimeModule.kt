package de.peekandpoke.ultra.slumber.builtin

import de.peekandpoke.ultra.slumber.Awaker
import de.peekandpoke.ultra.slumber.SlumberModule
import de.peekandpoke.ultra.slumber.Slumberer
import de.peekandpoke.ultra.slumber.builtin.datetime.*
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZonedDateTime
import kotlin.reflect.KType

object DateTimeModule : SlumberModule {

    override fun getAwaker(type: KType): Awaker? {
        return when (type.classifier) {
            LocalDateTime::class -> type.wrapIfNonNull(LocalDateTimeAwaker)
            Instant::class -> type.wrapIfNonNull(InstantAwaker)
            ZonedDateTime::class -> type.wrapIfNonNull(ZonedDateTimeAwaker)
            else -> null
        }
    }

    override fun getSlumberer(type: KType): Slumberer? {
        return when (type.classifier) {
            LocalDateTime::class -> type.wrapIfNonNull(LocalDateTimeSlumberer)
            Instant::class -> type.wrapIfNonNull(InstantSlumberer)
            ZonedDateTime::class -> type.wrapIfNonNull(ZonedDateTimeSlumberer)
            else -> null
        }
    }
}
