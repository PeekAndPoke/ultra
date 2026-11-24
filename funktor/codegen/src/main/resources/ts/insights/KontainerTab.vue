<script setup lang="ts">
/**
 * The `kontainer` slice: which services this request's kontainer instantiated, and the service graph.
 *
 * ## Two shapes, and the tab says which one it shows
 *
 * - `full` (written before 2026-10-19) -- every service with its definition. The graphs are drawn from
 *   the record itself, which is what the request actually ran with.
 * - `slim` (since) -- only services with creations, each with a count and the first and last creation
 *   time, plus the `graphFingerprint` they were written against. Injects and definitions come from the kontainer graph endpoint, joined on `cls`, fetched on
 *   the first graph click and never before, and cached by fingerprint (`InsightsGraphSource.kontainer`). A
 *   fingerprint mismatch is said, not hidden: the edges are then the server's, fetched for a record of this
 *   fingerprint, which is expected after almost any deploy.
 *
 * Services are ordered with instantiated ones first, then by FQN (`readKontainer` does the sort). A click on a
 * column header sorts by it, again reverses, as the old GUI's sortable table did (ULTRA-00190). The Service
 * column shows the full class name, so the browser's find reaches it; the other columns keep the simple name,
 * with the full one in its tooltip.
 *
 * **Without the graph the tab still renders**: no API client, a 401, a missing endpoint -- each leaves a
 * notice and the table, never an exception.
 *
 * See `RequestTab.vue` for the import-path note.
 */
import { computed, ref } from 'vue'
import type { Ref } from 'vue'
import FactList from '../ui/FactList.vue'
import JsonTree from '../ui/JsonTree.vue'
import type { Fact } from '../ui/types.ts'
import { kontainerNetwork, useInsightsGraphsOrNull } from './graphs.ts'
import type { NetworkData } from './graphs.ts'
import NetworkGraph from './NetworkGraph.vue'
import { definitionChain, graphMatch, hasCreations, isProcessWide, joinKontainerGraph, readKontainer } from './slices.ts'
import type { KontainerGraphData, KontainerService } from './slices.ts'

defineOptions({ name: 'KontainerTab' })

const props = defineProps<{ data: unknown }>()

/** Null outside a detail page, e.g. a tab mounted on its own; a `slim` record then has no graph. */
const graphs = useInsightsGraphsOrNull()

const slice = computed(() => readKontainer(props.data))

/** The kontainer graph endpoint's answer. Only a `slim` record asks for it, and only on a graph click. */
const graph: Ref<KontainerGraphData | null> = ref(null)
/** When [graph] was fetched -- possibly for an earlier record of the same fingerprint. */
const fetchedAt: Ref<string | null> = ref(null)
const graphProblem: Ref<string | null> = ref(null)
const loading = ref(false)
const shown: Ref<'instances' | 'full' | null> = ref(null)

const canDraw = computed(() => slice.value?.shape === 'full' || graphs !== null)

/** What the table lists: a `slim` record's services, joined with their definitions once the graph is in. */
const services = computed<KontainerService[]>(() => {
    const value = slice.value
    if (value === null) return []
    return value.shape === 'full' || graph.value === null ? value.services : joinKontainerGraph(value, graph.value, false)
})

const network = computed<NetworkData | null>(() => {
    const value = slice.value
    const which = shown.value
    if (value === null || which === null) return null

    if (value.shape === 'full') {
        const pool = which === 'instances' ? value.services.filter(hasCreations) : value.services
        return kontainerNetwork(pool, which)
    }

    return graph.value === null ? null : kontainerNetwork(joinKontainerGraph(value, graph.value, which === 'full'), which)
})

async function toggle(which: 'instances' | 'full'): Promise<void> {
    shown.value = shown.value === which ? null : which

    const value = slice.value
    if (shown.value === null || value === null || value.shape === 'full' || graph.value !== null || graphs === null) return

    loading.value = true
    graphProblem.value = null

    const result = await graphs.kontainer(value.graphFingerprint)

    loading.value = false
    if (result.ok) {
        graph.value = result.value
        fetchedAt.value = new Date(result.fetchedAt).toLocaleString()
    } else {
        graphProblem.value = `The kontainer graph is not available: ${result.reason}`
    }
}

