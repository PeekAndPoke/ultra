<script setup lang="ts">
/**
 * The `vault` slice: the database queries this request ran.
 *
 * ## The totals are computed here, and that is not an optimisation
 *
 * Every total on `QueryProfiler` was a private `lazy` property, so **none of them is in the record.**
 * `slices.ts` sums `totalNs` across entries and once per sub-measure. If a figure here disagrees with
 * one the server logged, this is where to look first.
 *
 * ## Two things the old tab had that this one does not
 *
 * 1. **Bind values (`vars`) are NOT rendered.** They are in the record, and the old GUI showed them.
 *    They include the values of auth lookups -- the token, session id or activation code *being looked
 *    up* -- and there is no per-query way to opt out yet. Blocked on a maintainer decision:
 *    `.claude/tasks/20260731-query-vars-in-insights.md`. Do not add this back without it.
 *
 *    **This is also why this tab has no "Raw slice" dump and no JSON-tree fallback**, which every other
 *    tab has. Both would render the entire slice, `vars` included, and undo the omission in one click.
 *    Suppressing one field while shipping a viewer for the object containing it is not a control.
 * 2. **No repository graph.** It came from `DatabaseGraphBuilder`, a live kontainer service queried at
 *    render time, and is not in the stored data at all. It describes the schema rather than the request,
 *    so it likely belongs on its own endpoint.
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
                    { label: 'Serializer', value: measure(entry.serializer) },
                    { label: 'Query', value: measure(entry.query_) },
                    { label: 'Iterator', value: measure(entry.iterator) },
                    { label: 'Deserializer', value: measure(entry.deserializer) },
                    { label: 'Explain', value: measure(entry.explain) },
                ]"
            />

            <PreBlock :text="entry.query" empty-text="No query text recorded" wrap />

            <details v-if="entry.queryExplained" class="fk-details">
                <summary>Explained</summary>
                <div class="fk-details__body"><PreBlock :text="entry.queryExplained" wrap /></div>
            </details>
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
