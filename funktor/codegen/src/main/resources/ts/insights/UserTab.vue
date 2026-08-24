<script setup lang="ts">
/**
 * The `user` slice: who the request was attributed to.
 *
 * **Nothing here is an authorization decision, and this view must never suggest otherwise.** It records
 * what the server resolved for that one request; the server re-derives identity and permissions from
 * the verified token on every request regardless. `isSuperUser` is shown because it explains what the
 * request was allowed to do, not because anything downstream trusts this record.
 *
 * Permissions stay a JSON tree: the sets are the application's shape, not the framework's, so any
 * table would be guessing.
 *
 * See `RequestTab.vue` for the import-path note.
 */
import { computed } from 'vue'
import FactList from '../ui/FactList.vue'
import JsonTree from '../ui/JsonTree.vue'
import type { Fact } from '../ui/types.ts'
import { readUser } from './slices.ts'

defineOptions({ name: 'UserTab' })

const props = defineProps<{ data: unknown }>()

const slice = computed(() => readUser(props.data))

/** Rendered as words rather than `true`/`false`: a row reading "Anonymous false" is a double negative. */
function yesNo(value: boolean | null): string | null {
    return value === null ? null : value ? 'yes' : 'no'
}

const facts = computed<Fact[]>(() => {
    const value = slice.value
    if (value === null) return []

    return [
        { key: 'User id', value: value.userId },
        { key: 'Description', value: value.desc },
        { key: 'Email', value: value.email },
        { key: 'Type', value: value.type },
        { key: 'Client IP', value: value.clientIp },
        // The discriminator, not the two boolean rows this used to show: `isAnonymous`/`isSystem` are
        // FUNCTIONS on UserRecord, so Slumber never wrote them and both always read n/a.
        { key: 'Kind', value: value.kind },
        {
            key: 'Superuser',
            value: yesNo(value.isSuperUser),
            // Toned only when true: a superuser request is the one worth spotting in a list of records.
            tone: value.isSuperUser === true ? 'warn' : undefined,
        },
    ]
})
</script>

<template>
    <div v-if="slice !== null">
        <section class="fk-section">
            <h4 class="fk-section__title">User</h4>
            <FactList :facts="facts" />
        </section>

        <section class="fk-section">
            <h4 class="fk-section__title">Permissions</h4>
            <p class="fk-section__note">
                What this request resolved to. The server re-derives permissions from the verified token
                on every request; nothing is authorized from a record.
            </p>
            <JsonTree :value="slice.permissions" :expand-depth="2" />
        </section>

        <details class="fk-details">
            <summary>Raw slice</summary>
            <div class="fk-details__body"><JsonTree :value="data" /></div>
        </details>
    </div>

    <JsonTree v-else :value="data" :expand-depth="2" />
</template>
