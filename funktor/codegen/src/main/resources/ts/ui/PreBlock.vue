<script setup lang="ts">
/**
 * Pre-formatted text -- a routing trace, a log entry, a stack trace.
 *
 * The content is ALWAYS attacker-controlled. Interpolation escapes; `v-html` does not. This component
 * exists partly so that "render the pre-formatted thing" has an obvious safe answer, because the
 * shortcut it replaces (`v-html` on text that already contains newlines) is the single most likely way
 * to introduce stored XSS into these views.
 *
 * No `<style>` block, here or in any funktor/ui component -- see `theme.css`. A component that injects
 * its own CSS cannot be fully restyled by an app that declines to import the theme.
 */
defineOptions({ name: 'PreBlock' })

withDefaults(
    defineProps<{
        /** The text to render. Null and empty both count as nothing recorded. */
        text?: string | null
        /** Shown instead of an empty block, so "nothing here" reads differently from "failed to load". */
        emptyText?: string
        /** Wrap long lines instead of scrolling horizontally. Off by default: a trace reads better unwrapped. */
        wrap?: boolean
    }>(),
    {
        text: null,
        emptyText: 'Nothing recorded',
        wrap: false,
    },
)
</script>

<template>
    <pre v-if="text" class="fk-pre" :class="{ 'fk-pre--wrap': wrap }">{{ text }}</pre>
    <div v-else class="fk-empty">{{ emptyText }}</div>
</template>
