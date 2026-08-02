<script setup lang="ts">
/**
 * A horizontal strip of value/label cells -- the runtime figures, the vault query totals.
 *
 * `tone` carries MEANING, not decoration: it is how a 500 or a slow query is spotted at a glance. The
 * theme's status variables are the ones an app should be most reluctant to rebrand.
 *
 * No `<style>` block, here or in any funktor/ui component -- see `theme.css`. A component that injects
 * its own CSS cannot be fully restyled by an app that declines to import the theme.
 */
import type { StatCell } from './types.ts'

defineOptions({ name: 'StatStrip' })

withDefaults(
    defineProps<{
        cells: StatCell[]
        /** Rendered in place of a null/undefined value, so a missing figure is not an empty box. */
        placeholder?: string
    }>(),
    { placeholder: 'n/a' },
)
</script>

<template>
    <div class="fk-strip">
        <div
            v-for="cell in cells"
            :key="cell.label"
            class="fk-strip__cell"
            :class="cell.tone ? `fk-strip__cell--${cell.tone}` : null"
        >
            <span class="fk-strip__value">{{ cell.value ?? placeholder }}</span>
            <span class="fk-strip__label">{{ cell.label }}</span>
            <span v-if="cell.hint" class="fk-strip__hint">{{ cell.hint }}</span>
        </div>
    </div>
</template>
