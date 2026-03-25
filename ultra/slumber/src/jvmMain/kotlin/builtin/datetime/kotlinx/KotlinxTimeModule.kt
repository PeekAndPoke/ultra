package io.peekandpoke.ultra.slumber.builtin.datetime.kotlinx

import io.peekandpoke.ultra.common.TypedAttributes
import io.peekandpoke.ultra.slumber.Awaker
import io.peekandpoke.ultra.slumber.SlumberModule
import io.peekandpoke.ultra.slumber.Slumberer
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlin.reflect.KType

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
