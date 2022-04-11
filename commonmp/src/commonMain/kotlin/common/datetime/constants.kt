package de.peekandpoke.ultra.common.datetime

// TODO:
//  - MpLocalDate additional test
//  - MpLocalDate.atTime(time): MpLocalDateTime -> is there -> test
//  - MpLocalDate.atTime(time, timezone): MpZonedDatetime
//  - MpLocalDateTime.plus(time): MpLocalDateTime
//  - MpZonedDateTime.plus(time): MpZonedDateTime -> timezone aware adding
//  - MpLocalTimeSlot.of(from: MpLocalTime, duration: Duration)

internal const val ISO_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ"

/**
 * The earliest timestamp used is considered to be '-10000-01-01T00:00:00Z'
 */
const val GENESIS_TIMESTAMP = -377_736_739_200_000L

/**
 * The latest timestamp used is considered to be '+10000-01-01T00:00:00Z'
 */
const val DOOMSDAY_TIMESTAMP = 253_402_300_800_000L

val PortableGenesisDate = PortableDate(GENESIS_TIMESTAMP)
val PortableDoomsdayDate = PortableDate(DOOMSDAY_TIMESTAMP)

val PortableGenesisDateTime = PortableDateTime(GENESIS_TIMESTAMP)
val PortableDoomsdayDateTime = PortableDateTime(DOOMSDAY_TIMESTAMP)
