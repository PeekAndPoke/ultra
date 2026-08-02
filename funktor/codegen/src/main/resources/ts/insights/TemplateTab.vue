<script setup lang="ts">
/**
 * The `template` slice: view rendering time.
 *
 * **A null time is the NORMAL case, not an error.** The slice is `{ timeNs: null }` on every request
 * that rendered no view, which is every API request -- so a tab that reported a problem here would flag
 * almost every record. It says "no view rendered" and stops.
 *
 * Thresholds (red above 30ms, yellow above 10) are inherited from the old insights bar. They are the
 * only real content this collector has.
 *
 * See `RequestTab.vue` for the import-path note.
 */
import { computed } from 'vue'
import FactList from '../ui/FactList.vue'
import { toneAbove } from '../ui/types.ts'
import type { Fact } from '../ui/types.ts'
import { nsToMs, readTemplateTimeNs } from './slices.ts'

defineOptions({ name: 'TemplateTab' })

const props = defineProps<{ data: unknown }>()

const timeNs = computed(() => readTemplateTimeNs(props.data))

const facts = computed<Fact[]>(() => {
    if (timeNs.value === null) return []

    return [
        {
            key: 'Render time',
            value: nsToMs(timeNs.value),
            tone: toneAbove(timeNs.value / 1_000_000, { warn: 10, error: 30 }),
        },
    ]
})
</script>

<template>
    <section class="fk-section">
        <FactList v-if="timeNs !== null" :facts="facts" />
        <div v-else class="fk-empty">No view was rendered for this request.</div>
    </section>
</template>
