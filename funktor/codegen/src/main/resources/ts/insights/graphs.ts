/**
 * The three insights graphs: their data, the nodes and edges drawn from it, and where a tab gets it.
 *
 * Ported from the old vis.js renderers (`funktor/insights/reference/collectors/KontainerCollector.kt` and
 * `VaultCollector.kt` before `e448c64d`): same colours, sizes, arrows and dashes.
 *
 * **Nothing here imports the graph library.** This file is re-exported by the SDK barrel, so a package
 * import here would be a dependency of every app that imports the barrel. `NetworkGraph.vue` loads
 * `vis-network` itself, on first use.
 *
 * Labels are drawn on a canvas, never as markup, and no node or edge carries an HTML `title`.
 */
import { inject, provide } from 'vue'
import type { InjectionKey } from 'vue'
import type { FunktorInsightsClient } from '../api/funktorInsightsClient.ts'
import type { ApiResponse } from '../runtime/apiResponse.ts'
import { isSuccess } from '../runtime/apiResponse.ts'
import { asArray, asRecord, asString } from './slices.ts'
import type { KontainerGraphData, KontainerService } from './slices.ts'
import { readKontainerGraph } from './slices.ts'

/*  What a network is drawn from  ---------------------------------------------------------------------  */

export interface NetworkNode {
    id: number
    label: string
    /** Node size, scaled by the library between its min and max. */
    value: number
    color?: string
}

export interface NetworkEdge {
    from: number
    to: number
    arrows: 'to'
    dashes: boolean
    value: number
    label?: string
}

/** Which graph a network is: each had its own physics in the old GUI. */
export type NetworkKind = 'instances' | 'full' | 'database'

export interface NetworkData {
    kind: NetworkKind
    nodes: NetworkNode[]
    edges: NetworkEdge[]
    /** Edges NOT drawn because one end is no node -- e.g. an injection whose declared type is no service key. */
    undrawn: number
}

/** The old GUI's colours, by provider type. */
const TYPE_COLOURS: Record<string, string> = {
    Singleton: '#1E90FF',
    Prototype: '#FFD700',
    Dynamic: '#8B0000',
    SemiDynamic: '#9400D3',
    DynamicOverride: '#DC143C',
}

function simpleName(fqn: string | null): string {
    return fqn === null ? '?' : (fqn.split('.').pop() ?? fqn)
}

/**
 * One node per service, one edge per injected class: arrow at the injected service, dashed and lighter
 * when the injection is `Lazy`.
 *
 * An injection names the DECLARED parameter type, which need not be a service key. The old graph drew
 * such an edge to an id with no node, which vis.js silently drops; here it is skipped and counted.
 */
export function kontainerNetwork(services: KontainerService[], kind: 'instances' | 'full'): NetworkData {
    const ids = new Map<string, number>()
    for (const service of services) {
        if (service.cls !== null && !ids.has(service.cls)) ids.set(service.cls, ids.size + 1)
    }

    const nodes: NetworkNode[] = []
    const edges: NetworkEdge[] = []
    let undrawn = 0

    for (const service of services) {
        const id = service.cls === null ? undefined : ids.get(service.cls)
        if (id === undefined) continue

        const injects = service.definition?.injects ?? []
        const type = service.type ?? '?'

        nodes.push({
            id,
            label: `${simpleName(service.cls)}\n${type}`,
            value: injects.reduce((sum, inject) => sum + inject.classes.length, 0),
            color: Object.hasOwn(TYPE_COLOURS, type) ? TYPE_COLOURS[type] : undefined,
        })

        for (const inject of injects) {
            const lazy = inject.provisionType === 'Lazy'
            for (const cls of inject.classes) {
                const to = ids.get(cls)
                if (to === undefined) {
                    undrawn++
                    continue
                }
                edges.push({ from: id, to, arrows: 'to', dashes: lazy, value: lazy ? 1 : 2 })
            }
        }
    }

    return { kind, nodes, edges, undrawn }
}

/**
 * The old GUI's vis.js options for each graph, verbatim apart from the two solvers it never used.
 *
 * `improvedLayout: false` is load-bearing: with it on, vis.js lays out a few hundred nodes for seconds.
 */
