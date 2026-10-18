<script setup lang="ts">
/**
 * The `runtime` slice: the JVM the request ran on.
 *
 * Restores the old tab's seven-cell strip. Heap figures are whole megabytes -- the old insights BAR
 * showed the same three in GB to two decimals, and that difference was deliberate: a bar had one line,
 * a tab has a column. The tab's unit is the one that survives.
 *
 * `systemProperties` is a plain list and is deliberately NOT a `StatStrip` -- it is long, unordered and
 * only ever read by searching, so a table beats cells.
 *
 * See `RequestTab.vue` for the import-path note.
 */
import { computed } from 'vue'
import JsonTree from '../ui/JsonTree.vue'
import KeyValueTable from '../ui/KeyValueTable.vue'
import StatStrip from '../ui/StatStrip.vue'
import { readRuntime, runtimeCells } from './slices.ts'

defineOptions({ name: 'RuntimeTab' })

const props = defineProps<{ data: unknown }>()

const slice = computed(() => readRuntime(props.data))
const cells = computed(() => (slice.value === null ? [] : runtimeCells(slice.value)))
</script>

<template>
    <div v-if="slice !== null">
        <section class="fk-section">
            <StatStrip :cells="cells" />
            <p v-if="slice.openFileDescriptors === 0 && slice.maxFileDescriptors === 0" class="fk-section__note">
                File descriptor counts are 0 on non-Unix platforms -- that is "not measured", not "none open".
            </p>
        </section>

        <section class="fk-section">
            <h4 class="fk-section__title">System properties</h4>
            <KeyValueTable
                :entries="slice.systemProperties"
                key-label="Property"
                empty-text="No system properties recorded"
            />
        </section>

        <details class="fk-details">
            <summary>Raw slice</summary>
            <div class="fk-details__body"><JsonTree :value="data" /></div>
        </details>
    </div>

    <JsonTree v-else :value="data" :expand-depth="2" />
</template>
