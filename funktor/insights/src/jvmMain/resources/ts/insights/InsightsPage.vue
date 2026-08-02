<script setup lang="ts">
/**
 * The mountable entry point: list, then detail, with navigation between them.
 *
 * Deliberately router-free. It holds which record is selected in local state, so an app can mount it at
 * a single route with no configuration:
 *
 * ```vue
 * <InsightsPage :client="new FunktorInsightsClient({ baseUrl, transport })" />
 * ```
 *
 * An app that wants the record in its URL should use `InsightsListPage` and `InsightsDetailPage`
 * directly and drive `bucket`/`file` from its own router -- both take them as props and emit their
 * navigation rather than performing it, precisely so that is possible.
 *
 * **This whole surface is superuser-only** (`InsightsApi`'s `authFloor`). The floor is on the server;
 * mounting this component is not what protects it, and hiding it is not either.
 */
import { ref } from 'vue'
import type { FunktorInsightsClient } from '../funktorInsightsClient.ts'
import type { InsightsRecordRef } from '../models.ts'
import InsightsDetailPage from './InsightsDetailPage.vue'
import InsightsListPage from './InsightsListPage.vue'

defineOptions({ name: 'InsightsPage' })

withDefaults(
    defineProps<{
        client: FunktorInsightsClient
        epp?: number
    }>(),
    { epp: 20 },
)

const selected = ref<InsightsRecordRef | null>(null)
</script>

<template>
    <InsightsDetailPage
        v-if="selected !== null"
        :client="client"
        :bucket="selected.bucket"
        :file="selected.file"
        @navigate="selected = $event"
        @back="selected = null"
    />
    <InsightsListPage v-else :client="client" :epp="epp" @select="selected = $event" />
</template>
