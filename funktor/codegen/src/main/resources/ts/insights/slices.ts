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

import type { StatCell } from '../ui/types.ts'

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

    // `Object.create(null)`, not `{}`. A header or query parameter named `__proto__` hits
    // `Object.prototype`'s setter on assignment: no own property is created, so the table silently
    // OMITS it -- while the raw-slice tree beside it shows it, because `JSON.parse` makes it an own
    // property. Two views of one record disagreeing, in a forensic tool, on a key an attacker picks.
    // `GET /x?__proto__=1` is the whole attack. Found by the review gate, 2026-08-24.
    const out: Record<string, string[]> = Object.create(null)

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
    /**
     * The polymorphic discriminator: `anonymous` | `system` | `logged-in` | `api-key`.
     *
     * Replaces the `isSystem` / `isAnonymous` rows this reader used to claim. Those are FUNCTIONS on
     * `UserRecord`, not properties, so Slumber never emitted them and both rows read `n/a` on every
     * record ever written. `_type` is what actually distinguishes an api-key caller from a session --
     * a distinction `type` (an app-supplied string) does not make. Found by the review gate, 2026-08-24.
     */
    kind: string | null
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
        kind: asString(user._type),
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
export interface TemplateSlice {
    /** Null is the NORMAL case -- it is null on every request that rendered no view. */
    timeNs: number | null
}

/**
 * Returns null only when the slice is UNRECOGNISABLE, so the tab can tell that apart from
 * `{timeNs: null}`.
 *
 * The two used to collapse into one null, and the tab reported both as "no view was rendered" -- so a
 * record whose field had been renamed showed a request that took 4.5 ms as one that rendered nothing,
 * with the data displayed nowhere. Found by the review gate, 2026-08-24.
 */
export function readTemplate(data: unknown): TemplateSlice | null {
    const record = asRecord(data)
    if (record === null || !('timeNs' in record)) return null

    return { timeNs: asNumber(record.timeNs) }
}

/**
 * One log line from the record.
 *
 * `level` is the enum NAME — ALL, TRACE, DEBUG, INFO, WARNING, ERROR, OFF — and is deliberately
 * typed `string`, because it arrives inside an open collector envelope rather than through a
 * schema. The generated `LogLevel` in `../models.ts` is the authority on the value set; there was a
 * hand-written copy of that union here and it is gone (2026-08-02): nothing referenced it, and a
 * copy of a generated enum drifts silently the moment Kotlin gains a level.
 *
 * It also collided. `export *` through the SDK barrel turns two modules exporting one name into
 * TS2308, which is the barrel doing its job.
 */
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

/*  runtime  ------------------------------------------------------------------------------------------  */

export interface RuntimeSlice {
    jvmVersion: string | null
    kotlinVersion: string | null
    cpus: number | null
    maxMem: number | null
    reservedMem: number | null
    freeMem: number | null
    openFileDescriptors: number | null
    maxFileDescriptors: number | null
    systemProperties: Record<string, string>
}

export function readRuntime(data: unknown): RuntimeSlice | null {
    const record = asRecord(data)
    if (record === null) return null

    const properties = asRecord(record.systemProperties) ?? {}
    // Null-prototype for the same reason as `asStringListMap` -- a property named `__proto__`.
    const systemProperties: Record<string, string> = Object.create(null)
    for (const [key, value] of Object.entries(properties)) {
        systemProperties[key] = asString(value) ?? String(value)
    }

    return {
        jvmVersion: asString(record.jvmVersion),
        kotlinVersion: asString(record.kotlinVersion),
        cpus: asNumber(record.cpus),
        maxMem: asNumber(record.maxMem),
        reservedMem: asNumber(record.reservedMem),
        freeMem: asNumber(record.freeMem),
        openFileDescriptors: asNumber(record.openFileDescriptors),
        maxFileDescriptors: asNumber(record.maxFileDescriptors),
        systemProperties,
    }
}

/**
 * Bytes as whole megabytes, the unit the old TAB used.
 *
 * The old insights BAR showed the same three figures in GB to two decimals. That difference was
 * deliberate, and the tab's unit is the one that survives -- a bar had one line, a tab has a column.
 */
export function bytesToMb(bytes: number | null): string | null {
    return bytes === null ? null : `${Math.round(bytes / (1024 * 1024))} MB`
}

/*  vault  --------------------------------------------------------------------------------------------  */

/** One of the five sub-measures. Both fields are summed across entries for the header strip. */
export interface VaultMeasure {
    totalNs: number
    count: number
}

