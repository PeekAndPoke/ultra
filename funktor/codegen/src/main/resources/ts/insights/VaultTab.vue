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
 * ## Bind values are withheld, and the frontend no longer decides that
 *
 * `VaultCollector` records a placeholder-ised query or none at all, records only the COUNT of bind
 * variables, and does not record the EXPLAIN plan. So this tab renders whatever it is given.
 *
 * That is a deliberate move of the check. An earlier version kept a language allowlist here, deciding
 * per driver whether the query text was safe to show — a frontend patch over a backend contract
 * violation, and a check every future consumer would have had to repeat. The invariant now lives at the
 * one place that turns a driver's output into a record.
 *
 * Two things are still withheld at the source, and they are not this tab's to reinstate:
 *
 * - **`vars`** — blocked on `.claude/tasks/20260731-query-vars-in-insights.md`. A query is how a session
 *   token or an activation code gets looked up.
 * - **`queryExplained`** — cannot be made safe by placeholder-ising, because the DATABASE substitutes
 *   the values into the plan it returns. Measured against a real Arango server.
 *
 * **This tab still has no raw-slice dump and no JSON-tree fallback.** Less critical now that the record
 * itself is clean, but the reasoning stands: a viewer for the whole object defeats withholding part of
 * it, and the slice is the one place a future field could arrive unreviewed.
 *
 * ## One thing the old tab had that this one does not
 *
 * **No repository graph.** It came from `DatabaseGraphBuilder`, a live kontainer service queried at
 * render time, and is not in the stored data at all. Tracked in
 * `.claude/tasks/20260824-insights-graphs-and-static-slices.md`.
 *
 * Queries are shown through `PreBlock` rather than syntax-highlighted: the old tab used Prism, which is
 * a dependency this SDK does not have and cannot assume the consuming app has.
 *
 * See `RequestTab.vue` for the import-path note.
 */
import { computed } from 'vue'
// Deliberately no JsonTree import -- see the note above about `vars`.
import PreBlock from '../ui/PreBlock.vue'
import StatStrip from '../ui/StatStrip.vue'
import { nsToMs, readVault, vaultCells } from './slices.ts'
import type { VaultEntry, VaultMeasure } from './slices.ts'

defineOptions({ name: 'VaultTab' })

const props = defineProps<{ data: unknown }>()

const slice = computed(() => readVault(props.data))
const cells = computed(() => (slice.value === null ? [] : vaultCells(slice.value)))

/** `X ms (Nx)` -- the old per-query format, where the count matters as much as the time. */
function measure(value: VaultMeasure): string {
    return `${nsToMs(value.totalNs)} (${value.count}x)`
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

        <div v-if="slice.entries.length === 0" class="fk-empty">No database queries ran for this request.</div>

        <section v-for="(entry, index) in slice.entries" :key="index" class="fk-section fk-query">
            <h4 class="fk-section__title">
                Query #{{ index + 1 }} took {{ nsToMs(entry.totalNs) }}
                <span class="fk-muted">- {{ entry.connection ?? 'unknown connection' }}</span>
            </h4>

            <StatStrip
                :cells="[
                    { label: 'Results', value: results(entry) },
                    { label: 'Bind vars', value: entry.varsCount },
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
                :empty-text="`No query text recorded for ${entry.queryLanguage ?? 'this driver'} — it inlines bind values, so the server does not store the text.`"
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

            <!--
              `queryExplained` is NOT rendered, for either backend. Arango's explain endpoint SUBSTITUTES
              the bind values into the returned plan -- measured 2026-08-24 against a real server, which
              answered with `FILTER ((d.token == "<the actual token>") ...)`. Monko's QUERY_PLANNER output
              carries `parsedQuery` with the same literals. So this field reproduces exactly what `vars`
              is withheld to protect.
            -->
        </section>

    </div>

    <!--
      No raw fallback here, unlike every other tab. An unreadable vault slice still contains `vars`, so
      dumping it would leak precisely what this tab omits on purpose.
    -->
    <div v-else class="fk-notice">
        This database slice could not be read. The raw view is suppressed because the slice contains query
        bind values, which are not safe to display -- see the query-vars task.
    </div>
</template>
