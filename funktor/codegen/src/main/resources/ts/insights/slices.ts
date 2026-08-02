/**
 * Readers for the collector slices.
 *
 * ## Why these are hand-written and defensive
 *
 * `InsightsCollectorSlice.data` is a `JsonElement` on purpose: the envelope must not enumerate the
 * collectors, or an app-defined one becomes impossible. Its KDoc says each collector's `Data` class is a
 * codegen root so a tab gets a generated schema -- **that is aspirational, not true today.** `models.ts`
 * carries the four envelope types and no collector `Data` type. Nor is it uniformly achievable:
 * `AppConfigCollector.Data(info: Any, config: Any)` has no derivable shape at all.
 *
 * So these are hand-written, and they are **narrowing readers, not casts**. Two reasons that matters:
 *
 * 1. **Records outlive the code that wrote them.** They sit in a depot; a record written last month may
 *    predate a field. A reader that assumes today's shape turns an old record into a crash.
 * 2. **A tab that throws takes the whole page with it.** Every reader returns null instead, and the tab
 *    falls back to a raw JSON tree -- degraded, but the data is still there and readable.
 *
 * Wire shapes come from `funktor/insights/reference/TAB-SPECS.md`, which read them out of a real record
 * rather than inferring them from the Kotlin types -- several differ.
 *
 * **No parity spec guards this against the Kotlin collectors** (maintainer, 2026-08-02): backend and
 * frontend are assumed in sync, which the design gives us -- the SDK is generated per app from the
 * running server, nothing is published, and there is no back-compat requirement. The narrowing below is
 * what makes that affordable: a renamed field degrades to a visible gap, not a corrupted value.
 *
 * **Everything here is attacker-controlled.** These readers narrow TYPES; they do not sanitise, and no
 * sanitising is wanted. Rendering escapes.
 */

/*  Primitive narrowing  ------------------------------------------------------------------------------  */

export function asRecord(value: unknown): Record<string, unknown> | null {
    if (value === null || typeof value !== 'object' || Array.isArray(value)) return null
    return value as Record<string, unknown>
}

export function asArray(value: unknown): unknown[] | null {
    return Array.isArray(value) ? value : null
}

export function asString(value: unknown): string | null {
    return typeof value === 'string' ? value : null
}

export function asNumber(value: unknown): number | null {
    // NaN is excluded deliberately: it would format as "NaN" in a cell that is supposed to read n/a.
    return typeof value === 'number' && !Number.isNaN(value) ? value : null
}

export function asBoolean(value: unknown): boolean | null {
    return typeof value === 'boolean' ? value : null
}

/**
 * Ktor's `HttpMethod` and `HttpStatusCode` serialise as objects, not scalars -- `{value}` and
 * `{value, description}`. A bare scalar is accepted too, because that is what an app-defined collector
 * or a future reshape would most likely write, and reading it costs nothing.
 */
export function asWrappedString(value: unknown): string | null {
    const direct = asString(value)
    if (direct !== null) return direct
    return asString(asRecord(value)?.value)
}

export function asWrappedNumber(value: unknown): number | null {
    const direct = asNumber(value)
    if (direct !== null) return direct
    return asNumber(asRecord(value)?.value)
}

/**
 * A header/query map: `{ "Name": ["v1", "v2"] }`.
 *
 * A bare string value is lifted into a single-element list rather than dropped -- losing a header
 * because it was not wrapped would be a silent hole in exactly the tab someone opened to find it.
 */
export function asStringListMap(value: unknown): Record<string, string[]> | null {
    const record = asRecord(value)
    if (record === null) return null

    const out: Record<string, string[]> = {}

    for (const [key, raw] of Object.entries(record)) {
        const list = asArray(raw)
        if (list !== null) {
            out[key] = list.map((item) => asString(item) ?? JSON.stringify(item) ?? String(item))
            continue
        }
        const single = asString(raw)
        out[key] = single !== null ? [single] : [JSON.stringify(raw) ?? String(raw)]
    }

    return out
}

/*  Slice readers  ------------------------------------------------------------------------------------  */

export interface RequestSlice {
    method: string | null
    scheme: string | null
    host: string | null
    port: number | null
    /** Path only -- scheme, host and query string are not in it. See {@link composeUrl}. */
    uri: string | null
    headers: Record<string, string[]>
    queryParams: Record<string, string[]>
}

