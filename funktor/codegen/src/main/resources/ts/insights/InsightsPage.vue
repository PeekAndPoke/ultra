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
 * **`client` is OPTIONAL, and that is what makes `mountAll` work.** A page the router constructs gets
 * no props, so a required client was `undefined` at run time — type-checked, mounted, and broken on
 * first render. Without a prop it builds one from the app-level config; an app that mounts this
 * itself can still pass a client and that wins.
 *
 * An app that wants the record in its URL should use `InsightsListPage` and `InsightsDetailPage`
 * directly and drive `bucket`/`file` from its own router -- both take them as props and emit their
 * navigation rather than performing it, precisely so that is possible.
 *
 * **This whole surface is superuser-only** (`InsightsApi`'s `authFloor`). The floor is on the server;
 * mounting this component is not what protects it, and hiding it is not either.
 */
import { ref } from 'vue'
import type { Ref } from 'vue'
import { FunktorInsightsClient } from '../funktorInsightsClient.ts'
import type { InsightsRecordRef } from '../models.ts'
import { useSdkConfigOrNull } from '../ui/sdkContext.ts'
import InsightsDetailPage from './InsightsDetailPage.vue'
import InsightsListPage from './InsightsListPage.vue'

defineOptions({ name: 'InsightsPage' })

const props = withDefaults(
    defineProps<{
        client?: FunktorInsightsClient
        epp?: number
    }>(),
    { epp: 20 },
)

// `inject` must run during setup, so it happens unconditionally and the choice is made after.
const injectedConfig = useSdkConfigOrNull()

if (props.client === undefined && injectedConfig === null) {
    throw new Error(
        "InsightsPage needs an API client: pass `:client`, or call `provideSdkConfig(app, config)` " +
            'once in your app entry point so pages mounted by `mountAll` can build their own.',
    )
}

// Resolved ONCE. The client is a stable singleton over the app's transport, so there is nothing for a
// computed to react to, and rebuilding one per render would discard nothing but cost allocations.
const client = props.client ?? new FunktorInsightsClient(injectedConfig!)

/**
 * Annotated as `Ref<…>` on the const rather than `ref<…>(null)`.
 *
 * The two are identical to TypeScript, and `vue-tsc` accepts both. IntelliJ does not: with the generic
 * form it cannot resolve properties off the unwrapped ref in a template — `selected.bucket` reports
 * *"Unresolved variable bucket"* — while the annotated form resolves cleanly. Measured both ways,
 * twice each, because that analyzer returns different answers on identical input.
 */
const selected: Ref<InsightsRecordRef | null> = ref(null)
</script>

<template>
    <!--
      `selected` is nullable, and the `v-if` is what makes `selected.bucket` safe. Two separate
      guarantees, both verified rather than assumed:

      RUNTIME: `v-if` on an element compiles to a ternary wrapping the whole vnode
      (`(_ctx.selected !== null) ? _createBlock(...) : ...`), so the props are never evaluated in the
      null case.

      TYPES: TypeScript narrows through it. Deleting the guard fails the build with TS18047 on exactly
      these two lines -- so this is enforced by `vue-tsc`, not by anyone remembering. That check lives in
      the consuming app, which is the only place a generated SDK is compiled by a real toolchain.
    -->
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