export function networkOptions(kind: NetworkKind): Record<string, unknown> {
    const solver = kind === 'full' ? 'barnesHut' : 'forceAtlas2Based'

    return {
        layout: { improvedLayout: false },
        physics: {
            solver,
            barnesHut: {
                gravitationalConstant: -2000,
                centralGravity: 0.3,
                springLength: 95,
                springConstant: 0.04,
                damping: 0.09,
                avoidOverlap: kind === 'instances' ? 0.1 : 0,
            },
            forceAtlas2Based: {
                gravitationalConstant: -50,
                centralGravity: 0.01,
                springConstant: 0.08,
                springLength: 100,
                damping: 0.4,
                avoidOverlap: kind === 'full' ? 0 : 0.25,
            },
            maxVelocity: 50,
            minVelocity: 0.75,
        },
        nodes: { shape: 'dot', scaling: { min: 10, max: 50 } },
        edges: { scaling: { min: 1, max: 2 } },
    }
}

/*  The database graph  ---------------------------------------------------------------------------------  */

export interface DatabaseRepoId {
    name: string | null
    connection: string | null
}

export interface DatabaseReference {
    /** Null when no repository stores the referenced type. */
    repo: DatabaseRepoId | null
    fqn: string | null
}

export interface DatabaseRepo {
    id: DatabaseRepoId
    storedClasses: { fqn: string | null; references: DatabaseReference[] }[]
}

export interface DatabaseGraphData {
    repos: DatabaseRepo[]
}

function readRepoId(value: unknown): DatabaseRepoId | null {
    const record = asRecord(value)
    return record === null ? null : { name: asString(record.name), connection: asString(record.connection) }
}

export function readDatabaseGraph(data: unknown): DatabaseGraphData | null {
    const repos = asArray(asRecord(data)?.repos)
    if (repos === null) return null

    return {
        repos: repos.map((raw) => {
            const repo = asRecord(raw) ?? {}
            return {
                id: readRepoId(repo.id) ?? { name: null, connection: null },
                storedClasses: (asArray(repo.storedClasses) ?? []).map((rawCls) => {
                    const cls = asRecord(rawCls) ?? {}
                    return {
                        fqn: asString(cls.fqn),
                        references: (asArray(cls.references) ?? []).map((rawRef) => {
                            const ref = asRecord(rawRef) ?? {}
                            return { repo: readRepoId(ref.repo), fqn: asString(ref.fqn) }
                        }),
                    }
                }),
            }
        }),
    }
}

function repoKey(id: DatabaseRepoId): string {
    return `${id.name ?? '?'}::${id.connection ?? '?'}`
}

/**
 * One node per repository, sized by its references; one edge per referenced repository, from it to the
 * referencing one, labelled with the count -- the old GUI's direction.
 *
 * The old GUI dashed `Lazy` references. The server only ever produced `Direct`, and the field is gone
 * from the endpoint, so every edge is solid.
 */
export function databaseNetwork(graph: DatabaseGraphData): NetworkData {
    const ids = new Map<string, number>()
    for (const repo of graph.repos) {
        const key = repoKey(repo.id)
        if (!ids.has(key)) ids.set(key, ids.size + 1)
    }

    const nodes: NetworkNode[] = []
    const edges: NetworkEdge[] = []
    let undrawn = 0

    for (const repo of graph.repos) {
        const id = ids.get(repoKey(repo.id))!
        const references = repo.storedClasses.flatMap((cls) => cls.references)

        nodes.push({
            id,
            label: `${repo.id.name ?? '?'}\n${repo.id.connection ?? '?'}`,
            value: references.length,
        })

        const groups = new Map<string, { from: number | undefined; count: number }>()
        for (const ref of references) {
            if (ref.repo === null) continue
            const groupKey = repoKey(ref.repo)
            const group = groups.get(groupKey) ?? { from: ids.get(groupKey), count: 0 }
            group.count++
            groups.set(groupKey, group)
        }

        for (const group of groups.values()) {
            if (group.from === undefined) {
                undrawn++
                continue
            }
            edges.push({
                from: group.from,
                to: id,
                arrows: 'to',
                dashes: false,
                value: group.count,
                label: `${group.count}`,
            })
        }
    }

    return { kind: 'database', nodes, edges, undrawn }
}

/*  Where a tab gets the graphs  -------------------------------------------------------------------------  */

/**
 * A graph and when it was fetched (epoch millis), or why there is none. A failure is a value: the tab keeps
 * rendering without the graph.
 */
export type GraphResult<T> = { ok: true; value: T; fetchedAt: number } | { ok: false; reason: string }

/** What a loader answers; [attempt] stamps the fetch time on success. */
type Loaded<T> = { ok: true; value: T } | { ok: false; reason: string }

export interface DatabaseGraphResult {
    graph: DatabaseGraphData
    /** The endpoint's `data` as received, for the "Database Graph Data" view. */
    raw: unknown
}

