<script setup lang="ts">
/**
 * The `template` slice: view rendering time.
 *
 * **A null time is the NORMAL case, not an error.** The slice is `{ timeNs: null }` on every request
 * that rendered no view, which is every API request -- so a tab that reported a problem here would flag
 * almost every record. It says "no view rendered" and stops.
 *
 * **But an unreadable slice is NOT the same thing**, and this tab used to conflate them: both produced
 * `null`, and both rendered "no view was rendered". A record whose field had been renamed therefore
 * displayed a 4.5 ms render as nothing at all, with the data shown nowhere. `readTemplate` now returns
 * null only for an unrecognised shape, and that falls back to `JsonTree` like every other tab.
 *
 * Thresholds (red above 30ms, yellow above 10) are inherited from the old insights bar. They are the
 * only real content this collector has.
 *
 * See `RequestTab.vue` for the import-path note.
 */
import { computed } from 'vue'
import FactList from '../ui/FactList.vue'
import JsonTree from '../ui/JsonTree.vue'
import { toneAbove } from '../ui/types.ts'
import type { Fact } from '../ui/types.ts'
import { nsToMs, readTemplate } from './slices.ts'

defineOptions({ name: 'TemplateTab' })

const props = defineProps<{ data: unknown }>()

const slice = computed(() => readTemplate(props.data))

const facts = computed<Fact[]>(() => {
    const timeNs = slice.value?.timeNs ?? null
    if (timeNs === null) return []

    return [
        {
            key: 'Render time',
            value: nsToMs(timeNs),
            tone: toneAbove(timeNs / 1_000_000, { warn: 10, error: 30 }),
        },
    ]
})
</script>

<template>
    <section v-if="slice !== null" class="fk-section">
        <FactList v-if="slice.timeNs !== null" :facts="facts" />
        <div v-else class="fk-empty">No view was rendered for this request.</div>
    </section>

    <JsonTree v-else :value="data" :expand-depth="2" />
</template>
