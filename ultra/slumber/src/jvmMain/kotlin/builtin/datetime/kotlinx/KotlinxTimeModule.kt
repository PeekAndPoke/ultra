package io.peekandpoke.ultra.slumber.builtin.datetime.kotlinx

import io.peekandpoke.ultra.common.TypedAttributes
import io.peekandpoke.ultra.slumber.Awaker
import io.peekandpoke.ultra.slumber.SlumberModule
import io.peekandpoke.ultra.slumber.Slumberer
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlin.reflect.KType

/**
 * SlumberModule for kotlinx-datetime types.
 *
 * Only covers [LocalDate] and [LocalDateTime] - unlike the `javatime` and `mp` modules, there is no
 * codec here for `Instant`, `ZonedDateTime`, `LocalTime`, or `TimeZone`.
 */
object KotlinxTimeModule : SlumberModule {

    override fun getAwaker(type: KType, attributes: TypedAttributes): Awaker? {

        return when (type.classifier) {

            LocalDate::class ->
                type.wrapIfNonNull(LocalDateAwaker)

            LocalDateTime::class ->
                type.wrapIfNonNull(LocalDateTimeAwaker)

            else -> null
        }
    }

    override fun getSlumberer(type: KType, attributes: TypedAttributes): Slumberer? {

        return when (type.classifier) {

            LocalDate::class ->
                type.wrapIfNonNull(LocalDateSlumberer)

            LocalDateTime::class ->
                type.wrapIfNonNull(LocalDateTimeSlumberer)

            else -> null
        }
    }
}