export interface VaultEntry {
    connection: string | null
    count: number | null
    totalCount: number | null
    /**
     * Placeholder-ised query text, or null when the driver could not provide one safely.
     *
     * **Guaranteed by the backend, not checked here.** `VaultCollector.Data.of` drops the text for any
     * driver that inlines its bind values, so this field never contains a value. The frontend used to
     * carry a language allowlist to decide that; enforcing it at the one place that turns a driver's
     * output into a record made every consumer's copy unnecessary.
     */
    query: string | null
    queryLanguage: string | null
    /** How many bind variables the query had. The VALUES are deliberately not recorded. */
    varsCount: number
    /**
     * True when the slice predates `VaultCollector.Data` and carries the raw profiler entry.
     *
     * Detected by the presence of `vars`, a field only the OLD shape has. It matters because the "query
     * text contains no bind values" guarantee is enforced by the collector, and a record written before
     * that collector existed never had it — Jackson serialised the profiler entry whole, values
     * included. Records outlive the code that wrote them, which is the premise of this entire file.
     */
    legacy: boolean
    totalNs: number
    serializer: VaultMeasure
    query_: VaultMeasure
    iterator: VaultMeasure
    deserializer: VaultMeasure
    explain: VaultMeasure
}

export interface VaultSlice {
    entries: VaultEntry[]
    /** Summed here because every total was a private `lazy` and is therefore NOT in the record. */
    totalNs: number
    serializer: VaultMeasure
    query: VaultMeasure
    iterator: VaultMeasure
    deserializer: VaultMeasure
    explain: VaultMeasure
}

function readMeasure(value: unknown): VaultMeasure {
    const record = asRecord(value)
    return { totalNs: asNumber(record?.totalNs) ?? 0, count: asNumber(record?.count) ?? 0 }
}

function sumMeasures(measures: VaultMeasure[]): VaultMeasure {
    return measures.reduce(
        (acc, measure) => ({ totalNs: acc.totalNs + measure.totalNs, count: acc.count + measure.count }),
        { totalNs: 0, count: 0 },
    )
}

export function readVault(data: unknown): VaultSlice | null {
    const rawEntries = asArray(asRecord(data)?.entries)
    if (rawEntries === null) return null

    const entries: VaultEntry[] = rawEntries.map((raw) => {
        const record = asRecord(raw) ?? {}
        const legacy = 'vars' in record || 'queryExplained' in record
        return {
            connection: asString(record.connection),
            count: asNumber(record.count),
            totalCount: asNumber(record.totalCount),
            // A legacy entry's `query` may hold inlined bind values, so it is dropped rather than
            // trusted. Its `vars` and `queryExplained` are simply never read by this file.
            query: legacy ? null : asString(record.query),
            queryLanguage: asString(record.queryLanguage),
            varsCount: asNumber(record.varsCount) ?? (legacy ? Object.keys(asRecord(record.vars) ?? {}).length : 0),
            legacy,
            totalNs: asNumber(record.totalNs) ?? 0,
            serializer: readMeasure(record.measureSerializer),
            query_: readMeasure(record.measureQuery),
            iterator: readMeasure(record.measureIterator),
            deserializer: readMeasure(record.measureDeserializer),
            explain: readMeasure(record.measureExplain),
        }
    })

    return {
        entries,
        totalNs: entries.reduce((sum, entry) => sum + entry.totalNs, 0),
        serializer: sumMeasures(entries.map((entry) => entry.serializer)),
        query: sumMeasures(entries.map((entry) => entry.query_)),
        iterator: sumMeasures(entries.map((entry) => entry.iterator)),
        deserializer: sumMeasures(entries.map((entry) => entry.deserializer)),
        explain: sumMeasures(entries.map((entry) => entry.explain)),
    }
}

/*  kontainer  ----------------------------------------------------------------------------------------  */

export interface KontainerInjection {
    name: string | null
    classes: string[]
    provisionType: string | null
}

export interface KontainerDefinition {
    creates: string | null
    injectionType: string | null
    injects: KontainerInjection[]
    codeLocation: string | null
    overwrites: KontainerDefinition | null
}

export interface KontainerInstance {
    cls: string | null
    /** Epoch MILLIS, normalised from either shape by {@link readInstantMillis}. */
    createdAtMillis: number | null
}

/**
 * An instant, as epoch MILLIS, from either shape a record can carry.
 *
 * `DebugInfo.InstanceDebugInfo.createdAt` is a `java.time.Instant`, which Slumber writes as an OBJECT
 * (`{ts, timezone, human}`, `ts` in millis) -- not a number. The Jackson-era writer emitted epoch
 * SECONDS as a double instead, so both shapes exist in a depot.
 *
 * Reading only the number was the one place in this file that could silently MISREAD rather than
 * degrade: a millis value multiplied by 1000 lands in the year 58,000. Found by the review gate,
 * 2026-08-24.
 */
export function readInstantMillis(value: unknown): number | null {
    const wrapped = asNumber(asRecord(value)?.ts)
    if (wrapped !== null) return wrapped

    // Legacy: epoch seconds as a double.
    const seconds = asNumber(value)
    return seconds === null ? null : seconds * 1000
}

export interface KontainerService {
    cls: string | null
    type: string | null
    definition: KontainerDefinition | null
    instances: KontainerInstance[]
}

