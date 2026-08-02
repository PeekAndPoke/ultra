<script setup lang="ts">
/**
 * A collapsible viewer for arbitrary JSON.
 *
 * This is the honest rendering for anything the framework cannot type: the insights envelope keeps
 * collector payloads as opaque JSON so an app-defined collector is possible at all, and
 * `AppConfigCollector` holds `Any`, which has no derivable shape by construction.
 *
 * **Everything rendered here is attacker-controlled** -- headers, paths, user agents, query values, all
 * recorded verbatim from unauthenticated requests. Keys and values go through interpolation, which
 * escapes. There is no `v-html` in this file and there must never be one.
 *
 * **Collapsed subtrees are not rendered at all** (`v-if`, not `v-show`). That is a load-bearing choice,
 * not a detail: the `kontainer` slice averages 137.8 KB and is 73.6% of a record, so rendering it
 * expanded by default would be the slowest thing in the UI.
 *
 * No `<style>` block, here or in any funktor/ui component -- see `theme.css`.
 */
import { computed, ref } from 'vue'

defineOptions({ name: 'JsonTree' })

const props = withDefaults(
    defineProps<{
        value?: unknown
        /** The key this node sits under. Null at the root, which then renders no key. */
        name?: string | null
        /** Depth to auto-expand to. 1 shows the top level's children; 0 shows a single collapsed node. */
        expandDepth?: number
        /** Set by the component on itself when recursing. Callers leave it alone. */
        depth?: number
    }>(),
    {
        value: undefined,
        name: null,
        expandDepth: 1,
        depth: 0,
    },
)

type Kind = 'null' | 'boolean' | 'number' | 'string' | 'array' | 'object'

const kind = computed<Kind>(() => {
    const value = props.value

    // `typeof null` is 'object', so null must be tested before the switch, and undefined is folded in
    // with it: JSON has no undefined, and a missing field reads as null everywhere else in these views.
    if (value === null || value === undefined) return 'null'
    if (Array.isArray(value)) return 'array'

    switch (typeof value) {
        case 'boolean':
            return 'boolean'
        case 'number':
            return 'number'
        case 'string':
            return 'string'
        default:
            return 'object'
    }
})

const isContainer = computed(() => kind.value === 'array' || kind.value === 'object')

const entries = computed<Array<[string, unknown]>>(() => {
    if (kind.value === 'array') {
        return (props.value as unknown[]).map((item, index) => [String(index), item])
    }
    if (kind.value === 'object') {
        return Object.entries(props.value as Record<string, unknown>)
    }
    return []
})

/**
 * The scalar, rendered the way JSON writes it.
 *
 * `JSON.stringify` rather than `String()` so a string is quoted and a control character is escaped --
 * which is the point in a debugging view, where `"123"` and `123` mean different things and an
 * invisible character in a header is exactly what someone is hunting for.
 */
const scalarText = computed(() => {
    if (props.value === undefined) return 'null'
    return JSON.stringify(props.value) ?? 'null'
})

const summary = computed(() => {
    const count = entries.value.length
    return kind.value === 'array' ? `[${count}]` : `{${count}}`
})

const open = ref(props.depth < props.expandDepth)
</script>

<template>
    <div class="fk-json">
        <div class="fk-json__row">
            <button
                v-if="isContainer"
                type="button"
                class="fk-json__toggle"
                :class="{ 'fk-json__toggle--open': open }"
                :aria-expanded="open"
                :aria-label="open ? 'Collapse' : 'Expand'"
                @click="open = !open"
            >
                <span class="fk-json__caret" aria-hidden="true"></span>
            </button>
            <span v-else class="fk-json__toggle fk-json__toggle--leaf" aria-hidden="true"></span>

            <span v-if="name !== null" class="fk-json__key">{{ name }}</span>

            <span v-if="isContainer" class="fk-json__summary">{{ summary }}</span>
            <span v-else class="fk-json__scalar" :class="`fk-json__scalar--${kind}`">{{ scalarText }}</span>
        </div>

        <div v-if="isContainer && open" class="fk-json__children">
            <JsonTree
                v-for="[childKey, childValue] in entries"
                :key="childKey"
                :name="childKey"
                :value="childValue"
                :depth="depth + 1"
                :expand-depth="expandDepth"
            />
            <div v-if="entries.length === 0" class="fk-json__empty">empty</div>
        </div>
    </div>
</template>
