<script setup lang="ts">
/**
 * The `vault` slice: the database queries this request ran.
 *
 * ## The header totals are summed here; the per-query ones are not
 *
 * Each entry now carries its own `totalNs` and a `{totalNs, count}` per sub-measure, because
 * `VaultCollector.Data` puts them in the DTO. They used to be computed getters on `StopWatch.Impl` that
 * no codec emitted — the slice would have read `0.00 ms (0x)` everywhere even after it became writable
 * at all. `slices.ts` still sums ACROSS entries for the header strip; that part is genuinely frontend
 * arithmetic, and if a header figure disagrees with the per-query rows, it is where to look.
 *
 * ## Bind values are shown, except where the server withheld them
 *
 * Maintainer, 2026-11-09 (ULTRA-00171): "query values: show them again". As in the old GUI, each query shows
 * its text, its bind values and, when the app explains queries, the plan in a collapsed "Explained".
 *
 * The frontend decides none of it. `VaultCollector` records what may be shown: everything for an ordinary
 * repository; for one that holds credentials (auth records, sent messages: `withholdingQueryValues()`) only
 * placeholder-ised text and the COUNT of values, marked `valuesWithheld`, which this tab says. An earlier
 * version kept a language allowlist here: a frontend patch over a backend contract, which every consumer
 * would have had to repeat.
 *
 * A record from before `VaultCollector.Data` (`legacy`) still has its text hidden: written whole by Jackson,
 * from every repository, the withholding ones too.
 *
 * ## The repository graph is not in the record
 *
 * It describes the schema, not the request, so it comes from its own endpoint
 * (`GET /_/funktor/insights/graphs/database`), fetched on the first click of "Show Database Graph" or
 * of "Database Graph Data", never before, and once per page. Without it -- no API client, a 401, a missing endpoint --
 * the tab renders as before, with a notice. Its raw view is safe to show: it holds repository names,
 * stored classes and references, no query and no value.
 *
 * See `RequestTab.vue` for the import-path note.
 */
import { computed, ref } from 'vue'
import type { Ref } from 'vue'
// JsonTree renders the DATABASE GRAPH only, never the slice.
import JsonTree from '../ui/JsonTree.vue'
import PreBlock from '../ui/PreBlock.vue'
import StatStrip from '../ui/StatStrip.vue'
import { databaseNetwork, useInsightsGraphsOrNull } from './graphs.ts'
import type { DatabaseGraphResult } from './graphs.ts'
import NetworkGraph from './NetworkGraph.vue'
import { nsToMs, readVault, valuesText, vaultCells } from './slices.ts'
import type { VaultEntry, VaultMeasure } from './slices.ts'

defineOptions({ name: 'VaultTab' })

const props = defineProps<{ data: unknown }>()

const slice = computed(() => readVault(props.data))
const cells = computed(() => (slice.value === null ? [] : vaultCells(slice.value)))

/** Null outside a detail page, e.g. a tab mounted on its own: the graph is then not offered. */
const graphs = useInsightsGraphsOrNull()

const database: Ref<DatabaseGraphResult | null> = ref(null)
/** When [database] was fetched: once per page, so possibly for an earlier record. */
const fetchedAt: Ref<string | null> = ref(null)
const graphProblem: Ref<string | null> = ref(null)
const loading = ref(false)
const showGraph = ref(false)

const network = computed(() => (showGraph.value && database.value !== null ? databaseNetwork(database.value.graph) : null))

/** Fetches the database graph once; both the button and the raw view call this. */
async function loadGraph(): Promise<void> {
    if (database.value !== null || loading.value || graphs === null) return

    loading.value = true
    graphProblem.value = null

    const result = await graphs.database()

    loading.value = false
    if (result.ok) {
        database.value = result.value
        fetchedAt.value = new Date(result.fetchedAt).toLocaleString()
    } else {
        graphProblem.value = `The database graph is not available: ${result.reason}`
    }
}

function toggleGraph(): void {
    showGraph.value = !showGraph.value
    if (showGraph.value) void loadGraph()
}

function onDataToggle(event: Event): void {
    if ((event.target as HTMLDetailsElement).open) void loadGraph()
}

/** `X ms (Nx)` -- the old per-query format, where the count matters as much as the time. */
function measure(value: VaultMeasure): string {
    return `${nsToMs(value.totalNs)} (${value.count}x)`
}

