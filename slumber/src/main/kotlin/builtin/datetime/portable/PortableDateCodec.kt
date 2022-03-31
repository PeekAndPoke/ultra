package de.peekandpoke.ultra.slumber.builtin.datetime.portable

import de.peekandpoke.ultra.common.datetime.PortableDate
import de.peekandpoke.ultra.common.datetime.date
import de.peekandpoke.ultra.slumber.Awaker
import de.peekandpoke.ultra.slumber.Slumberer
import de.peekandpoke.ultra.slumber.builtin.datetime.TS
import de.peekandpoke.ultra.slumber.builtin.datetime.format
import de.peekandpoke.ultra.slumber.builtin.datetime.toMap
import de.peekandpoke.ultra.slumber.builtin.datetime.utc
import java.time.ZonedDateTime

object PortableDateAwaker : Awaker {

    override fun awake(data: Any?, context: Awaker.Context): PortableDate? {

        if (data !is Map<*, *>) {
            return null
        }

        return when (val ts = data[TS]) {
            is Number -> PortableDate(ts.toLong())
            else -> null
        }
    }
}

object PortableDateSlumberer : Slumberer {

    override fun slumber(data: Any?, context: Slumberer.Context): Map<String, Any>? {

        if (data !is PortableDate) {
            return null
        }

        val zoned: ZonedDateTime = data.date.atStartOfDay(utc)

        return toMap(
            data.timestamp,
            utc,
            zoned.format(format)
        )
    }
}
