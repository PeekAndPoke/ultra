<script setup lang="ts">
/**
 * The `log` slice: what was logged while handling the request.
 *
 * `text` is already formatted by `LogAppender.format` -- render it, do not re-parse it. It goes through
 * `PreBlock`, which interpolates: a log line can contain anything an attacker put in a header, and log
 * text is the classic place where "it's just text, let me keep the newlines" turns into stored XSS.
 *
 * An unknown level renders neutral rather than being dropped -- an app may define its own.
 *
 * See `RequestTab.vue` for the import-path note.
 */
import { computed } from 'vue'
import JsonTree from '../ui/JsonTree.vue'
import PreBlock from '../ui/PreBlock.vue'
import { readLogEntries, toneForLogLevel } from './slices.ts'

defineOptions({ name: 'LogTab' })

const props = defineProps<{ data: unknown }>()

const entries = computed(() => readLogEntries(props.data))
</script>

<template>
    <div v-if="entries !== null">
        <div v-if="entries.length === 0" class="fk-empty">No log entries</div>

        <section v-else class="fk-section">
            <!--
              Index is part of the key because two identical lines at the same level are ordinary -- a
              retry loop logs the same message repeatedly, and that repetition is often the finding.
            -->
            <div v-for="(entry, index) in entries" :key="`${index}:${entry.level}`" class="fk-log-entry">
                <span class="fk-label" :class="`fk-label--${toneForLogLevel(entry.level)}`">{{
                    entry.level ?? 'UNKNOWN'
                }}</span>
                <PreBlock :text="entry.text" empty-text="(empty log entry)" wrap />
            </div>
        </section>
    </div>

    <JsonTree v-else :value="data" :expand-depth="2" />
</template>