const summary = computed<Fact[]>(() => {
    const value = slice.value
    if (value === null) return []

    const instantiated = value.services.filter(hasCreations)
    const shared = instantiated.filter(isProcessWide).length
    const own = `${instantiated.length} instantiated: ${instantiated.length - shared} this request's own, ${shared} process-wide singletons`

    return [
        {
            key: 'Record shape',
            value:
                value.shape === 'full'
                    ? 'full -- every definition inline (written before 2026-10-19)'
                    : 'slim -- instantiated services only; definitions come from the kontainer graph',
        },
        // The old bar's `young / old / total`, "old" meaning alive longer than 15 SECONDS. Only a `full` record
        // has them, as recorded -- and they were never measured: funktor never switches kontainer tracking on.
        ...(value.shape === 'full'
            ? [{ key: 'Kontainers (young / old / total)', value: `${counts(value)} -- not measured, tracking is never enabled` }]
            : []),
        { key: 'Services', value: value.shape === 'full' ? `${value.services.length} defined, ${own}` : own },
        ...(value.shape === 'slim'
            ? [{ key: 'Graph fingerprint', value: value.graphFingerprint ?? 'none -- the server could not build the graph' }]
            : []),
    ]
})

/** Null until the graph is in; then whether this record was written against it. */
const match = computed(() => (slice.value === null || graph.value === null ? null : graphMatch(slice.value, graph.value)))

/** Full date and time, not just the clock: it says whether THIS request created the instance. */
function at(millis: number | null): string {
    return millis === null ? 'n/a' : new Date(millis).toLocaleString()
}

/** A full record's counts exactly as recorded; `?` for a missing one. */
function counts(value: { numOld: number | null; numTotal: number | null }): string {
    const young = value.numTotal === null || value.numOld === null ? '?' : `${value.numTotal - value.numOld}`
    return `${young} / ${value.numOld ?? '?'} / ${value.numTotal ?? '?'}`
}

/*  Click-to-sort  ----------------------------------------------------------------------------------  */

type SortColumn = 'service' | 'type' | 'created' | 'injects' | 'definition'

const SORT_COLUMNS: Array<{ column: SortColumn, label: string }> = [
    { column: 'service', label: 'Service' },
    { column: 'type', label: 'Type' },
    { column: 'created', label: 'Created' },
    { column: 'injects', label: 'Injects' },
    { column: 'definition', label: 'Definition & overwrites' },
]

/** Null: the reader's order, instantiated first. */
const sortBy: Ref<{ column: SortColumn, descending: boolean } | null> = ref(null)

function sortKey(service: KontainerService, column: SortColumn): string | number {
    switch (column) {
        case 'service':
            return service.cls ?? ''
        case 'type':
            return service.type ?? ''
        case 'created':
            return service.count
        case 'injects':
            return service.definition?.injects.length ?? 0
        case 'definition':
            // What the cell shows, not the hidden FQN: sorting by package would look unsorted
            return service.definition === null ? '' : `${service.definition.injectionType ?? '?'} - ${shortName(service.definition.creates)}`
    }
}

/** Stable, so equal keys keep the reader's order: instantiated first, then by name. */
const sorted = computed<KontainerService[]>(() => {
    const by = sortBy.value
    if (by === null) return services.value

    const direction = by.descending ? -1 : 1

    return [...services.value].sort((a, b) => {
        const left = sortKey(a, by.column)
        const right = sortKey(b, by.column)
        const order = typeof left === 'number' && typeof right === 'number'
            ? left - right
            : String(left).localeCompare(String(right))

        return order * direction
    })
})

function sortOn(column: SortColumn): void {
    const by = sortBy.value
    sortBy.value = { column, descending: by?.column === column ? !by.descending : false }
}

function ariaSort(column: SortColumn): 'ascending' | 'descending' | 'none' {
    const by = sortBy.value
    return by?.column !== column ? 'none' : by.descending ? 'descending' : 'ascending'
}

function arrow(column: SortColumn): string {
    const sort = ariaSort(column)
    return sort === 'ascending' ? '▲' : sort === 'descending' ? '▼' : ''
}

function shortName(fqn: string | null): string {
    return fqn === null ? 'n/a' : (fqn.split('.').pop() ?? fqn)
}
</script>

