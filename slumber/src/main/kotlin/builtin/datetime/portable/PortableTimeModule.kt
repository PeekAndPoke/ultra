package de.peekandpoke.ultra.slumber.builtin.datetime.portable

import de.peekandpoke.ultra.common.datetime.PortableDate
import de.peekandpoke.ultra.common.datetime.PortableDateTime
import de.peekandpoke.ultra.common.datetime.PortableTime
import de.peekandpoke.ultra.slumber.Awaker
import de.peekandpoke.ultra.slumber.SlumberModule
import de.peekandpoke.ultra.slumber.Slumberer
import kotlin.reflect.KType

object PortableTimeModule : SlumberModule {

    override fun getAwaker(type: KType): Awaker? {
        return when (type.classifier) {
            PortableTime::class ->
                type.wrapIfNonNull(PortableTimeAwaker)

            PortableDate::class ->
                type.wrapIfNonNull(PortableDateAwaker)

            PortableDateTime::class ->
                type.wrapIfNonNull(PortableDateTimeAwaker)

            else -> null
        }
    }

    override fun getSlumberer(type: KType): Slumberer? {

        return when (type.classifier) {
            PortableTime::class ->
                type.wrapIfNonNull(PortableTimeSlumberer)

            PortableDate::class ->
                type.wrapIfNonNull(PortableDateSlumberer)

            PortableDateTime::class ->
                type.wrapIfNonNull(PortableDateTimeSlumberer)

            else -> null
        }
    }
}
