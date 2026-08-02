<script setup lang="ts">
/**
 * The `request` slice: what arrived.
 *
 * Replaces the old tab's raw JSON dump with tables, which is what the spec asked for -- the dump stays
 * behind a `<details>` because it is the only view that shows a field this tab does not know about.
 *
 * Every name and value here came from an unauthenticated request. Rendering escapes; there is no
 * `v-html` in this file.
 *
 * Imports resolve relative to the emitted layout: `funktor/ui` at `<out>/ui/`, these tabs at
 * `<out>/insights/`. When the `@sdk` alias lands (see the vue-contributors plan) these become
 * `@sdk/ui/...` and stop depending on depth.
 */
import { computed } from 'vue'
import FactList from '../ui/FactList.vue'
import JsonTree from '../ui/JsonTree.vue'
import KeyValueTable from '../ui/KeyValueTable.vue'
import type { Fact } from '../ui/types.ts'
import { composeUrl, readRequest } from './slices.ts'

defineOptions({ name: 'RequestTab' })

const props = defineProps<{ data: unknown }>()

const slice = computed(() => readRequest(props.data))

const facts = computed<Fact[]>(() => {
    const value = slice.value
    if (value === null) return []

    return [
        { key: 'Method', value: value.method },
        { key: 'URL', value: composeUrl(value) },
    ]
})
</script>

<template>
    <div v-if="slice !== null">
        <section class="fk-section">
            <FactList :facts="facts" />
            <p class="fk-section__note">
                The recorded URL is a path; the query string is listed separately and is not part of it.
            </p>
        </section>

        <section class="fk-section">
            <h4 class="fk-section__title">Headers</h4>
            <KeyValueTable :entries="slice.headers" key-label="Header" empty-text="No headers recorded" />
        </section>

        <section class="fk-section">
            <h4 class="fk-section__title">Query parameters</h4>
            <KeyValueTable
                :entries="slice.queryParams"
                key-label="Parameter"
                empty-text="No query parameters"
            />
        </section>

        <details class="fk-details">
            <summary>Raw slice</summary>
            <div class="fk-details__body"><JsonTree :value="data" /></div>
        </details>
    </div>

    <!-- Unrecognised shape -- most likely a record older than this tab. Show the data, not an error. -->
    <JsonTree v-else :value="data" :expand-depth="2" />
</template>
