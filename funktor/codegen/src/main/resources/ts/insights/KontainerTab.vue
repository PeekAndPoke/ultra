<script setup lang="ts">
/**
 * The `kontainer` slice: every service definition, and which of them were instantiated.
 *
 * **The biggest slice by far -- 137.8 KB average, 73.6% of a record.** That is why the raw dump stays in
 * a collapsed `<details>` and why `JsonTree` renders nothing until opened.
 *
 * Services are ordered with instantiated ones first, then by FQN (`readKontainer` does the sort). That
 * was the old table's order and it is right: what the request actually touched is the usual question.
 *
 * The old tab also drew two vis.js graphs, and everything they needed IS derivable from the record --
 * unlike the vault graph. Not built here: it needs a graph library, which is a dependency decision for
 * the SDK rather than something this tab should pick unilaterally.
 *
 * See `RequestTab.vue` for the import-path note.
 */
import { computed } from 'vue'
import FactList from '../ui/FactList.vue'
import JsonTree from '../ui/JsonTree.vue'
import type { Fact } from '../ui/types.ts'
import { definitionChain, readKontainer } from './slices.ts'
import type { KontainerInstance } from './slices.ts'

defineOptions({ name: 'KontainerTab' })

const props = defineProps<{ data: unknown }>()

const slice = computed(() => readKontainer(props.data))

const summary = computed<Fact[]>(() => {
    const value = slice.value
    if (value === null) return []

    const total = value.numTotal ?? 0
    const old = value.numOld ?? 0

    return [
        // The old bar's `young / old / total`. "Old" is alive longer than 15 minutes -- a hardcoded
        // threshold in the collector, not something configurable, so it is stated rather than implied.
        { key: 'Instances (young / old / total)', value: `${total - old} / ${old} / ${total}` },
        { key: 'Services', value: value.services.length },
    ]
})

/**
 * Full date and time, not just the clock.
 *
 * The old table printed `Instant.toString()` in full, and the date is what separates "instantiated
 * during this request" from "alive since boot" -- which is the whole reason the table sorts
 * instantiated services first. `toLocaleTimeString` dropped it.
 */
function createdAt(instance: KontainerInstance): string {
    return instance.createdAtMillis === null ? 'n/a' : new Date(instance.createdAtMillis).toLocaleString()
}

function shortName(fqn: string | null): string {
    return fqn === null ? 'n/a' : (fqn.split('.').pop() ?? fqn)
}
</script>

<template>
    <div v-if="slice !== null">
        <section class="fk-section">
            <FactList :facts="summary" />
        </section>

        <section class="fk-section">
            <table class="fk-kv fk-services">
                <thead>
                    <tr>
                        <th>Service</th>
                        <th>Type</th>
                        <th>Instances</th>
                        <th>Injects</th>
                        <th>Definition &amp; overwrites</th>
                    </tr>
                </thead>
                <tbody>
                    <tr v-for="service in slice.services" :key="service.cls ?? ''">
                        <td class="fk-mono" :title="service.cls ?? ''">{{ shortName(service.cls) }}</td>
                        <td><span class="fk-label">{{ service.type ?? 'n/a' }}</span></td>

                        <td class="fk-mono">
                            <div v-if="service.instances.length === 0" class="fk-muted">-</div>
                            <div v-for="(instance, index) in service.instances" :key="index">
                                {{ shortName(instance.cls) }}
                                <span class="fk-muted">{{ createdAt(instance) }}</span>
                            </div>
                        </td>

                        <td class="fk-mono">
                            <div v-if="!service.definition?.injects.length" class="fk-muted">-</div>
                            <div v-for="inject in service.definition?.injects ?? []" :key="inject.name ?? ''">
                                {{ inject.name ?? '?' }}:
                                <span class="fk-muted">
                                    {{ inject.classes.map(shortName).join(', ') || 'n/a' }}
                                </span>
                                <!-- Lazy injection is why a singleton can hold a request-scoped service. -->
                                <span v-if="inject.provisionType === 'Lazy'" class="fk-label">Lazy</span>
                            </div>
                        </td>

                        <td class="fk-mono">
                            <div
                                v-for="(definition, index) in definitionChain(service.definition)"
                                :key="index"
                            >
                                <span v-if="index > 0" class="fk-muted">Overwrites -&gt;</span>
                                {{ definition.injectionType ?? '?' }} - {{ shortName(definition.creates) }}
                                <div v-if="definition.codeLocation" class="fk-muted">
                                    {{ definition.codeLocation }}
                                </div>
                            </div>
                        </td>
                    </tr>
                </tbody>
            </table>
        </section>

        <details class="fk-details">
            <summary>Raw slice -- large, around 137 KB</summary>
            <div class="fk-details__body"><JsonTree :value="data" /></div>
        </details>
    </div>

    <JsonTree v-else :value="data" :expand-depth="2" />
</template>