export function readRequest(data: unknown): RequestSlice | null {
    const record = asRecord(data)
    if (record === null) return null

    return {
        method: asWrappedString(record.method),
        scheme: asString(record.scheme),
        host: asString(record.host),
        port: asNumber(record.port),
        uri: asString(record.uri),
        headers: asStringListMap(record.headers) ?? {},
        queryParams: asStringListMap(record.queryParams) ?? {},
    }
}

/**
 * Rebuild the full URL the old `Data.fullUrl` used to carry.
 *
 * It was removed from the record because a computed property skewed the stored shape, so the frontend
 * composes it. The default port is left in rather than hidden -- this is a debugging view, and "which
 * port did this actually arrive on" is a question it exists to answer.
 */
export function composeUrl(slice: RequestSlice): string | null {
    if (slice.uri === null) return null
    if (slice.scheme === null || slice.host === null) return slice.uri

    const port = slice.port === null ? '' : `:${slice.port}`
    return `${slice.scheme}://${slice.host}${port}${slice.uri}`
}

export interface ResponseSlice {
    status: number | null
    statusDescription: string | null
    headers: Record<string, string[]>
}

export function readResponse(data: unknown): ResponseSlice | null {
    const record = asRecord(data)
    if (record === null) return null

    return {
        status: asWrappedNumber(record.status),
        statusDescription: asString(asRecord(record.status)?.description),
        headers: asStringListMap(record.headers) ?? {},
    }
}

export interface UserSlice {
    /** A flat string -- the `UserId` value class does not survive as an object. */
    userId: string | null
    clientIp: string | null
    email: string | null
    desc: string | null
    type: string | null
    isSystem: boolean | null
    isAnonymous: boolean | null
    isSuperUser: boolean | null
    /** Left raw: the permission sets are lists whose shape is the app's, not the framework's. */
    permissions: unknown
}

export function readUser(data: unknown): UserSlice | null {
    const record = asRecord(data)
    if (record === null) return null

    const user = asRecord(record.user) ?? {}
    const permissions = asRecord(record.permissions) ?? {}

    return {
        userId: asString(user.userId),
        clientIp: asString(user.clientIp),
        email: asString(user.email),
        desc: asString(user.desc),
        type: asString(user.type),
        isSystem: asBoolean(user.isSystem),
        isAnonymous: asBoolean(user.isAnonymous),
        isSuperUser: asBoolean(permissions.isSuperUser),
        permissions: record.permissions,
    }
}

export function readRoutingTrace(data: unknown): string | null {
    return asString(asRecord(data)?.trace)
}

/**
 * View render time in nanoseconds.
 *
 * **Null is the NORMAL case, not an error** -- it is null on every API request, which is most of them.
 * A tab that treats it as a failure would report a problem on almost every record.
 */
export function readTemplateTimeNs(data: unknown): number | null {
    return asNumber(asRecord(data)?.timeNs)
}

/** `level` is the enum NAME: ALL TRACE DEBUG INFO WARNING ERROR OFF. */
export type LogLevel = 'ALL' | 'TRACE' | 'DEBUG' | 'INFO' | 'WARNING' | 'ERROR' | 'OFF'

export interface LogEntry {
    level: string | null
    /** Already formatted by `LogAppender.format` -- render as-is, do not re-parse. */
    text: string | null
}

export function readLogEntries(data: unknown): LogEntry[] | null {
    const entries = asArray(asRecord(data)?.entries)
    if (entries === null) return null

    return entries.map((raw) => {
        const record = asRecord(raw) ?? {}
        return { level: asString(record.level), text: asString(record.text) }
    })
}

/**
 * Log level to tone.
 *
 * `OFF` is grouped with `ERROR` because that is what the old GUI did, and it is right: `OFF` reaching an
 * appender at all means something logged above every threshold.
 */
export function toneForLogLevel(level: string | null): 'ok' | 'warn' | 'error' | 'neutral' {
    switch (level) {
        case 'INFO':
            return 'ok'
        case 'WARNING':
            return 'warn'
        case 'ERROR':
        case 'OFF':
            return 'error'
        default:
            return 'neutral'
    }
}

/** Nanoseconds as milliseconds, at the precision the old GUI used. Null stays null. */
export function nsToMs(ns: number | null): string | null {
    return ns === null ? null : `${(ns / 1_000_000).toFixed(2)} ms`
}
