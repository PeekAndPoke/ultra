<script setup lang="ts">
/**
 * One record: the overview, then a tab per collector slice.
 *
 * ## The tab registry is OPEN, and that is the point
 *
 * `collectors` is an open envelope so an app-defined collector is possible at all. A key with no
 * registered component therefore is NOT an error -- it renders through `JsonTree`, which is also how
 * the four collectors whose tabs are not built yet (`runtime`, `vault`, `kontainer`, `app-config`)
 * currently appear. The page is complete and usable now; tabs improve it as they land.
 *
 * ## Why the headline is repeated here
 *
 * A `BRIEF` record has no collectors at all, so a detail page reached by deep link rather than from
 * the list would otherwise be an empty envelope with no way to tell it from a record that genuinely
 * collected nothing. `method`/`path`/`status` on the record itself are what distinguish them.
 *
 * Imports assume the emitted layout -- SDK root at `<out>/`, these files at `<out>/insights/`.
 */
import { computed, ref, watch } from 'vue'
import type { Component, Ref } from 'vue'
import type { FunktorInsightsClient } from '../funktorInsightsClient.ts'
import type { InsightsCollectorSlice, InsightsRecord, InsightsRecordRef } from '../models.ts'
import { isSuccess } from '../runtime/apiResponse.ts'
import { toDate } from '../runtime/datetime.ts'
import FactList from '../ui/FactList.vue'
import JsonTree from '../ui/JsonTree.vue'
import { toneForStatus } from '../ui/types.ts'
import type { Fact } from '../ui/types.ts'
import StatStrip from '../ui/StatStrip.vue'
import AppConfigTab from './AppConfigTab.vue'
import KontainerTab from './KontainerTab.vue'
import LogTab from './LogTab.vue'
import RequestTab from './RequestTab.vue'
import ResponseTab from './ResponseTab.vue'
import RoutingTab from './RoutingTab.vue'
import RuntimeTab from './RuntimeTab.vue'
import TemplateTab from './TemplateTab.vue'
import UserTab from './UserTab.vue'
import VaultTab from './VaultTab.vue'
import { readRuntime, readVault, runtimeCells, vaultCells } from './slices.ts'

defineOptions({ name: 'InsightsDetailPage' })

const props = defineProps<{
    client: FunktorInsightsClient
    bucket: string
    file: string
}>()

/** Call-signature form, not the object map -- see the note in `InsightsListPage.vue`. */
const emit = defineEmits<{ (e: 'navigate', ref: InsightsRecordRef): void; (e: 'back'): void }>()

/** Collector key to component. A key that is absent falls through to `JsonTree`. */
const TABS: Record<string, Component> = {
    request: RequestTab,
    response: ResponseTab,
    user: UserTab,
    routing: RoutingTab,
    template: TemplateTab,
    log: LogTab,
    runtime: RuntimeTab,
    vault: VaultTab,
    kontainer: KontainerTab,
    'app-config': AppConfigTab,
}

/** Display names for the keys we know. An unknown key shows its raw key, which is the honest label. */
const TAB_LABELS: Record<string, string> = {
    request: 'Request',
    response: 'Response',
    user: 'User',
    routing: 'Routing',
    runtime: 'Runtime',
    log: 'Logs',
    template: 'View',
    vault: 'Database',
    kontainer: 'Kontainer',
    'app-config': 'Config',
}

/** Annotated as `Ref<…>` -- see the note in `InsightsPage.vue`; the generic form breaks IntelliJ. */
const record: Ref<InsightsRecord | null> = ref(null)
const loading = ref(false)
const problem = ref<string | null>(null)
const activeKey = ref<string | null>(null)

async function load(): Promise<void> {
    loading.value = true
    problem.value = null

    try {
        const response = await props.client.insights.getRecord({ bucket: props.bucket, file: props.file })

        if (!isSuccess(response)) {
            problem.value =
                response.messages?.map((message) => message.text).join('; ') ??
                `${response.status.value} ${response.status.description}`
            record.value = null
            return
        }

        // `data` is nullable even on 2xx -- `okOrNotFound()` sends null, which is what a missing record is.
        record.value = response.data
        if (response.data === null) problem.value = 'This record no longer exists.'

        activeKey.value = response.data?.collectors[0]?.key ?? null
    } catch (error) {
        problem.value = error instanceof Error ? error.message : String(error)
        record.value = null
    } finally {
        loading.value = false
    }
}

watch(() => [props.bucket, props.file, props.client], load, { immediate: true })

