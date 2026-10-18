/**
 * The app's own routes, plus everything the SDK contributed.
 *
 * `mountAll(router)` is the line that makes a page shipped from a Kotlin module clickable here. It
 * is deliberately a RUNTIME call rather than a static array: the generator owns its output directory
 * and rewrites it every run, so it can write nothing into this file — which means this file never
 * has to change when a module starts or stops shipping pages.
 */
import { createRouter, createWebHistory } from 'vue-router'
import { mountAll } from './funktorsdk/mount.ts'
import { ensureAcl, session } from './sdk.ts'
import HomeView from './views/HomeView.vue'
import LoginView from './views/LoginView.vue'

export const router = createRouter({
    history: createWebHistory(),
    routes: [
        { path: '/', name: 'home', component: HomeView, meta: { requiresAuth: true } },
        { path: '/login', name: 'login', component: LoginView, meta: { requiresAuth: false } },
    ],
})

// Everything contributed by a Kotlin module. Empty until some contributor registers a page — the
// aggregate is emitted either way, so this line compiles regardless.
mountAll(router)

/**
 * The gate.
 *
 * `requiresAuth` is DECLARED by the contributor rather than derived from the access matrix, because
 * a logged-out visitor cannot fetch a matrix — that is the whole reason page gating and API-route
 * publicness are separate mechanisms.
 */
router.beforeEach((to) => {
    const loggedIn = session.state().isLoggedIn

    if (to.meta.requiresAuth === true && !loggedIn) {
        // `redirect` so the user lands where they were going, not on a generic home page.
        return { name: 'login', query: { redirect: to.fullPath } }
    }

    if (to.name === 'login' && loggedIn) return { path: '/' }

    // A no-op unless there is a session and NO matrix — see `ensureAcl`, which owns that guard.
    // Calling `acl.load()` directly here would re-fetch on every navigation.
    ensureAcl()

    return true
})
