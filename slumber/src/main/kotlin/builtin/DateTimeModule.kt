package de.peekandpoke.ultra.slumber.builtin

import de.peekandpoke.ultra.slumber.Awaker
import de.peekandpoke.ultra.slumber.Shared
import de.peekandpoke.ultra.slumber.SlumberModule
import de.peekandpoke.ultra.slumber.Slumberer
import de.peekandpoke.ultra.slumber.builtin.datetime.InstantCodec
import de.peekandpoke.ultra.slumber.builtin.datetime.LocalDateTimeCodec
import de.peekandpoke.ultra.slumber.builtin.datetime.ZonedDateTimeCodec
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZonedDateTime
import kotlin.reflect.KType

class DateTimeModule : SlumberModule {

    override fun getAwaker(type: KType, shared: Shared): Awaker? {
        return when (type) {
            LocalDateTime::class -> LocalDateTimeCodec
            Instant::class -> InstantCodec
            ZonedDateTime::class -> ZonedDateTimeCodec
            else -> null
        }
    }

    override fun getSlumberer(type: KType, shared: Shared): Slumberer? {
        return when (type) {
            LocalDateTime::class -> LocalDateTimeCodec
            Instant::class -> InstantCodec
            ZonedDateTime::class -> ZonedDateTimeCodec
            else -> null
        }
    }
}
