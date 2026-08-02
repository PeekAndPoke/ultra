<script setup lang="ts">
/**
 * A small key/value list for the handful of scalar fields at the top of a tab.
 *
 * Distinct from {@link KeyValueTable}, which is for the repeating multi-valued case (headers, query
 * parameters). This one is a `<dl>`, is not sorted, and keeps the caller's order -- because for facts
 * the order IS the meaning: method before status before duration.
 *
 * No `<style>` block, here or in any funktor/ui component -- see `theme.css`.
 */
import type { Fact } from './types.ts'

defineOptions({ name: 'FactList' })

withDefaults(
    defineProps<{
        facts: Fact[]
        /** Rendered in place of a null/undefined value, so a missing fact is not a blank row. */
        placeholder?: string
    }>(),
    { placeholder: 'n/a' },
)
</script>

<template>
    <dl class="fk-facts">
        <template v-for="fact in facts" :key="fact.key">
            <dt class="fk-facts__key">{{ fact.key }}</dt>
            <dd class="fk-facts__value">
                <span v-if="fact.tone" class="fk-label" :class="`fk-label--${fact.tone}`">{{
                    fact.value ?? placeholder
                }}</span>
                <template v-else>{{ fact.value ?? placeholder }}</template>
            </dd>
        </template>
    </dl>
</template>
