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
 * The matrix to gate on, or `null` while a FIRST load is still in flight.
 *
 * `null` means "no answer yet" and the menu withholds everything, rather than rendering entries
 * denied and letting them pop in one by one as the matrix lands. A RE-load — after a token refresh —
 * carries the previous matrix as `loading.stale`, which exists precisely so the menu does not blank
 * while a refresh is in flight; ignoring it made the whole navigation disappear and reappear.
 *
 * A logged-out visitor gets [ApiAcl.empty], which denies everything it has no row for. That is the
 * honest answer — the matrix endpoint needs a session — and it is not over-strict, because
 * `canAccess` short-circuits on a route the generator marked public.
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
 * `requires` is empty for an ungated page, and `every` over an empty array is true, so a page that
 * declares nothing is never hidden here.
 */
function visibleNav(): readonly SdkNavItem[] {
    // NOT named `acl` — that is the AclLoader singleton imported from `sdk.ts`, and shadowing it
    // here would read as the loader while being the matrix.
    const matrix = readableAcl()

    if (matrix === null) return []

    return navItems.filter((item) =>
        (!item.requiresAuth || auth.value.isLoggedIn) &&
        item.requires.every((route) => matrix.canAccess(route))
    )
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
