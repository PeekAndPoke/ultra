<script setup lang="ts">
/**
 * The `kontainer` slice: which services this request's kontainer instantiated, and the service graph.
 *
 * ## Two shapes, and the tab says which one it shows
 *
 * - `full` (written before 2026-10-19) -- every service with its definition. The graphs are drawn from
 *   the record itself, which is what the request actually ran with.
 * - `slim` (since) -- only services with instances, plus the `graphFingerprint` they were written
 *   against. Injects and definitions come from the kontainer graph endpoint, joined on `cls`, fetched on
 *   the first graph click and never before, at most once per page. A fingerprint mismatch is said, not
 *   hidden: the edges are then today's, which is expected after almost any deploy.
 *
 * Services are ordered with instantiated ones first, then by FQN (`readKontainer` does the sort).
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
import { definitionChain, graphMatch, isProcessWide, joinKontainerGraph, readKontainer } from './slices.ts'
import type { KontainerGraphData, KontainerInstance, KontainerService } from './slices.ts'

defineOptions({ name: 'KontainerTab' })

const props = defineProps<{ data: unknown }>()

/** Null outside a detail page, e.g. a tab mounted on its own; a `slim` record then has no graph. */
const graphs = useInsightsGraphsOrNull()

const slice = computed(() => readKontainer(props.data))

/** The kontainer graph endpoint's answer. Only a `slim` record asks for it, and only on a graph click. */
const graph: Ref<KontainerGraphData | null> = ref(null)
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
        const pool = which === 'instances' ? value.services.filter((service) => service.instances.length > 0) : value.services
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

    const result = await graphs.kontainer()

    loading.value = false
    if (result.ok) graph.value = result.value
    else graphProblem.value = `The kontainer graph is not available: ${result.reason}`
}

const summary = computed<Fact[]>(() => {
    const value = slice.value
    if (value === null) return []

    const instantiated = value.services.filter((service) => service.instances.length > 0)
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
function createdAt(instance: KontainerInstance): string {
    return instance.createdAtMillis === null ? 'n/a' : new Date(instance.createdAtMillis).toLocaleString()
}

/** A full record's counts exactly as recorded; `?` for a missing one. */
function counts(value: { numOld: number | null; numTotal: number | null }): string {
    const young = value.numTotal === null || value.numOld === null ? '?' : `${value.numTotal - value.numOld}`
    return `${young} / ${value.numOld ?? '?'} / ${value.numTotal ?? '?'}`
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
                first needed it. Every other type belongs to this request's kontainer. The creation time only
                says whether this request was the one that created the instance.
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
                {{ slice.graphFingerprint }}, today {{ graph?.fingerprint ?? 'none' }}). The definitions and
                edges shown are today's. Expected after almost any deploy: the fingerprint covers line numbers.
            </div>
            <div v-if="match === 'unknown'" class="fk-notice">
                This record names no kontainer graph -- the server could not build one when it was written -- so
                whether today's graph matches it cannot be verified. The definitions and edges shown are today's.
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
                        <th>Service</th>
                        <th>Type</th>
                        <th>Instances</th>
                        <th>Injects</th>
                        <th>Definition &amp; overwrites</th>
                    </tr>
                </thead>
                <tbody>
                    <tr v-for="service in services" :key="service.cls ?? ''">
                        <td class="fk-mono" :title="service.cls ?? ''">{{ shortName(service.cls) }}</td>
                        <td>
                            <span class="fk-label">{{ service.type ?? 'n/a' }}</span>
                            <div v-if="service.instances.length > 0" class="fk-muted">
                                {{ isProcessWide(service) ? 'process-wide' : "this request's" }}
                            </div>
                        </td>

                        <td class="fk-mono">
                            <div v-if="service.instances.length === 0" class="fk-muted">-</div>
                            <div v-for="(instance, index) in service.instances" :key="index">
                                {{ shortName(instance.cls) }}
                                <span class="fk-muted">{{ createdAt(instance) }}</span>
                            </div>
                        </td>

                        <td class="fk-mono">
                            <div v-if="!service.definition?.injects.length" class="fk-muted">-</div>
                            <div v-for="inject in service.definition?.injects ?? []" :key="inject.name ?? ''">
                                {{ inject.name ?? '?' }}:
                                <span class="fk-muted">
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
                                {{ definition.injectionType ?? '?' }} - {{ shortName(definition.creates) }}
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
