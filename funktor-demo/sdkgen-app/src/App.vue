<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { FunktorConfClient } from './funktorsdk/funktorConfClient.ts'
import type { EventModel } from './funktorsdk/models.ts'
import { isSuccess } from './funktorsdk/runtime/apiResponse.ts'
import { fetchTransport } from './funktorsdk/runtime/http.ts'

// The API is mounted behind a HOST matcher — `host("api.*".toRegex())` in the demo's server.kt — so
// it is not served on plain localhost. This is the same base the Kraft frontends use
// (`AdminAppConfig.apiBaseUrl`), and the server's CORS list allows this app's origin.
const client = new FunktorConfClient({
    baseUrl: 'http://api.funktor-demo.localhost:36587',
    transport: fetchTransport(),
})

// `listEvents` is one of the group classes, destructured off the client. This is the Vue-composable
// idiom, and it is exactly why members are emitted as arrow class fields rather than prototype
// methods — a prototype method type-checks here and throws at run time.
const { listEvents } = client.funktorConf

const events = ref<EventModel[]>([])
const problem = ref<string | null>(null)
const loading = ref(true)

onMounted(async () => {
    try {
        const response = await listEvents()

        // Non-2xx is a VALUE, not a throw — the contract the Kotlin ApiClient defends and the
        // generated SDK mirrors. Branch on it; do not wrap this in a try/catch and expect it there.
        if (!isSuccess(response)) {
            problem.value = response.messages?.map((m) => m.text).join('; ')
                ?? `${response.status.value} ${response.status.description}`
            return
        }

        // `data` is nullable even on 2xx — noContent() and okOrNotFound() both send null.
        events.value = response.data ?? []
    } catch (e) {
        // Reaching here means the response was not an ApiResponse envelope at all, or the network
        // failed. Note the message names the drifted FIELDS but never carries the payload.
        problem.value = (e as Error).message
    } finally {
        loading.value = false
    }
})
</script>

<template>
    <main>
        <h1>funktor SDK generator</h1>
        <p class="sub">
            Generated TypeScript, consumed by Vue. Nothing here is hand-written against the API —
            the client, the models and the schemas all came out of the Kotlin route graph.
        </p>

        <p v-if="loading">Loading events…</p>

        <p v-else-if="problem" class="problem">{{ problem }}</p>

        <p v-else-if="events.length === 0">
            No events yet. Start the demo server and add one.
        </p>

        <ul v-else>
            <li v-for="event in events" :key="event.id">
                <strong>{{ event.name }}</strong>
                <!-- `status` is a Kotlin enum, emitted as a literal union — so a typo here is a
                     compile error, not a runtime surprise. -->
                <span class="status">{{ event.status }}</span>
                <span class="when">{{ event.startDate }} – {{ event.endDate }}</span>
            </li>
        </ul>
    </main>
</template>

<style scoped>
main {
    font-family: system-ui, sans-serif;
    max-width: 40rem;
    margin: 3rem auto;
    padding: 0 1rem;
}

.sub {
    color: #555;
}

.problem {
    color: #b00020;
}

.status {
    color: #0a7;
    margin-left: 0.5rem;
    font-size: 0.85em;
    text-transform: uppercase;
}

.when {
    color: #555;
    margin-left: 0.5rem;
}
</style>
