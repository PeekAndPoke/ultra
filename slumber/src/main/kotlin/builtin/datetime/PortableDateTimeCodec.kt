package de.peekandpoke.ultra.slumber.builtin.datetime

import de.peekandpoke.ultra.common.datetime.PortableDateTime
import de.peekandpoke.ultra.common.datetime.date
import de.peekandpoke.ultra.slumber.Awaker
import de.peekandpoke.ultra.slumber.Slumberer
import java.time.ZonedDateTime

object PortableDateTimeAwaker : Awaker {

    override fun awake(data: Any?, context: Awaker.Context): PortableDateTime? {

        if (data !is Map<*, *>) {
            return null
        }

        return when (val ts = data[TS]) {
            is Number -> PortableDateTime(ts.toLong())
            else -> null
        }
    }
}

object PortableDateTimeSlumberer : Slumberer {

    override fun slumber(data: Any?, context: Slumberer.Context): Map<String, Any>? {

        if (data !is PortableDateTime) {
            return null
        }

        val zoned: ZonedDateTime = data.date.atZone(utc)

        return toMap(
            data.timestamp,
            utc,
            zoned.toString()
        )
    }
}
