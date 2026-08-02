<script setup lang="ts">
/**
 * Proof the whole chain works: session → attached token → ACL → a PROTECTED call.
 *
 * `listRecords` floors at `isSuperUser()` on the server, so reaching it means the token really was
 * attached and really was accepted. It is also the endpoint the contributed insights pages will use,
 * so this page goes away once they land.
 */
import { onMounted, ref, shallowRef } from 'vue'
import type { AclState } from '../funktorsdk/runtime/acl-loader.ts'
import { isSuccess } from '../funktorsdk/runtime/apiResponse.ts'
import { acl, insights, session } from '../sdk.ts'

const aclState = shallowRef<AclState>(acl.state())
acl.subscribe((s) => { aclState.value = s })

const state = session.state()

const records = ref<unknown[]>([])
const problem = ref<string | null>(null)
const loading = ref(true)

onMounted(async () => {
    try {
        const response = await insights.insights.listRecords({ page: 1, epp: 10 })

        if (!isSuccess(response)) {
            problem.value = response.messages?.map((m) => m.text).join('; ')
                ?? `${response.status.value} ${response.status.description}`
            return
        }

        records.value = response.data?.items ?? []
    } catch (e) {
        problem.value = (e as Error).message
    } finally {
        loading.value = false
    }
})
</script>

<template>
    <section>
        <h2>Signed in</h2>

        <dl>
            <dt>User</dt>
            <dd>{{ state.userId ?? '—' }}</dd>
            <dt>Superuser</dt>
            <dd>{{ state.permissions?.isSuperUser === true ? 'yes' : 'no' }}</dd>
            <dt>Token expires</dt>
            <dd>{{ state.expiresAt === null ? 'no expiry' : new Date(state.expiresAt).toLocaleString() }}</dd>
            <dt>Access matrix</dt>
            <dd>{{ aclState._type }}</dd>
        </dl>

        <h2>Insights records</h2>
        <p class="hint">
            A superuser-only endpoint. Reaching it proves the session reached the transport — nothing
            on this page passes a token by hand.
        </p>

        <p v-if="loading">Loading…</p>
        <p v-else-if="problem" class="problem">{{ problem }}</p>
        <p v-else-if="records.length === 0">
            No insights records yet. Make a few requests against the demo API and reload.
        </p>
        <p v-else>{{ records.length }} record(s) returned.</p>
    </section>
</template>

<style scoped>
dl {
    display: grid;
    grid-template-columns: max-content 1fr;
    gap: 0.25rem 1rem;
    margin: 1rem 0 2rem;
}

dt {
    color: #666;
}

.hint {
    color: #666;
    font-size: 0.9rem;
}

.problem {
    color: #b00020;
}
</style>
