<script setup lang="ts">
/**
 * The shell: navigation, the routed page, and sign-out.
 *
 * The navigation is the interesting part. Entries come from `navItems`, which every contributing
 * Kotlin module feeds — so a module that starts shipping a page appears in this menu without this
 * file changing.
 *
 * **Gated on the ACL, and the `loading` state is handled deliberately.** Rendering a denied menu
 * while the matrix is in flight makes entries pop in one by one as it lands, which reads as broken;
 * withholding them until it resolves does not.
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

/** Entries to show. Withheld entirely while the matrix loads — see the note above. */
function visibleNav(): readonly { path: string; label: string }[] {
    if (!auth.value.isLoggedIn) return navItems.filter((item) => !item.requiresAuth)
    if (aclState.value._type !== 'ready') return []

    return navItems
}

async function leave(): Promise<void> {
    signOut()
    await router.push({ name: 'login' })
}
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
