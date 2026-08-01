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
 * ### Only [ts] is a queryable contract
 *
 * [timezone] is a literal `"UTC"` for [MpInstant], [MpLocalDateTime] and [MpLocalDate] — only
 * [MpZonedDateTime] writes a real zone. So `FILTER(x.createdAt.timezone EQ "Europe/Berlin")` on an
 * [MpInstant] field compiles, type-checks and matches nothing, forever.
 *
 * [human] is a debug-only rendering. Neither codec reads it back — both reconstruct from [ts] — so it
 * is nullable with a default purely so a payload that omits it still decodes. Its format is not a
 * stable contract: sorting or indexing on it works only by accident of ISO strings sorting
 * lexicographically, and breaks silently if the rendering ever changes.
 *
 * ### The nullability of [human] is inconsistent in both directions — deliberately, for now
 *
 * The slumber codecs always EMIT a non-null `human`, so this declaration is weaker than reality; the
 * `String? = ""` exists for kotlinx decode leniency and is kept verbatim so that behaviour is unchanged.
 * In the other direction, the karango/monko generators drop nullability entirely when rendering a path
 * type, so they emit `String`. A consumer that derives a schema from this class — `ultra:codegen` — will
 * inherit the nullable form and should narrow it deliberately rather than by accident. Tightening the
 * field here would also tighten kotlinx decoding of payloads that omit it, which is a behaviour change
 * on a battle-tested path and the maintainer's call.
 */
@Serializable
data class MpDateTimeRawData(
    val ts: Long,
    val timezone: String,
    val human: String? = "",
)