export interface KontainerSlice {
    numOld: number | null
    numTotal: number | null
    services: KontainerService[]
}

/** Class references are wrapped: `{"fqn": "..."}`. */
function readFqn(value: unknown): string | null {
    return asString(asRecord(value)?.fqn) ?? asString(value)
}

function readDefinition(value: unknown, depth = 0): KontainerDefinition | null {
    const record = asRecord(value)
    if (record === null) return null

    // `overwrites` is a chain. Bounded because a cycle in a malformed record would hang the tab, and
    // hanging is worse than truncating: nothing here is important enough to lock the browser for.
    const overwrites = depth < 16 ? readDefinition(record.overwrites, depth + 1) : null

    return {
        creates: readFqn(record.creates),
        injectionType: asString(record.injectionType),
        injects: (asArray(record.injects) ?? []).map((raw) => {
            const inject = asRecord(raw) ?? {}
            return {
                name: asString(inject.name),
                classes: (asArray(inject.classes) ?? []).map(readFqn).filter((fqn): fqn is string => fqn !== null),
                provisionType: asString(inject.provisionType),
            }
        }),
        codeLocation: asString(asRecord(record.codeLocation)?.location),
        overwrites,
    }
}

export function readKontainer(data: unknown): KontainerSlice | null {
    const record = asRecord(data)
    if (record === null) return null

    const rawServices = asArray(asRecord(record.info)?.services)
    if (rawServices === null) return null

    const services: KontainerService[] = rawServices.map((raw) => {
        const service = asRecord(raw) ?? {}
        return {
            cls: readFqn(service.cls),
            type: asString(service.type),
            definition: readDefinition(service.definition),
            instances: (asArray(service.instances) ?? []).map((rawInstance) => {
                const instance = asRecord(rawInstance) ?? {}
                return { cls: readFqn(instance.cls), createdAtMillis: readInstantMillis(instance.createdAt) }
            }),
        }
    })

    // The old table's order: services WITH instances first, then by FQN. Instantiated services are what
    // the request actually touched, which is the question the tab is usually open to answer.
    services.sort((a, b) => {
        const byInstances = Number(b.instances.length > 0) - Number(a.instances.length > 0)
        return byInstances !== 0 ? byInstances : (a.cls ?? '').localeCompare(b.cls ?? '')
    })

    return { numOld: asNumber(record.numOld), numTotal: asNumber(record.numTotal), services }
}

/** Walks the `overwrites` chain into a flat list; the first entry is the definition in force. */
export function definitionChain(definition: KontainerDefinition | null): KontainerDefinition[] {
    const chain: KontainerDefinition[] = []
    let current = definition
    while (current !== null) {
        chain.push(current)
        current = current.overwrites
    }
    return chain
}

/*  app-config  ---------------------------------------------------------------------------------------  */

export interface AppConfigSlice {
    info: unknown
    config: unknown
}

export function readAppConfig(data: unknown): AppConfigSlice | null {
    const record = asRecord(data)
    if (record === null) return null
    if (record.info === undefined && record.config === undefined) return null

    return { info: record.info, config: record.config }
}

/*  Stat strips shared with the Overview section  -----------------------------------------------------  */

/**
 * The seven runtime cells.
 *
 * Lives here rather than in `RuntimeTab.vue` because the old GUI showed this same strip inside Overview
 * as well, and `<script setup>` cannot export. Two copies would drift the first time a threshold moved.
 */
export function runtimeCells(slice: RuntimeSlice): StatCell[] {
    return [
        { label: 'JVM', value: slice.jvmVersion },
        { label: 'Kotlin', value: slice.kotlinVersion },
        { label: 'CPUs', value: slice.cpus },
        { label: 'Free heap', value: bytesToMb(slice.freeMem) },
        { label: 'Reserved heap', value: bytesToMb(slice.reservedMem) },
        { label: 'Max heap', value: bytesToMb(slice.maxMem) },
        {
            label: 'File descriptors',
            value: slice.openFileDescriptors,
            // Both are 0 on non-Unix, which is "not measured" rather than "none open" -- so the hint
            // says max rather than implying a ratio that would read as 0/0.
            hint: `max: ${slice.maxFileDescriptors ?? 'n/a'}`,
        },
    ]
}

/** The seven database cells. Also shown in Overview -- same reason as {@link runtimeCells}. */
export function vaultCells(slice: VaultSlice): StatCell[] {
    return [
        { label: 'Queries', value: slice.entries.length },
        { label: 'Total', value: nsToMs(slice.totalNs) },
        { label: 'Serializer', value: nsToMs(slice.serializer.totalNs) },
        { label: 'Query', value: nsToMs(slice.query.totalNs) },
        { label: 'Iterator', value: nsToMs(slice.iterator.totalNs) },
        { label: 'Deserializer', value: nsToMs(slice.deserializer.totalNs) },
        { label: 'Explain', value: nsToMs(slice.explain.totalNs) },
    ]
}
