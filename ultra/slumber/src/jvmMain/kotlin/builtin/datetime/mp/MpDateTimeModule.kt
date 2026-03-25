package io.peekandpoke.ultra.slumber.builtin.datetime.mp

import io.peekandpoke.ultra.common.TypedAttributes
import io.peekandpoke.ultra.datetime.MpInstant
import io.peekandpoke.ultra.datetime.MpLocalDate
import io.peekandpoke.ultra.datetime.MpLocalDateTime
import io.peekandpoke.ultra.datetime.MpLocalTime
import io.peekandpoke.ultra.datetime.MpTimezone
import io.peekandpoke.ultra.datetime.MpZonedDateTime
import io.peekandpoke.ultra.slumber.Awaker
import io.peekandpoke.ultra.slumber.SlumberModule
import io.peekandpoke.ultra.slumber.Slumberer
import kotlin.reflect.KType

object MpDateTimeModule : SlumberModule {

    override fun getAwaker(type: KType, attributes: TypedAttributes): Awaker? {

        return when (type.classifier) {

            MpInstant::class ->
                type.wrapIfNonNull(MpInstantAwaker)

            MpLocalDate::class ->
                type.wrapIfNonNull(MpLocalDateAwaker)

            MpLocalDateTime::class ->
                type.wrapIfNonNull(MpLocalDateTimeAwaker)

            MpLocalTime::class ->
                type.wrapIfNonNull(MpLocalTimeAwaker)

            MpZonedDateTime::class ->
                type.wrapIfNonNull(MpZonedDateTimeAwaker)

            MpTimezone::class ->
                type.wrapIfNonNull(MpTimezoneAwaker)

            else -> null
        }
    }

    override fun getSlumberer(type: KType, attributes: TypedAttributes): Slumberer? {

        return when (type.classifier) {

            MpInstant::class ->
                type.wrapIfNonNull(MpInstantSlumberer)

            MpLocalDate::class ->
                type.wrapIfNonNull(MpLocalDateSlumberer)

            MpLocalDateTime::class ->
                type.wrapIfNonNull(MpLocalDateTimeSlumberer)

            MpLocalTime::class ->
                type.wrapIfNonNull(MpLocalTimeSlumberer)

            MpZonedDateTime::class ->
                type.wrapIfNonNull(MpZonedDateTimeSlumberer)

            MpTimezone::class ->
                type.wrapIfNonNull(MpTimezoneSlumberer)

            else -> null
        }
    }
}
