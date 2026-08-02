<script setup lang="ts">
/**
 * The record list -- the entry point, and where the deleted insights BAR's job landed.
 *
 * The bar was an overlay on every page showing the current request's status and timing. It is gone
 * (maintainer, 2026-07-30); the same information is here, for every request rather than the current one.
 *
 * `path` is a path, not a URL: the writer stores `request.path()` deliberately so a `?token=` never
 * reaches the record. It is still attacker-controlled and still escaped on render.
 *
 * Imports assume the emitted layout -- SDK root at `<out>/`, these files at `<out>/insights/`.
 */
import { computed, ref, watch } from 'vue'
import type { FunktorInsightsClient } from '../funktorInsightsClient.ts'
import type { InsightsRecordRef, InsightsRecordSummary } from '../models.ts'
import { isSuccess } from '../runtime/apiResponse.ts'
import { toDate } from '../runtime/datetime.ts'
import { toneForStatus } from '../ui/types.ts'
import type { FkTone } from '../ui/types.ts'

defineOptions({ name: 'InsightsListPage' })

const props = withDefaults(
    defineProps<{
        client: FunktorInsightsClient
        /** Server-capped at 200 (`InsightsApi.MAX_EPP`); a larger value is silently coerced there. */
        epp?: number
    }>(),
    { epp: 20 },
)

const emit = defineEmits<{ select: [ref: InsightsRecordRef] }>()

const page = ref(1)
const items = ref<InsightsRecordSummary[]>([])
const fullItemCount = ref<number | null>(null)
const loading = ref(false)
const problem = ref<string | null>(null)

async function load(): Promise<void> {
    loading.value = true
    problem.value = null

    try {
        const response = await props.client.insights.listRecords({ page: page.value, epp: props.epp })

        // A non-2xx is a VALUE here, not a throw -- branch on it rather than wrapping in try/catch.
        if (!isSuccess(response)) {
            problem.value =
                response.messages?.map((message) => message.text).join('; ') ??
                `${response.status.value} ${response.status.description}`
            items.value = []
            return
        }

        items.value = response.data?.items ?? []
        fullItemCount.value = response.data?.fullItemCount ?? null
    } catch (error) {
        // Reaching here means no envelope at all -- a network failure, or a 403 page from a proxy.
        problem.value = error instanceof Error ? error.message : String(error)
        items.value = []
    } finally {
        loading.value = false
    }
}

watch(() => [page.value, props.epp, props.client], load, { immediate: true })

/**
 * Duration thresholds inherited from the old bar, minus its fourth band.
 *
 * It had olive above 75ms as well as yellow above 150 and red above 300. Three tones is what the theme
 * carries, and a fourth colour that means "slightly slow" earns less than it costs to explain.
 */
function durationTone(durationMs: number | null): FkTone {
    if (durationMs === null) return 'neutral'
    if (durationMs > 300) return 'error'
    if (durationMs > 150) return 'warn'
    return 'ok'
}

function formatDuration(durationMs: number | null): string {
    return durationMs === null ? 'n/a' : `${durationMs.toFixed(1)} ms`
}

function formatRecordedAt(summary: InsightsRecordSummary): string {
    // `human` on the wire is advisory -- parse `ts`, which is epoch millis, exactly as datetime.ts says.
    return summary.recordedAt === null ? 'n/a' : toDate(summary.recordedAt).toLocaleString()
}

const hasNextPage = computed(() => items.value.length >= props.epp)
</script>

<template>
    <div class="fk-root fk-insights">
        <h3 class="fk-section__title">Recorded requests</h3>

        <div v-if="problem !== null" class="fk-notice fk-notice--error">{{ problem }}</div>
        <div v-else-if="loading" class="fk-empty">Loading...</div>
        <div v-else-if="items.length === 0" class="fk-empty">No records</div>

        <table v-else class="fk-kv fk-records">
            <thead>
                <tr>
                    <th>Recorded</th>
                    <th>Method</th>
                    <th>Path</th>
                    <th>Status</th>
                    <th>Duration</th>
                </tr>
            </thead>
            <tbody>
                <tr
                    v-for="summary in items"
                    :key="`${summary.ref.bucket}/${summary.ref.file}`"
                    class="fk-records__row"
                    @click="emit('select', summary.ref)"
                >
                    <td class="fk-mono">{{ formatRecordedAt(summary) }}</td>
                    <td class="fk-mono">{{ summary.method ?? 'n/a' }}</td>
                    <td class="fk-mono fk-records__path">{{ summary.path ?? 'n/a' }}</td>
                    <td>
                        <span class="fk-label" :class="`fk-label--${toneForStatus(summary.status)}`">{{
                            summary.status ?? 'n/a'
                        }}</span>
                    </td>
                    <td>
                        <span class="fk-label" :class="`fk-label--${durationTone(summary.durationMs)}`">{{
                            formatDuration(summary.durationMs)
                        }}</span>
                    </td>
                </tr>
            </tbody>
        </table>

        <div class="fk-pager">
            <button type="button" class="fk-button" :disabled="page <= 1 || loading" @click="page -= 1">
                Previous
            </button>
            <span class="fk-muted">
                Page {{ page }}<template v-if="fullItemCount !== null"> of {{ Math.max(1, Math.ceil(fullItemCount / epp)) }}</template>
            </span>
            <button type="button" class="fk-button" :disabled="!hasNextPage || loading" @click="page += 1">
                Next
            </button>
        </div>
    </div>
</template>