/**
 * Fetches the two graph endpoints for the tabs, LAZILY: nothing is requested until a tab asks.
 *
 * `InsightsPage` holds one source for the page's lifetime. A FAILED fetch is not kept, so the next click
 * asks again. Neither method ever throws.
 */
export interface InsightsGraphSource {
    /**
     * The kontainer graph for a record written against [fingerprint] (null: the record names none).
     *
     * Cached by the fingerprint the server answered. A fingerprint not in the cache is fetched ONCE: after a
     * redeploy the server serves another graph, and a page open across it would otherwise keep the old one.
     * If the server still answers another fingerprint, that answer is kept for [fingerprint]. A record naming
     * none takes the latest answer.
     */
    kontainer(fingerprint: string | null): Promise<GraphResult<KontainerGraphData>>
    /** The database graph, fetched once per source: it has no fingerprint, nothing joins it. */
    database(): Promise<GraphResult<DatabaseGraphResult>>
}

function refusal(response: ApiResponse<unknown>): string {
    const messages = response.messages?.map((message) => message.text).join('; ')
    return `${response.status.value} ${response.status.description}${messages ? ` -- ${messages}` : ''}`
}

async function attempt<T>(load: () => Promise<Loaded<T>>): Promise<GraphResult<T>> {
    try {
        const result = await load()
        return result.ok ? { ...result, fetchedAt: Date.now() } : result
    } catch (error) {
        // A missing endpoint on an older server answers with something that is not an envelope, and
        // the client throws. That is a reason to show, not a reason to take the tab down.
        return { ok: false, reason: error instanceof Error ? error.message : String(error) }
    }
}

/** Keeps a successful result; forgets a failed one, so the next call asks again. */
function once<T>(load: () => Promise<Loaded<T>>): () => Promise<GraphResult<T>> {
    let pending: Promise<GraphResult<T>> | null = null

    return async () => {
        pending ??= attempt(load)
        const result = await pending
        if (!result.ok) pending = null
        return result
    }
}

/** The cache behind [InsightsGraphSource.kontainer]. */
function byFingerprint(load: () => Promise<Loaded<KontainerGraphData>>): InsightsGraphSource['kontainer'] {
    /** Successful answers, by the fingerprint the SERVER sent. */
    const answers = new Map<string, GraphResult<KontainerGraphData>>()
    /** The fetch made for each fingerprint a record named; null for a record naming none. */
    const asked = new Map<string | null, Promise<GraphResult<KontainerGraphData>>>()
    let latest: GraphResult<KontainerGraphData> | null = null

    function ask(wanted: string | null): Promise<GraphResult<KontainerGraphData>> {
        const pending = attempt(load).then((result) => {
            if (result.ok) {
                if (result.value.fingerprint !== null) answers.set(result.value.fingerprint, result)
                latest = result
            } else {
                asked.delete(wanted)
            }
            return result
        })
        asked.set(wanted, pending)
        return pending
    }

    return async (wanted) => {
        const known = wanted === null ? latest : answers.get(wanted)
        return known ?? (await (asked.get(wanted) ?? ask(wanted)))
    }
}

export function createGraphSource(client: () => FunktorInsightsClient): InsightsGraphSource {
    return {
        kontainer: byFingerprint(async () => {
            const response = await client().insights.getKontainerGraph()
            if (!isSuccess(response)) return { ok: false, reason: refusal(response) }

            const graph = readKontainerGraph(response.data)
            return graph === null ? { ok: false, reason: 'The server sent no kontainer graph.' } : { ok: true, value: graph }
        }),

        database: once<DatabaseGraphResult>(async () => {
            const response = await client().insights.getDatabaseGraph()
            if (!isSuccess(response)) return { ok: false, reason: refusal(response) }

            const graph = readDatabaseGraph(response.data)
            return graph === null
                ? { ok: false, reason: 'The server sent no database graph.' }
                : { ok: true, value: { graph, raw: response.data } }
        }),
    }
}

const INSIGHTS_GRAPHS: InjectionKey<InsightsGraphSource> = Symbol('funktor.insights.graphs')

/** Makes [source] available to the tabs below the calling component. Call during `setup()`. */
export function provideInsightsGraphs(source: InsightsGraphSource): void {
    provide(INSIGHTS_GRAPHS, source)
}

/** The graph source, or null when none was provided -- the tab then renders without its graphs. */
export function useInsightsGraphsOrNull(): InsightsGraphSource | null {
    return inject(INSIGHTS_GRAPHS, null)
}