<template>
    <div v-if="slice !== null">
        <section class="fk-section">
            <FactList :facts="summary" />
            <p class="fk-section__note">
                A Singleton is process state, shared by every request: created at boot or by whichever request
                first needed it. Every other type belongs to this request's kontainer. The creation times only
                say whether this request was the one that created the instances. Prototypes a Singleton creates
                are counted outside every request and show in no record.
            </p>
        </section>

        <section class="fk-section">
            <div class="fk-toolbar">
                <button
                    type="button"
                    class="fk-button fk-graph-button--ok"
                    :disabled="!canDraw"
                    :aria-pressed="shown === 'instances'"
                    @click="toggle('instances')"
                >
                    Show instances Graph
                </button>
                <button
                    type="button"
                    class="fk-button fk-graph-button--error"
                    :disabled="!canDraw"
                    :aria-pressed="shown === 'full'"
                    @click="toggle('full')"
                >
                    Show Full Graph
                </button>
            </div>

            <div v-if="!canDraw" class="fk-notice">
                No API client here to fetch the kontainer graph, which this record's shape needs.
            </div>
            <div v-if="loading" class="fk-empty">Loading the kontainer graph...</div>
            <div v-if="graphProblem !== null" class="fk-notice fk-notice--error">{{ graphProblem }}</div>
            <div v-if="match === 'other'" class="fk-notice">
                This record was written against another kontainer graph (fingerprint
                {{ slice.graphFingerprint }}). Asked for it at {{ fetchedAt }}, the server answered
                {{ graph?.fingerprint ?? 'none' }}: the definitions and edges shown are that graph's, not this
                record's. Expected after almost any deploy: the fingerprint covers line numbers.
            </div>
            <div v-if="match === 'unknown'" class="fk-notice">
                This record names no kontainer graph -- the server could not build one when it was written -- so
                whether the server's graph, fetched at {{ fetchedAt }}, matches it cannot be verified. The
                definitions and edges shown are that graph's.
            </div>

            <NetworkGraph v-if="network !== null" :key="shown ?? ''" :data="network" />
        </section>

        <section class="fk-section">
            <p v-if="slice.shape === 'slim' && graph === null" class="fk-section__note">
                Injects and definitions are not part of this record. Show a graph to load them from the
                server; the table fills in too.
            </p>
            <table class="fk-kv fk-services">
                <thead>
                    <tr>
                        <th v-for="header in SORT_COLUMNS" :key="header.column" :aria-sort="ariaSort(header.column)">
                            <button type="button" class="fk-sort" @click="sortOn(header.column)">
                                {{ header.label }}
                                <span class="fk-sort__arrow" aria-hidden="true">{{ arrow(header.column) }}</span>
                            </button>
                        </th>
                    </tr>
                </thead>
                <tbody>
                    <tr v-for="service in sorted" :key="service.cls ?? ''">
                        <td class="fk-mono fk-services__cls">{{ service.cls ?? 'n/a' }}</td>
                        <td>
                            <span class="fk-label">{{ service.type ?? 'n/a' }}</span>
                            <div v-if="hasCreations(service)" class="fk-muted">
                                {{ isProcessWide(service) ? 'process-wide' : "this request's" }}
                            </div>
                        </td>

                        <td class="fk-mono">
                            <div v-if="!hasCreations(service)" class="fk-muted">-</div>
                            <template v-else>
                                <div>{{ service.count }}</div>
                                <div v-if="service.count === 1" class="fk-muted">{{ at(service.firstCreatedAtMillis) }}</div>
                                <template v-else>
                                    <div class="fk-muted">first {{ at(service.firstCreatedAtMillis) }}</div>
                                    <div class="fk-muted">last {{ at(service.lastCreatedAtMillis) }}</div>
                                </template>
                            </template>
                        </td>

                        <td class="fk-mono">
                            <div v-if="!service.definition?.injects.length" class="fk-muted">-</div>
                            <div v-for="inject in service.definition?.injects ?? []" :key="inject.name ?? ''">
                                {{ inject.name ?? '?' }}:
                                <span class="fk-muted" :title="inject.classes.join(', ')">
                                    {{ inject.classes.map(shortName).join(', ') || 'n/a' }}
                                </span>
                                <!-- Lazy injection is why a singleton can hold a request-scoped service. -->
                                <span v-if="inject.provisionType === 'Lazy'" class="fk-label">Lazy</span>
                            </div>
                        </td>

                        <td class="fk-mono">
                            <div v-if="service.definition === null" class="fk-muted">-</div>
                            <div
                                v-for="(definition, index) in definitionChain(service.definition)"
                                :key="index"
                            >
                                <span v-if="index > 0" class="fk-muted">Overwrites -&gt;</span>
                                {{ definition.injectionType ?? '?' }} -
                                <span :title="definition.creates ?? ''">{{ shortName(definition.creates) }}</span>
                                <div v-if="definition.codeLocation" class="fk-muted">
                                    {{ definition.codeLocation }}
                                </div>
                            </div>
                        </td>
                    </tr>
                </tbody>
            </table>
        </section>

        <details class="fk-details">
            <summary>Raw slice{{ slice.shape === 'full' ? ' -- large, every definition inline' : '' }}</summary>
            <div class="fk-details__body"><JsonTree :value="data" /></div>
        </details>
    </div>

    <JsonTree v-else :value="data" :expand-depth="2" />
</template>