const overview = computed<Fact[]>(() => {
    const value = record.value
    if (value === null) return []

    return [
        { key: 'Request', value: [value.method, value.path].filter((part) => part !== null).join(' ') || null },
        { key: 'Status', value: value.status, tone: toneForStatus(value.status) },
        { key: 'Duration', value: value.durationMs === null ? null : `${value.durationMs.toFixed(1)} ms` },
        { key: 'Recorded', value: value.recordedAt === null ? null : toDate(value.recordedAt).toLocaleString() },
    ]
})

/**
 * The record's slices, or none.
 *
 * The template reads THIS rather than `record.collectors`, and every lookup below goes through an
 * annotated local with an explicitly typed callback parameter. All three shapes are equivalent to
 * TypeScript; IntelliJ resolves only this one. Measured -- the direct
 * `record.value?.collectors.find((s) => …)` form reports *"Argument types do not match parameters"*.
 */
const collectors = computed<InsightsCollectorSlice[]>(() => {
    const rec: InsightsRecord | null = record.value
    return rec === null ? [] : rec.collectors
})

const activeSlice = computed<InsightsCollectorSlice | null>(() => {
    return collectors.value.find((slice: InsightsCollectorSlice) => slice.key === activeKey.value) ?? null
})

function sliceFor(key: string): unknown {
    return collectors.value.find((slice: InsightsCollectorSlice) => slice.key === key)?.data ?? null
}

/**
 * The Database and Runtime strips, inlined into Overview.
 *
 * The old GUI showed these two here as well as in their own tabs -- they are the figures worth seeing
 * without choosing a tab first. Empty when the collector is absent, which is normal for a BRIEF record.
 */
const overviewVault = computed(() => {
    const parsed = readVault(sliceFor('vault'))
    return parsed === null ? [] : vaultCells(parsed)
})

const overviewRuntime = computed(() => {
    const parsed = readRuntime(sliceFor('runtime'))
    return parsed === null ? [] : runtimeCells(parsed)
})

function labelFor(key: string): string {
    return TAB_LABELS[key] ?? key
}
</script>

<template>
    <div class="fk-root fk-insights">
        <div class="fk-toolbar">
            <button type="button" class="fk-button" @click="emit('back')">Back to list</button>
            <span class="fk-toolbar__spacer"></span>
            <button
                type="button"
                class="fk-button"
                :disabled="!record?.previous"
                @click="record?.previous && emit('navigate', record.previous)"
            >
                Previous
            </button>
            <button
                type="button"
                class="fk-button"
                :disabled="!record?.next"
                @click="record?.next && emit('navigate', record.next)"
            >
                Next
            </button>
        </div>

        <div v-if="problem !== null" class="fk-notice fk-notice--error">{{ problem }}</div>
        <div v-else-if="loading" class="fk-empty">Loading...</div>

        <template v-else-if="record !== null">
            <section class="fk-section">
                <FactList :facts="overview" />
                <p class="fk-section__note">
                    Neighbours are within the same day folder, so the oldest record of a day reports no
                    previous even when earlier days exist.
                </p>
            </section>

            <section v-if="overviewVault.length > 0" class="fk-section">
                <h4 class="fk-section__title">Database</h4>
                <StatStrip :cells="overviewVault" />
            </section>

            <section v-if="overviewRuntime.length > 0" class="fk-section">
                <h4 class="fk-section__title">Runtime</h4>
                <StatStrip :cells="overviewRuntime" />
            </section>

            <div v-if="collectors.length === 0" class="fk-empty">
                This record collected nothing -- it was recorded in BRIEF mode.
            </div>

            <template v-else>
                <nav class="fk-tabs">
                    <button
                        v-for="slice in collectors"
                        :key="slice.key"
                        type="button"
                        class="fk-tabs__tab"
                        :class="{ 'fk-tabs__tab--active': slice.key === activeKey }"
                        :aria-selected="slice.key === activeKey"
                        @click="activeKey = slice.key"
                    >
                        {{ labelFor(slice.key) }}
                    </button>
                </nav>

                <section v-if="activeSlice !== null" class="fk-tabs__panel">
                    <component :is="TABS[activeSlice.key]" v-if="TABS[activeSlice.key]" :data="activeSlice.data" />
                    <!-- No tab for this key: an app-defined collector, or one not built yet. -->
                    <JsonTree v-else :value="activeSlice.data" :expand-depth="2" />
                </section>
            </template>
        </template>
    </div>
</template>
