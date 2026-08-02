<script setup lang="ts">
/**
 * A name/value table -- request and response headers, query parameters.
 *
 * Multi-valued by design: HTTP headers and query parameters both repeat, and flattening them to
 * `"a, b"` loses whether the client sent one header with a comma or two headers. Each value gets its
 * own line.
 *
 * **Every key and every value is attacker-controlled.** Interpolation escapes; never reach for `v-html`
 * to "format" a value.
 *
 * No `<style>` block, here or in any funktor/ui component -- see `theme.css`.
 */
import { computed } from 'vue'
import { FK_REDACTED } from './types.ts'

defineOptions({ name: 'KeyValueTable' })

const props = withDefaults(
    defineProps<{
        /** Values may be a list or a bare string; a bare string is treated as a single-element list. */
        entries?: Record<string, string[] | string> | null
        keyLabel?: string
        valueLabel?: string
        emptyText?: string
        /** Sort by key. On by default -- header order is not meaningful and stable reading is. */
        sort?: boolean
    }>(),
    {
        entries: null,
        keyLabel: 'Name',
        valueLabel: 'Value',
        emptyText: 'None recorded',
        sort: true,
    },
)

const rows = computed(() => {
    const src = props.entries
    if (!src) return []

    const out = Object.entries(src).map(([key, value]) => ({
        key,
        values: Array.isArray(value) ? value : [value],
    }))

    return props.sort ? out.sort((a, b) => a.key.localeCompare(b.key)) : out
})
</script>

<template>
    <table v-if="rows.length > 0" class="fk-kv">
        <thead>
            <tr>
                <th class="fk-kv__key-head">{{ keyLabel }}</th>
                <th>{{ valueLabel }}</th>
            </tr>
        </thead>
        <tbody>
            <tr v-for="row in rows" :key="row.key">
                <td class="fk-kv__key">{{ row.key }}</td>
                <td class="fk-kv__values">
                    <!--
                      A repeated header can legitimately send the same value twice, so the index is part
                      of the key -- the value alone is not unique.
                    -->
                    <div
                        v-for="(value, index) in row.values"
                        :key="`${index}:${value}`"
                        class="fk-kv__value"
                        :class="{ 'fk-kv__value--redacted': value === FK_REDACTED }"
                    >{{ value }}</div>
                </td>
            </tr>
        </tbody>
    </table>
    <div v-else class="fk-empty">{{ emptyText }}</div>
</template>
