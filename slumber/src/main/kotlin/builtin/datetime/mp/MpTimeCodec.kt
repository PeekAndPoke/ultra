package de.peekandpoke.ultra.slumber.builtin.datetime.mp

import de.peekandpoke.ultra.common.datetime.MpInstant
import de.peekandpoke.ultra.common.datetime.MpLocalDate
import de.peekandpoke.ultra.common.datetime.MpLocalDateTime
import de.peekandpoke.ultra.common.datetime.MpZonedDateTime
import de.peekandpoke.ultra.slumber.Awaker
import de.peekandpoke.ultra.slumber.SlumberModule
import de.peekandpoke.ultra.slumber.Slumberer
import kotlin.reflect.KType

object MpTimeCodec : SlumberModule {

    override fun getAwaker(type: KType): Awaker? {

        return when (type.classifier) {

            MpInstant::class ->
                type.wrapIfNonNull(MpInstantAwaker)

            MpLocalDate::class ->
                type.wrapIfNonNull(MpLocalDateAwaker)

            MpLocalDateTime::class ->
                type.wrapIfNonNull(MpLocalDateTimeAwaker)

            MpZonedDateTime::class ->
                type.wrapIfNonNull(MpZonedDateTimeAwaker)

            else -> null
        }
    }

    override fun getSlumberer(type: KType): Slumberer? {

        return when (type.classifier) {

            MpInstant::class ->
                type.wrapIfNonNull(MpInstantSlumberer)

            MpLocalDate::class ->
                type.wrapIfNonNull(MpLocalDateSlumberer)

            MpLocalDateTime::class ->
                type.wrapIfNonNull(MpLocalDateTimeSlumberer)

            MpZonedDateTime::class ->
                type.wrapIfNonNull(MpZonedDateTimeSlumberer)

            else -> null
        }
    }
}
