package io.peekandpoke.ultra.datetime

/**
 * Expected zone math, generated from java.time (`ZonedDateTime.ofLocal`, `LocalDate.atStartOfDay`,
 * `ZonedDateTime.plusDays` / `plusMonths`). Runs on every target via [ZoneParitySpec]; `ZoneParityJvmSpec`
 * recomputes every row from java.time, so the table cannot drift from the spec it encodes.
 */
internal object ZoneParityTable {

    /** [local] in [zone] → [epochSeconds] at [offsetSeconds], which reads back as [resultLocal]. */
    data class LocalCase(
        val zone: String,
        val local: String,
        val epochSeconds: Long,
        val offsetSeconds: Int,
        val resultLocal: String,
    )

    /** The start of [date] in [zone] is [epochSeconds], i.e. the wall time [resultLocal]. */
    data class StartOfDayCase(
        val zone: String,
        val date: String,
        val epochSeconds: Long,
        val resultLocal: String,
    )

    /** [startEpochSeconds] plus [months] then plus [days] in [zone] is [epochSeconds], i.e. [resultLocal]. */
    data class PlusCase(
        val zone: String,
        val startEpochSeconds: Long,
        val days: Int,
        val months: Int,
        val epochSeconds: Long,
        val resultLocal: String,
    )

    val localCases = listOf(
        LocalCase("Europe/Berlin", "2022-07-01T12:00", 1656669600L, 7200, "2022-07-01T12:00"), // normal, summer
        LocalCase("Europe/Berlin", "2022-01-15T12:00", 1642244400L, 3600, "2022-01-15T12:00"), // normal, winter
        LocalCase("Europe/Berlin", "2022-03-27T02:00", 1648342800L, 7200, "2022-03-27T03:00"), // gap start
        LocalCase("Europe/Berlin", "2022-03-27T02:30", 1648344600L, 7200, "2022-03-27T03:30"), // inside gap -> shifted forward
        LocalCase("Europe/Berlin", "2022-03-27T03:00", 1648342800L, 7200, "2022-03-27T03:00"), // gap end
        LocalCase("Europe/Berlin", "2022-03-27T01:59:59", 1648342799L, 3600, "2022-03-27T01:59:59"), // just before gap
        LocalCase("Europe/Berlin", "2022-10-30T02:00", 1667088000L, 7200, "2022-10-30T02:00"), // overlap start -> earlier offset
        LocalCase("Europe/Berlin", "2022-10-30T02:30", 1667089800L, 7200, "2022-10-30T02:30"), // inside overlap -> earlier offset
        LocalCase("Europe/Berlin", "2022-10-30T03:00", 1667095200L, 3600, "2022-10-30T03:00"), // overlap end
        LocalCase("Europe/Berlin", "2022-10-30T01:59:59", 1667087999L, 7200, "2022-10-30T01:59:59"), // just before overlap
        LocalCase("America/New_York", "2022-03-13T02:30", 1647156600L, -14400, "2022-03-13T03:30"), // gap
        LocalCase("America/New_York", "2022-11-06T01:30", 1667712600L, -14400, "2022-11-06T01:30"), // overlap
        LocalCase("Australia/Sydney", "2022-10-02T02:30", 1664641800L, 39600, "2022-10-02T03:30"), // southern hemisphere gap
        LocalCase("Australia/Sydney", "2022-04-03T02:30", 1648913400L, 39600, "2022-04-03T02:30"), // southern hemisphere overlap
        LocalCase("Australia/Lord_Howe", "2022-10-02T02:15", 1664639100L, 39600, "2022-10-02T02:45"), // 30-minute DST gap
        LocalCase("Australia/Lord_Howe", "2022-04-03T01:45", 1648910700L, 39600, "2022-04-03T01:45"), // 30-minute DST overlap
        LocalCase("Asia/Kolkata", "2022-07-01T12:00", 1656657000L, 19800, "2022-07-01T12:00"), // +05:30
        LocalCase("Asia/Kathmandu", "2022-07-01T12:00", 1656656100L, 20700, "2022-07-01T12:00"), // +05:45
        LocalCase("America/St_Johns", "2022-11-06T01:30", 1667707200L, -9000, "2022-11-06T01:30"), // -02:30/-03:30 overlap
        LocalCase("America/Santiago", "2022-09-11T00:30", 1662870600L, -10800, "2022-09-11T01:30"), // gap at midnight
        LocalCase("America/Sao_Paulo", "2018-11-04T00:30", 1541302200L, -7200, "2018-11-04T01:30"), // gap at midnight (historic rules)
        LocalCase("Europe/Moscow", "2011-06-01T12:00", 1306915200L, 14400, "2011-06-01T12:00"), // rule change: permanent +04 from 2011
        LocalCase("Europe/Moscow", "2015-06-01T12:00", 1433149200L, 10800, "2015-06-01T12:00"), // rule change: +03 from 2014
        LocalCase("Pacific/Apia", "2011-12-30T12:00", 1325282400L, 50400, "2011-12-31T12:00"), // a skipped calendar day
        LocalCase("Pacific/Apia", "2011-12-29T23:00", 1325235600L, -36000, "2011-12-29T23:00"), // last hour before the skipped day
        LocalCase("Pacific/Kiritimati", "1994-12-31T12:00", 788911200L, 50400, "1995-01-01T12:00"), // a skipped calendar day
        LocalCase("Europe/Berlin", "1850-01-01T00:00", -3786828808L, 3208, "1850-01-01T00:00"), // local mean time, seconds offset
        LocalCase("Europe/Berlin", "0039-06-15T12:00", -60922155208L, 3208, "0039-06-15T12:00"), // two-digit year
        LocalCase("Europe/Berlin", "-0099-06-15T12:00", -65277060808L, 3208, "-0099-06-15T12:00"), // before the common era
        LocalCase("Europe/Berlin", "2037-07-01T12:00", 2130055200L, 7200, "2037-07-01T12:00"), // far future, rule based
        LocalCase("Asia/Tokyo", "2100-07-01T12:00", 4118094000L, 32400, "2100-07-01T12:00"), // far future, no DST
        LocalCase("Etc/GMT-14", "2022-07-01T12:00", 1656626400L, 50400, "2022-07-01T12:00"), // Etc zone, inverted sign
        LocalCase("+05:30", "2022-07-01T12:00", 1656657000L, 19800, "2022-07-01T12:00"), // fixed offset id
        LocalCase("UTC+01:00", "2022-07-01T12:00", 1656673200L, 3600, "2022-07-01T12:00"), // prefixed fixed offset id
        LocalCase("GMT-3", "2022-07-01T12:00", 1656687600L, -10800, "2022-07-01T12:00"), // prefixed one-digit fixed offset id
        LocalCase("America/Toronto", "1919-03-31T00:00", -1601751600L, -14400, "1919-03-31T01:00"), // midnight strictly inside the gap -> shifted forward
        LocalCase("UTC", "2022-03-27T02:30", 1648348200L, 0, "2022-03-27T02:30"), // UTC
    )

