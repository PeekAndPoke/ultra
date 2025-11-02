package de.peekandpoke.ultra.slumber.builtin.datetime.kotlinx

import de.peekandpoke.ultra.common.TypedAttributes
import de.peekandpoke.ultra.slumber.Awaker
import de.peekandpoke.ultra.slumber.SlumberModule
import de.peekandpoke.ultra.slumber.Slumberer
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
