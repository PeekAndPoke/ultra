package de.peekandpoke.ultra.slumber.builtin

import de.peekandpoke.ultra.slumber.Awaker
import de.peekandpoke.ultra.slumber.Config
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

    override fun getAwaker(type: KType, config: Config): Awaker? {
        return when (type.classifier) {
            LocalDateTime::class -> LocalDateTimeCodec
            Instant::class -> InstantCodec
            ZonedDateTime::class -> ZonedDateTimeCodec
            else -> null
        }
    }

    override fun getSlumberer(type: KType, config: Config): Slumberer? {
        return when (type.classifier) {
            LocalDateTime::class -> LocalDateTimeCodec
            Instant::class -> InstantCodec
            ZonedDateTime::class -> ZonedDateTimeCodec
            else -> null
        }
    }
}
