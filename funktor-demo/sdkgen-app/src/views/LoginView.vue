<script setup lang="ts">
/**
 * Sign in to the operators realm.
 *
 * The part worth reading is the outcome switch. `AuthSignInResponse` is a three-way union and two of
 * its branches are NOT failures — the credentials were right and there is a defined next step.
 * Treating `activation-required` as a bad password is the obvious bug and it is silent, which is why
 * `completeSignIn` returns a four-way outcome instead of a boolean.
 */
import { ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { completeSignIn } from '../funktorsdk/runtime/login.ts'
import { REALM, auth, ensureAcl, session } from '../sdk.ts'

const router = useRouter()
const route = useRoute()

// Prefilled with the demo's seeded superuser. `InsightsApi` floors at `isSuperUser()`, so an
// ordinary operator would sign in fine and see nothing.
const email = ref('karsten.john.gerber@googlemail.com')
const password = ref('S3cret123!')

const busy = ref(false)
const problem = ref<string | null>(null)
const note = ref<string | null>(null)

async function submit(): Promise<void> {
    busy.value = true
    problem.value = null
    note.value = null

    try {
        const outcome = await completeSignIn(
            // The SESSION it writes to on success — the same singleton the transport reads, or the
            // token would be stored somewhere no request ever looks.
            session,
            // The realm is a path parameter; `email-password` is the provider id
            // (`EmailAndPasswordAuth.ID`).
            auth.login.signIn({ realm: REALM }, {
                _type: 'email_and_password',
                provider: 'email-password',
                email: email.value,
                password: password.value,
            }),
        )

        switch (outcome._type) {
            case 'signed-in':
                // Order matters: session first, THEN the matrix. Nothing gated should render before
                // the ACL has had a chance to load.
                ensureAcl()
                await router.push((route.query.redirect as string | undefined) ?? '/')
                break

            case 'org-selection-required':
                note.value = `Pick an organisation (${outcome.organisations.length} available).`
                break

            case 'activation-required':
                note.value = 'This account still needs activating — check your email.'
                break

            case 'rejected':
                // OUR copy, not the server's. `login.ts` warns that `outcome.message` is
                // server-authored, unsanitised, may quote request input, and distinguishes "no such
                // user" from "wrong password" — so showing it on a login form is an account
                // enumeration channel the SDK's own docs tell you to avoid. The detail goes to the
                // console for a developer; the form says one thing.
                console.warn('sign-in rejected', outcome.status, outcome.message)
                problem.value = 'Sign-in failed. Check your email and password.'
                break
        }
    } catch (e) {
        problem.value = (e as Error).message
    } finally {
        busy.value = false
    }
}
</script>

<template>
    <main class="login">
        <h1>Funktor Ops</h1>
        <p class="sub">Realm <code>{{ REALM }}</code></p>

        <form @submit.prevent="submit">
            <label>
                Email
                <input v-model="email" type="email" autocomplete="username" required />
            </label>

            <label>
                Password
                <input v-model="password" type="password" autocomplete="current-password" required />
            </label>

            <button type="submit" :disabled="busy">{{ busy ? 'Signing in…' : 'Sign in' }}</button>
        </form>

        <p v-if="problem" class="problem">{{ problem }}</p>
        <p v-else-if="note" class="note">{{ note }}</p>
    </main>
</template>

<style scoped>
.login {
    max-width: 22rem;
    margin: 4rem auto;
    font-family: system-ui, sans-serif;
}

.sub {
    color: #666;
}

form {
    display: grid;
    gap: 0.75rem;
    margin-top: 1.5rem;
}

label {
    display: grid;
    gap: 0.25rem;
    font-size: 0.9rem;
}

input {
    padding: 0.5rem;
    font: inherit;
}

button {
    padding: 0.6rem;
    font: inherit;
    cursor: pointer;
}

.problem {
    color: #b00020;
}

.note {
    color: #0a7;
}
</style>
