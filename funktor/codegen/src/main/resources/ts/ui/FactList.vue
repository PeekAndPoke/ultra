<script setup lang="ts">
/**
 * The handful of scalar fields at the top of a tab, as a grid of small cards: a muted label above the value.
 *
 * Distinct from `KeyValueTable`, which is for the repeating multi-valued case (headers, query
 * parameters). This one is a `<dl>`, is not sorted, and keeps the caller's order -- because for facts
 * the order IS the meaning: method before status before duration.
 *
 * No `<style>` block, here or in any funktor/ui component -- see `theme.css`.
 */
import type { Fact } from './types.ts'

defineOptions({ name: 'FactList' })

const props = withDefaults(
    defineProps<{
        facts: Fact[]
        /** Rendered in place of a null/undefined value, so a missing fact is not a blank row. */
        placeholder?: string
    }>(),
    { placeholder: 'n/a' },
)

/** From this many characters on, a fact's card takes two columns. */
const WIDE_FACT_CHARS = 28

/** A card's width: the whole row for a `full` fact, two columns for a long value, else one. */
function widthOf(fact: Fact): string | null {
    if (fact.full === true) return 'fk-fact--full'
    return String(fact.value ?? props.placeholder).length > WIDE_FACT_CHARS ? 'fk-fact--wide' : null
}
</script>

<template>
    <dl class="fk-facts">
        <div v-for="fact in facts" :key="fact.key" class="fk-fact" :class="widthOf(fact)">
            <dt class="fk-facts__key">{{ fact.key }}</dt>
            <dd class="fk-facts__value">
                <span v-if="fact.tone" class="fk-label" :class="`fk-label--${fact.tone}`">{{
                    fact.value ?? placeholder
                }}</span>
                <template v-else>{{ fact.value ?? placeholder }}</template>
            </dd>
        </div>
    </dl>
</template>
