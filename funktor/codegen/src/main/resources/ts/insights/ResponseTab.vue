<script setup lang="ts">
/**
 * The `response` slice: what went back.
 *
 * `Set-Cookie` lives in these headers, which is why the server redacts it before the record is written
 * -- an unredacted login response would contain a working session. `***redacted***` arriving here is a
 * value to render, and the table marks it as metadata so nobody reads it as the actual cookie.
 *
 * See `RequestTab.vue` for the import-path note.
 */
import { computed } from 'vue'
import FactList from '../ui/FactList.vue'
import JsonTree from '../ui/JsonTree.vue'
import KeyValueTable from '../ui/KeyValueTable.vue'
import { toneForStatus } from '../ui/types.ts'
import type { Fact } from '../ui/types.ts'
import { readResponse } from './slices.ts'

defineOptions({ name: 'ResponseTab' })

const props = defineProps<{ data: unknown }>()

const slice = computed(() => readResponse(props.data))

const facts = computed<Fact[]>(() => {
    const value = slice.value
    if (value === null) return []

    // The reason phrase is worth showing next to the code -- "418" alone sends the reader to a lookup.
    const status =
        value.status === null
            ? null
            : value.statusDescription === null
              ? `${value.status}`
              : `${value.status} ${value.statusDescription}`

    return [{ key: 'Status', value: status, tone: toneForStatus(value.status) }]
})
</script>

<template>
    <div v-if="slice !== null">
        <section class="fk-section">
            <FactList :facts="facts" />
        </section>

        <section class="fk-section">
            <h4 class="fk-section__title">Headers</h4>
            <KeyValueTable :entries="slice.headers" key-label="Header" empty-text="No headers recorded" />
        </section>

        <details class="fk-details">
            <summary>Raw slice</summary>
            <div class="fk-details__body"><JsonTree :value="data" /></div>
        </details>
    </div>

    <JsonTree v-else :value="data" :expand-depth="2" />
</template>
