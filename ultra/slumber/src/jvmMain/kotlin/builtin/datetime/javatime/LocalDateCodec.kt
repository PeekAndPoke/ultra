package io.peekandpoke.ultra.slumber.builtin.datetime.javatime

import io.peekandpoke.ultra.slumber.Awaker
import io.peekandpoke.ultra.slumber.Slumberer
import io.peekandpoke.ultra.slumber.builtin.datetime.TS
import io.peekandpoke.ultra.slumber.builtin.datetime.toMap
import io.peekandpoke.ultra.slumber.builtin.datetime.utc
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime

object LocalDateAwaker : Awaker {

    override fun awake(data: Any?, context: Awaker.Context): LocalDate? {

        if (data !is Map<*, *>) {
            return null
        }

        return when (val ts = data[TS]) {
            is Number -> LocalDateTime.ofInstant(
                Instant.ofEpochMilli(ts.toLong()),
                utc
            ).toLocalDate()
            else -> null
        }
    }
}

object LocalDateSlumberer : Slumberer {

    override fun slumber(data: Any?, context: Slumberer.Context): Map<String, Any>? {

        if (data !is LocalDate) {
            return null
        }

        val zoned = data.atStartOfDay(utc)

        return toMap(
            zoned.toInstant().toEpochMilli(),
            utc,
            zoned.toString()
        )
    }
}