/** Why an entry has no query text; Monko's old `json` entries and raw AQL have their own reasons. */
function emptyQueryText(entry: VaultEntry): string {
    if (entry.queryLanguage === 'json') {
        return 'No query text recorded: written before 2026-10-19, when Monko still put the bind values into its ' +
            'query text, so the server withheld it and did not count the bind variables.'
    }
    if (entry.valuesWithheld) {
        return `No query text recorded (${entry.queryLanguage ?? 'unknown language'}): this repository withholds ` +
            'its values, and the text may hold them as literals.'
    }
    return `No query text recorded (${entry.queryLanguage ?? 'unknown language'}). Either the query failed ` +
        'before it was printed, or the record is from before 2026-11-10, when the server withheld text that ' +
        'could hold values.'
}

function results(entry: VaultEntry): string {
    const count = entry.count ?? 0
    return entry.totalCount === null ? `${count} of n/a` : `${count} of ${entry.totalCount}`
}

</script>

<template>
    <div v-if="slice !== null">
        <section class="fk-section">
            <StatStrip :cells="cells" />
        </section>

        <section class="fk-section">
            <div class="fk-toolbar">
                <button
                    type="button"
                    class="fk-button fk-graph-button--error"
                    :disabled="graphs === null"
                    :aria-pressed="showGraph"
                    @click="toggleGraph"
                >
                    Show Database Graph
                </button>
            </div>
            <p class="fk-section__note">
                The database graph is the server's schema{{ fetchedAt === null ? '' : ` as fetched at ${fetchedAt}` }},
                not part of this record. It is fetched once per page.
            </p>
            <div v-if="graphs === null" class="fk-notice">No API client here to fetch the database graph.</div>
            <div v-if="loading" class="fk-empty">Loading the database graph...</div>
            <div v-if="graphProblem !== null" class="fk-notice fk-notice--error">{{ graphProblem }}</div>
            <NetworkGraph v-if="network !== null" :data="network" />
        </section>

        <div v-if="slice.entries.length === 0" class="fk-empty">No database queries ran for this request.</div>

        <section v-for="(entry, index) in slice.entries" :key="index" class="fk-section fk-query">
            <h4 class="fk-section__title">
                Query #{{ index + 1 }} took {{ nsToMs(entry.totalNs) }}
                <span class="fk-muted">- {{ entry.connection ?? 'unknown connection' }}</span>
            </h4>

            <StatStrip
                :cells="[
                    { label: 'Results', value: results(entry) },
                    { label: 'Bind vars', value: entry.varsCount ?? 'unknown' },
                    { label: 'Serializer', value: measure(entry.serializer) },
                    { label: 'Query', value: measure(entry.query_) },
                    { label: 'Iterator', value: measure(entry.iterator) },
                    { label: 'Deserializer', value: measure(entry.deserializer) },
                    { label: 'Explain', value: measure(entry.explain) },
                ]"
            />

            <PreBlock
                v-if="!entry.legacy"
                :text="entry.query"
                :empty-text="emptyQueryText(entry)"
                wrap
            />
            <!--
              A record written before `VaultCollector.Data` existed carries the raw profiler entry, whose
              query text can hold the bind values inlined. That record predates the guarantee, so its
              text is not shown -- the timings above are still accurate and still useful.
            -->
            <div v-else class="fk-notice">
                Query text is hidden: this record predates the collector that guarantees bind values are
                kept out of it.
            </div>

            <div v-if="entry.valuesWithheld" class="fk-notice">
                Values withheld: this repository holds credentials, so the server recorded only that the query
                had {{ entry.varsCount ?? 'some' }} bind {{ entry.varsCount === 1 ? 'variable' : 'variables' }}.
            </div>
            <PreBlock
                v-else-if="entry.values !== null && Object.keys(entry.values).length > 0"
                :text="valuesText(entry.values)"
                wrap
            />

            <details v-if="entry.plan !== null" class="fk-details">
                <summary>Explained</summary>
                <div class="fk-details__body">
                    <PreBlock :text="entry.plan" wrap />
                </div>
            </details>
        </section>

        <details v-if="graphs !== null" class="fk-details fk-graph-data" @toggle="onDataToggle">
            <summary>Database Graph Data</summary>
            <div class="fk-details__body">
                <JsonTree v-if="database !== null" :value="database.raw" :expand-depth="2" />
                <div v-else-if="graphProblem !== null" class="fk-notice fk-notice--error">{{ graphProblem }}</div>
                <div v-else class="fk-empty">Loading the database graph...</div>
            </div>
        </details>
    </div>

    <!--
      No raw fallback here, unlike every other tab: an unreadable slice may be a pre-collector one, written
      whole by Jackson with the values of every repository, the withholding ones too.
    -->
    <div v-else class="fk-notice">
        This database slice could not be read. The raw view is suppressed: an unreadable slice may be an old
        one that holds the values of repositories that withhold them now.
    </div>
</template>
