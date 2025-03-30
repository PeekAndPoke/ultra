package de.peekandpoke.ktorfx.core.broker.vault

import de.peekandpoke.ktorfx.core.broker.CouldNotConvertException
import de.peekandpoke.ktorfx.core.broker.IncomingParamConverter
import de.peekandpoke.ktorfx.core.broker.OutgoingParamConverter
import de.peekandpoke.ultra.common.reflection.kType
import java.time.DateTimeException
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import kotlin.reflect.KClass
import kotlin.reflect.KType


private val supportedClasses = listOf(
    ZoneId::class,
    LocalTime::class,
    Instant::class,
    LocalDate::class,
    LocalDateTime::class,
    ZonedDateTime::class,
)

/**
 * Checks for all types the are supported by [IncomingJavaTimeConverter] and [OutgoingJavaTimeConverter]
 */
private fun supportsType(type: KType): Boolean = type.classifier in supportedClasses

/**
 * Incoming param converter for java.time
 */
class IncomingJavaTimeConverter : IncomingParamConverter {

    companion object {
        private val zZoneId = ZoneId.of("Z")

        // Taken from https://stackoverflow.com/questions/3389348/parse-any-date-in-java
        private val DATE_FORMAT_REGEXPS: Map<Regex, NamedFormat> = mapOf(
            "^\\d{4}-\\d{1,2}-\\d{1,2}$".toRegex()
                    to NamedFormat("yyyy-MM-dd"),

            "^\\d{4}-\\d{1,2}-\\d{1,2}T\\d{2}:\\d{2}$".toRegex()
                    to NamedFormat("yyyy-MM-dd'T'HH:mm"),

            "^\\d{4}-\\d{1,2}-\\d{1,2} \\d{2}:\\d{2}$".toRegex()
                    to NamedFormat("yyyy-MM-dd HH:mm"),

            "^\\d{4}-\\d{1,2}-\\d{1,2}T\\d{2}:\\d{2}:\\d{2}$".toRegex()
                    to NamedFormat("yyyy-MM-dd'T'HH:mm:ss"),

            "^\\d{4}-\\d{1,2}-\\d{1,2} \\d{2}:\\d{2}:\\d{2}$".toRegex()
                    to NamedFormat("yyyy-MM-dd HH:mm:ss"),

            "^\\d{4}-\\d{1,2}-\\d{1,2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}$".toRegex()
                    to NamedFormat("yyyy-MM-dd'T'HH:mm:ss.SSS"),

            "^\\d{4}-\\d{1,2}-\\d{1,2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3}$".toRegex()
                    to NamedFormat("yyyy-MM-dd HH:mm:ss.SSS"),

            "^\\d{4}-\\d{1,2}-\\d{1,2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}\\[.*]$".toRegex()
                    to NamedFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'['VV']'"),

            "^\\d{4}-\\d{1,2}-\\d{1,2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3}\\[.*]$".toRegex()
                    to NamedFormat("yyyy-MM-dd HH:mm:ss.SSS'['VV']'"),

            "^\\d{4}-\\d{1,2}-\\d{1,2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}.*$".toRegex()
                    to NamedFormat("yyyy-MM-dd'T'HH:mm:ss.SSSz"),

            "^\\d{4}-\\d{1,2}-\\d{1,2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3}.*$".toRegex()
                    to NamedFormat("yyyy-MM-dd HH:mm:ss.SSSz"),
        )

        private fun determineDateFormat(dateString: String): NamedFormat? = DATE_FORMAT_REGEXPS.entries
            .firstOrNull { (regex, _) -> regex.matches(dateString) }
            ?.let { (_, format) -> format }

    }

    data class NamedFormat(
        val pattern: String,
        val format: DateTimeFormatter = DateTimeFormatter.ofPattern(pattern),
    )

    override fun canHandle(type: KType): Boolean = supportsType(type)

    suspend fun <T : Any> convert(value: String, cls: KClass<T>): T {
        @Suppress("UNCHECKED_CAST")
        return convert(value, cls.kType().type) as T
    }

    override suspend fun convert(value: String, type: KType): Any {

        return when (type.classifier) {

            ZoneId::class -> try {
                ZoneId.of(
                    ZoneId.SHORT_IDS.getOrDefault(value, value)
                )

            } catch (e: DateTimeException) {
                throw CouldNotConvertException("Could not parse ZoneId '$value'", e)
            }

            LocalTime::class -> try {
                LocalTime.parse(value)
            } catch (e: DateTimeParseException) {
                throw CouldNotConvertException("Could not parse LocalTime '$value'", e)
            }

            else -> {
                val format = determineDateFormat(value)
                    ?: throw CouldNotConvertException("Unknown date format '$value'")

                val parsed = try {
                    format.format.parse(value)
                } catch (e: Throwable) {
                    throw CouldNotConvertException("Could not parse '$value'", e)
                }

                val parsedDate = try {
                    LocalDate.from(parsed)
                } catch (e: Throwable) {
                    LocalDate.MIN
                }

                val parsedTime = try {
                    LocalTime.from(parsed)
                } catch (e: Throwable) {
                    LocalTime.MIN
                }

                val parsedZone = try {
                    ZoneId.from(parsed)
                } catch (e: Throwable) {
                    zZoneId
                }

                val zoned = ZonedDateTime.of(parsedDate, parsedTime, parsedZone)

                when (type.classifier) {

                    Instant::class -> zoned.toInstant()

                    LocalDate::class -> zoned.toLocalDate()

                    LocalDateTime::class -> zoned.toLocalDateTime()

                    else -> zoned
                }
            }
        }
    }
}

/**
 * Outgoing param converter for java.time
 */
class OutgoingJavaTimeConverter : OutgoingParamConverter {

    override fun canHandle(type: KType): Boolean = supportsType(type)

    override fun convert(value: Any, type: KType): String = value.toString()
}
