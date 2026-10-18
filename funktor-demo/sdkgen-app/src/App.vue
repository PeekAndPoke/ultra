<script setup lang="ts">
/**
 * The shell: navigation, the routed page, and sign-out.
 *
 * The navigation is the interesting part. Entries come from `navItems`, which every contributing
 * Kotlin module feeds — so a module that starts shipping a page appears in this menu without this
 * file changing.
 *
 * **Two gates, and they are different mechanisms.** `requiresAuth` is DECLARED by the contributor and
 * deliberately coarse — it is the only thing a logged-out visitor can be judged by, since the matrix
 * endpoint is itself authenticated. `requires` is the access matrix: the routes the page cannot work
 * without, resolved at generation time, ALL of which must be accessible.
 *
 * Both are ADVISORY. The server is the authority; this decides what to RENDER. The router guard
 * deliberately does NOT consult the matrix, so a user who types `/insights` still reaches the page
 * and it 403s honestly — a redirect driven by a matrix that failed to load would be the worse
 * failure (maintainer, 2026-08-24).
 */
import { shallowRef } from 'vue'
import { useRouter } from 'vue-router'
import type { AclState } from './funktorsdk/runtime/acl-loader.ts'
import { ApiAcl } from './funktorsdk/runtime/acl.ts'
import type { AuthSessionState } from './funktorsdk/runtime/auth.ts'
import { navItems, type SdkNavItem } from './funktorsdk/mount.ts'
import type { UserPermissions } from './funktorsdk/models.ts'
import { acl, session, signOut } from './sdk.ts'

const router = useRouter()

const auth = shallowRef<AuthSessionState<UserPermissions>>(session.state())
session.subscribe((s) => { auth.value = s })

const aclState = shallowRef<AclState>(acl.state())
acl.subscribe((s) => { aclState.value = s })

/**
 * The matrix to gate on, or `null` when there is no answer yet.
 *
 * A logged-out visitor gets [ApiAcl.empty], which denies everything it has no row for. That is the
 * honest answer — the matrix endpoint needs a session — and it is not over-strict, because
 * `canAccess` short-circuits on a route the generator marked public.
 *
 * **The `loading.stale` branch is unreachable as THIS app is wired, and the menu therefore does
 * blank for the length of a matrix fetch on every token refresh.** `AclLoader.load()` sets `stale`
 * from its own `ready` state, and the only path into it here is `ensureAcl()`, which fires only when
 * the state is already `absent` — and the refresh handler in `sdk.ts` calls `clear()` first, on
 * purpose, so the departing token's matrix cannot be read as current.
 *
 * That blank is the FAIL-CLOSED direction and it is kept deliberately: the alternative renders
 * pre-refresh permissions during the reload window, so a revocation would not show until the new
 * matrix lands. The branch stays because `AclLoader` is shared runtime and another app may drive it
 * differently.
 *
 * Worth stating plainly, because the comment here previously claimed the opposite: honouring
 * `loading.stale` was itself a `/feature-review` fix (2026-08-09), and the `absent` guard and the
 * `clear()`-before-reload added in the SAME batch made it dead on arrival. Caught by two reviewers
 * independently on 2026-08-24.
 */
function readableAcl(): ApiAcl | null {
    if (!auth.value.isLoggedIn) return ApiAcl.empty

    const state = aclState.value

    if (state._type === 'ready') return state.acl
    if (state._type === 'loading') return state.stale

    return null
}

/**
 * Entries to show — both gates, in one predicate.
 *
 * With no matrix yet, only the ACCESS clause is unknown. An entry that declares no requirements is
 * not waiting on anything, so it still shows — `SdkNavItem.requires` promises "empty means the entry
 * is never hidden on access grounds", and blanking the whole menu broke that promise for every
 * contributed page that does not gate. A second module's public "Docs" entry used to vanish for the
 * length of an unrelated fetch. Raised in the 2026-08-24 review gate.
 */
function visibleNav(): readonly SdkNavItem[] {
    // NOT named `acl` — that is the AclLoader singleton imported from `sdk.ts`, and shadowing it
    // here would read as the loader while being the matrix.
    const matrix = readableAcl()

    return navItems.filter((item) => {
        if (item.requiresAuth && !auth.value.isLoggedIn) return false

        // Withhold rather than render denied: entries must not pop in one by one as the matrix lands.
        if (matrix === null) return item.requires.length === 0

        return item.requires.every((route) => matrix.canAccess(route))
    })
}

async function leave(): Promise<void> {
    signOut()
    await router.push({ name: 'login' })
}

/**
 * Leave a protected page when the session ends WITHOUT the user asking.
 *
 * A refresh rejected at expiry, or an access-matrix fetch that 403s, calls `signOut()` from inside
 * `sdk.ts` — which cannot reach the router (`router.ts` imports it, so the dependency only goes one
 * way). Nothing unmounted the routed component and `beforeEach` only runs on a navigation, so the
 * page stayed fully rendered on an ended session: for `/insights` that means other users' request
 * traces, headers and identities left on screen indefinitely. Found by `/feature-review`, 2026-08-09.
 *
 * Watched here because this is where both the session and the router are in scope.
 */
session.subscribe((state) => {
    if (state.isLoggedIn) return
    if (router.currentRoute.value.meta.requiresAuth !== true) return

    void router.replace({ name: 'login' })
})
</script>

<template>
    <div class="shell">
        <header>
            <strong>Funktor Ops</strong>

            <nav>
                <RouterLink to="/">Overview</RouterLink>
                <RouterLink v-for="item in visibleNav()" :key="item.path" :to="item.path">
                    {{ item.label }}
                </RouterLink>
            </nav>

            <span v-if="auth.isLoggedIn" class="who">
                {{ auth.userId }}
                <button type="button" @click="leave">Sign out</button>
            </span>
        </header>

        <main>
            <RouterView />
        </main>
    </div>
</template>

<style scoped>
.shell {
    font-family: system-ui, sans-serif;
    max-width: 52rem;
    margin: 0 auto;
    padding: 0 1rem;
}

header {
    display: flex;
    align-items: center;
    gap: 1.5rem;
    padding: 1rem 0;
    border-bottom: 1px solid #ddd;
}

nav {
    display: flex;
    gap: 1rem;
}

.who {
    margin-left: auto;
    display: flex;
    align-items: center;
    gap: 0.75rem;
    color: #666;
    font-size: 0.9rem;
}

button {
    font: inherit;
    cursor: pointer;
}

main {
    padding: 1.5rem 0;
}
</style>
