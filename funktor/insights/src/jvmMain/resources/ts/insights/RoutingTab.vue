<script setup lang="ts">
/**
 * The `routing` slice: ktor's `RoutingResolveTrace`.
 *
 * One pre-formatted text tree, and TAB-SPECS is explicit that the JSON dump adds nothing here -- the
 * slice has exactly one field, so a raw view would show the same string with quotes around it.
 *
 * The trace contains the request path, which is attacker-controlled. `PreBlock` interpolates; reaching
 * for `v-html` to "keep the formatting" is precisely the mistake that would make this the XSS sink.
 *
 * See `RequestTab.vue` for the import-path note.
 */
import { computed } from 'vue'
import JsonTree from '../ui/JsonTree.vue'
import PreBlock from '../ui/PreBlock.vue'
import { readRoutingTrace } from './slices.ts'

defineOptions({ name: 'RoutingTab' })

const props = defineProps<{ data: unknown }>()

const trace = computed(() => readRoutingTrace(props.data))
</script>

<template>
    <section v-if="trace !== null" class="fk-section">
        <PreBlock :text="trace" empty-text="No routing trace recorded" />
    </section>

    <JsonTree v-else :value="data" :expand-depth="2" />
</template>
