<script setup lang="ts">
/**
 * Draws one network with `vis-network`, loaded on first use.
 *
 * **The library is the APP's dependency**, not the SDK's: `sdk:ts:generate` refuses to write an SDK
 * whose imports `package.json` does not declare. The import is dynamic, so the ~650 KB bundle is a
 * separate chunk that only a click on a graph button downloads.
 *
 * `vis-network/standalone` bundles its own `vis-data` and the other peers, and injects its stylesheet
 * into `<head>` (a CSP must allow inline styles for the tooltip). Labels are drawn on a canvas.
 *
 * Mounted only when its graph is asked for: a canvas created while hidden cannot be laid out, which is
 * why the old GUI waited 500 ms after revealing the container.
 *
 * A failure to load or draw is shown here and stays here -- the tab around it keeps rendering.
 */
import { onBeforeUnmount, onMounted, ref } from 'vue'
import type { Ref } from 'vue'
import type { NetworkData } from './graphs.ts'
import { networkOptions } from './graphs.ts'

defineOptions({ name: 'NetworkGraph' })

const props = defineProps<{ data: NetworkData }>()

/** Annotated as `Ref<…>` -- see the note in `InsightsPage.vue`; the generic form breaks IntelliJ. */
const canvas: Ref<HTMLElement | null> = ref(null)
const problem: Ref<string | null> = ref(null)

let network: { destroy(): void } | null = null
let unmounted = false

onMounted(async () => {
    try {
        const { Network } = await import('vis-network/standalone')

        // The import is async; the tab may have been closed meanwhile.
        if (unmounted || canvas.value === null) return

        network = new Network(
            canvas.value,
            { nodes: props.data.nodes, edges: props.data.edges },
            networkOptions(props.data.kind),
        )
    } catch (error) {
        problem.value = `The graph could not be drawn: ${error instanceof Error ? error.message : String(error)}`
    }
})

onBeforeUnmount(() => {
    unmounted = true
    network?.destroy()
    network = null
})
</script>

<template>
    <div class="fk-graph">
        <p class="fk-section__note">
            {{ data.nodes.length }} nodes, {{ data.edges.length }} edges<template v-if="data.undrawn > 0"
                >, {{ data.undrawn }} not drawn: their target is no node in this graph</template
            >.
        </p>
        <div v-if="problem !== null" class="fk-notice fk-notice--error">{{ problem }}</div>
        <div v-else ref="canvas" class="fk-graph__canvas"></div>
    </div>
</template>
