package de.peekandpoke.ultra.common.datetime

import de.peekandpoke.ultra.common.datetime.kotlinx.offsetMillisAt
import kotlinx.datetime.TimeZone

object TestConstants {

    const val tsUTC_20220405_000000: Long = 1649116800000L

    const val tsUtc_20220405_121314: Long = 1649160794000L

    val tsBucharest_20220405_121314: Long = (tsUtc_20220405_121314 -
            TimeZone.of("Europe/Bucharest").offsetMillisAt(
                MpInstant.fromEpochMillis(tsUtc_20220405_121314)
            ))
}