    val startOfDayCases = listOf(
        StartOfDayCase("Europe/Berlin", "2022-03-27", 1648335600L, "2022-03-27T00:00"), // gap not at midnight
        StartOfDayCase("Europe/Berlin", "2022-10-30", 1667080800L, "2022-10-30T00:00"), // overlap not at midnight
        StartOfDayCase("America/Santiago", "2022-09-11", 1662868800L, "2022-09-11T01:00"), // midnight in the gap -> end of gap
        StartOfDayCase("America/Sao_Paulo", "2018-11-04", 1541300400L, "2018-11-04T01:00"), // midnight in the gap -> end of gap
        StartOfDayCase("America/Havana", "2022-03-13", 1647147600L, "2022-03-13T01:00"), // midnight in the gap -> end of gap
        StartOfDayCase("Asia/Gaza", "2022-03-27", 1648332000L, "2022-03-27T01:00"), // gap at midnight
        StartOfDayCase("Pacific/Apia", "2011-12-31", 1325239200L, "2011-12-31T00:00"), // the day after the skipped day
        StartOfDayCase("America/Toronto", "1919-03-31", -1601753400L, "1919-03-31T00:30"), // midnight strictly inside the gap (23:30 to 00:30) -> end of gap
        StartOfDayCase("Australia/Lord_Howe", "2022-10-02", 1664631000L, "2022-10-02T00:00"), // 30-minute gap, not at midnight
    )

    val plusCases = listOf(
        PlusCase("Europe/Berlin", 1648258200L, days = 1, months = 0, 1648344600L, "2022-03-27T03:30"), // plus one day into the gap -> shifted forward
        PlusCase("Europe/Berlin", 1666485000L, days = 7, months = 0, 1667089800L, "2022-10-30T02:30"), // into the overlap from summer time -> keeps +02
        PlusCase("Europe/Berlin", 1667698200L, days = -7, months = 0, 1667093400L, "2022-10-30T02:30"), // into the overlap from winter time -> keeps +01
        PlusCase("Europe/Berlin", 1645925400L, days = 0, months = 1, 1648344600L, "2022-03-27T03:30"), // plus one month into the gap
        PlusCase("Europe/Berlin", 1648335600L, days = 1, months = 0, 1648418400L, "2022-03-28T00:00"), // a 23 hour day
        PlusCase("Europe/Berlin", 1667080800L, days = 1, months = 0, 1667170800L, "2022-10-31T00:00"), // a 25 hour day
        PlusCase("America/New_York", 1643648400L, days = 0, months = 1, 1646067600L, "2022-02-28T12:00"), // end of month clamps
        PlusCase("Pacific/Apia", 1325196000L, days = 1, months = 0, 1325282400L, "2011-12-31T12:00"), // plus one day onto the skipped day
    )
}
