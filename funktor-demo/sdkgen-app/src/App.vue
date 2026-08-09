<script setup lang="ts">
/**
 * The shell: navigation, the routed page, and sign-out.
 *
 * The navigation is the interesting part. Entries come from `navItems`, which every contributing
 * Kotlin module feeds — so a module that starts shipping a page appears in this menu without this
 * file changing.
 *
 * **Gating is `requiresAuth` ONLY — the menu does not consult the access matrix.** It cannot:
 * `SdkNavItem` carries `path`, `label`, `icon` and `requiresAuth`, and nothing that identifies the
 * ROUTES a page calls, so there is nothing to look up in `ApiAcl`. The consequence is real and worth
 * knowing: an ordinary operator sees "Insights" and lands on a page whose every call 403s, because
 * `InsightsApi` floors at `isSuperUser()`. Making that work needs contributors to declare the routes
 * their page needs — tracked as `.claude/tasks/20260809-acl-aware-navigation.md`.
 *
 * This KDoc used to claim the menu was ACL-gated. It never was; `/feature-review` caught it
 * (2026-08-09).
 */
import { shallowRef } from 'vue'
import { useRouter } from 'vue-router'
import type { AclState } from './funktorsdk/runtime/acl-loader.ts'
import type { AuthSessionState } from './funktorsdk/runtime/auth.ts'
import { navItems } from './funktorsdk/mount.ts'
import type { UserPermissions } from './funktorsdk/models.ts'
import { acl, session, signOut } from './sdk.ts'

const router = useRouter()

const auth = shallowRef<AuthSessionState<UserPermissions>>(session.state())
session.subscribe((s) => { auth.value = s })

const aclState = shallowRef<AclState>(acl.state())
acl.subscribe((s) => { aclState.value = s })

/**
 * Entries to show.
 *
 * Withheld only on a FIRST load. A re-load — after a token refresh — carries the previous matrix as
 * `loading.stale`, which exists precisely so the menu does not blank while a refresh is in flight;
 * ignoring it made the whole navigation disappear and reappear on every reload.
 */
function visibleNav(): readonly { path: string; label: string }[] {
    if (!auth.value.isLoggedIn) return navItems.filter((item) => !item.requiresAuth)

    const state = aclState.value
    const settled = state._type === 'ready' || (state._type === 'loading' && state.stale !== null)

    return settled ? navItems : []
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
