/**
 * TypeScript mirror of the ultra/datetime types.
 *
 * HAND-WRITTEN AND CHECKED IN — not generated. These shapes come from custom Slumber codecs
 * (`ultra/slumber/src/jvmMain/kotlin/builtin/datetime/mp/`), so they cannot be derived from the Kotlin
 * type graph. `MpInstant` is a data class over a single `millis` value, yet slumbers to
 * `{ts, timezone, human}` — nothing in its type says so. Note also that two of these are NOT objects.
 *
 * When a codec under `builtin/datetime/mp/` changes, change this file in the same commit.
 * `MpDateTimeFieldParitySpec` fails if the two drift.
 */
import { z } from 'zod'

/**
 * The shape shared by the timestamp-carrying types.
 *
 * `ts` is epoch milliseconds; `timezone` is an IANA id; `human` is an ISO string, advisory only —
 * do not parse it, use `ts`.
 */
const timestamped = {
    ts: z.number(),
    timezone: z.string(),
    human: z.string(),
}

/** A point on the timeline. Always serialized with timezone UTC. */
export const MpInstant = z.object(timestamped)
export type MpInstant = z.infer<typeof MpInstant>

/** A date with no time. `ts` is its start-of-day in UTC. */
export const MpLocalDate = z.object(timestamped)
export type MpLocalDate = z.infer<typeof MpLocalDate>

/** A date and time with no zone. `ts` is its instant interpreted as UTC. */
export const MpLocalDateTime = z.object(timestamped)
export type MpLocalDateTime = z.infer<typeof MpLocalDateTime>

/** A point on the timeline plus the zone it should be displayed in. */
export const MpZonedDateTime = z.object(timestamped)
export type MpZonedDateTime = z.infer<typeof MpZonedDateTime>

/**
 * A time of day, as milliseconds since midnight.
 *
 * A BARE NUMBER on the wire, not an object — `MpLocalTimeCodec` returns `inWholeMilliSeconds()`.
 */
export const MpLocalTime = z.number()
export type MpLocalTime = z.infer<typeof MpLocalTime>

/**
 * An IANA timezone id, e.g. `Europe/Berlin`.
 *
 * A BARE STRING on the wire, not an object — `MpTimezoneCodec` returns `id`.
 */
export const MpTimezone = z.string()
export type MpTimezone = z.infer<typeof MpTimezone>

//  Helpers  ///////////////////////////////////////////////////////////////////////////////////////

/** Converts any of the timestamp-carrying types to a JavaScript Date. */
export function toDate(value: { ts: number }): Date {
    return new Date(value.ts)
}

/** Milliseconds since midnight to a `HH:mm` string. */
export function localTimeToHhMm(value: MpLocalTime): string {
    const totalMinutes = Math.floor(value / 60_000)
    const hours = Math.floor(totalMinutes / 60)
    const minutes = totalMinutes % 60

    return `${String(hours).padStart(2, '0')}:${String(minutes).padStart(2, '0')}`
}
