package io.peekandpoke.ultra.datetime

import kotlinx.serialization.Serializable

/**
 * The wire shape the four object-shaped `Mp*` date/time types serialize to: [MpInstant],
 * [MpZonedDateTime], [MpLocalDateTime] and [MpLocalDate].
 *
 * Used by two independent codecs, which is why it is public rather than an implementation detail of
 * either: the kotlinx serializers in this module encode and decode it directly, and slumber declares it
 * through `@Slumber.As` so karango, monko and the TypeScript generator can read the shape instead of
 * mirroring it by hand.
 *
 * [human] is a debug-only rendering. Neither codec reads it back — both reconstruct from [ts] — so it
 * is nullable with a default purely so a payload that omits it still decodes.
 */
@Serializable
data class MpDateTimeRawData(
    val ts: Long,
    val timezone: String,
    val human: String? = "",
)
