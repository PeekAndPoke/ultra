/**
 * The sign-in FLOW — what to do with the response, as opposed to how to send the request.
 *
 * Sign-in is not one outcome. `AuthSignInResponse` is a three-way union and two of its branches are
 * **not failures**: the credentials were right and there is a defined next step. Getting that wrong —
 * treating `activation-required` as a bad password — is the obvious bug, it is silent, and it is worth
 * writing down once rather than in every app.
 *
 * ```ts
 * const outcome = await completeSignIn(session, client.login.signIn({ realm }, credentials))
 *
 * switch (outcome._type) {
 *     case 'signed-in':               router.push('/'); break
 *     case 'org-selection-required':  showOrgPicker(outcome.organisations, outcome.selectionToken); break
 *     case 'activation-required':     showResendActivation(outcome.resendToken); break
 *     case 'rejected':                showError(outcome.message); break
 * }
 * ```
 *
 * **Deliberately no view.** This ships the part an app must not get wrong and no part of what it
 * should own — markup, styling, field layout, where to navigate. The framework's own rule is that it
 * ships only non-customizable things, and a login form's appearance is the first thing anyone wants
 * to change.
 *
 * HAND-WRITTEN AND CHECKED IN, next to the session it drives. Structural throughout, so the generated
 * types satisfy it without this file importing anything generated.
 */
import type { ApiResponse } from './apiResponse.ts'
import type { AuthSession, AuthSessionState, SignedIn } from './auth.ts'

/**
 * The sign-in response as the server sends it, mirroring `AuthSignInResponse`.
 *
 * The generated `AuthSignInResponse` satisfies it. [P] is the permissions type — pass the generated
 * `UserPermissions` and the outcome is typed end to end.
 */
export type SignInResult<P = unknown> =
    | ({ readonly _type: 'success' } & SignedIn<P>)
    | {
          readonly _type: 'org-selection-required'
          /** Short-lived, single-use proof the credential check passed. Feeds `selectOrg`. */
          readonly selectionToken: string
          readonly organisations: readonly OrgChoice[]
      }
    | {
          readonly _type: 'activation-required'
          /** Short-lived, single-use proof the credential check passed. Feeds `resendActivation`. */
          readonly resendToken: string
      }

/** One organisation the user may sign in to. Structural — the generated `AuthOrgRef` satisfies it. */
export interface OrgChoice {
    readonly id: string
    readonly name?: string
}

/**
 * What the caller should do next.
 *
 * A DIFFERENT union from [SignInResult]: it adds `rejected`, which the server expresses as a non-2xx
 * envelope rather than as a variant, and it renames `success` to `signed-in` because by the time a
 * caller sees this the session has already been stored — the name should say what happened, not what
 * the wire said.
 */
export type LoginOutcome<P = unknown> =
    | { readonly _type: 'signed-in'; readonly session: AuthSessionState<P> }
    | {
          readonly _type: 'org-selection-required'
          readonly selectionToken: string
          readonly organisations: readonly OrgChoice[]
      }
    | { readonly _type: 'activation-required'; readonly resendToken: string }
    | {
          readonly _type: 'rejected'
          /**
           * The server's message, or `null` when it sent none.
           *
           * **Server-authored text — render it as TEXT, never as HTML.** It is not sanitised and it
           * is not guaranteed to be free of caller input: `AuthSystem.kt` answers an unknown realm
           * with `"Realm not found: $realm"`, and `{realm}` is a path segment on a public route, so
           * an attacker can put arbitrary characters in it. Interpolating this into `v-html` or
           * `dangerouslySetInnerHTML` is reflected XSS on the one page that is about to hold a
           * session.
           *
           * It may also distinguish "no such user" from "wrong password", so treat it as an
           * account-enumeration channel and prefer your own copy on a login form.
           */
          readonly message: string | null
          readonly status: number
      }

/**
 * Awaits a sign-in call and applies its outcome to [session].
 *
 * Takes the PROMISE, so a caller writes one expression:
 * `await completeSignIn(session, client.login.signIn(...))`.
 *
 * **Only `success` touches the session.** The other two branches carry single-use tokens that grant a
 * next step and nothing else — storing them as a session would be wrong, and would make
 * `isLoggedIn` true for a user who is not.
 *
 * Non-2xx becomes `rejected` rather than throwing, matching `request`'s contract: this SDK surfaces a
 * decoded envelope instead of an exception, so a wrong password is ordinary control flow.
 */
export async function completeSignIn<P>(
    session: AuthSession<P>,
    call: Promise<ApiResponse<SignInResult<P>>>,
): Promise<LoginOutcome<P>> {
    const response = await call

    return applySignIn(session, response)
}

/** The synchronous half of [completeSignIn], for a caller that already has the envelope. */
export function applySignIn<P>(
    session: AuthSession<P>,
    response: ApiResponse<SignInResult<P>>,
): LoginOutcome<P> {
    const status = response.status.value
    const result = response.data

    // `data` can be null on a 2xx the client did not expect — a proxy, or a route that answered with
    // an empty envelope. Treating that as rejected is right: there is no session either way.
    if (status < 200 || status > 299 || result === null) {
        return { _type: 'rejected', message: firstMessage(response), status }
    }

    switch (result._type) {
        case 'success':
            session.signedIn(result)

            return { _type: 'signed-in', session: session.state() }

        case 'org-selection-required':
            return {
                _type: 'org-selection-required',
                selectionToken: result.selectionToken,
                organisations: result.organisations,
            }

        case 'activation-required':
            return { _type: 'activation-required', resendToken: result.resendToken }

        default:
            // A variant this SDK does not know — a newer server against a cached bundle, MFA being
            // the obvious next one. Without this the function returned `undefined` through a
            // signature that promises an outcome, so the documented `switch (outcome._type)` threw a
            // TypeError. In `startAutoRefresh` that throw was swallowed by its own error handler,
            // leaving the refresher silently dead and the user "randomly logged out" at expiry.
            //
            // Rejecting is the safe reading: an outcome we cannot interpret must not create a
            // session. Regenerating the SDK is what adds the branch.
            return { _type: 'rejected', message: firstMessage(response), status }
    }
}

/**
 * The first message the envelope carries, or `null`.
 *
 * Not concatenated: a login screen shows one line, and joining server messages produces something no
 * one designed. A caller wanting all of them reads `response.messages` itself.
 */
function firstMessage(response: ApiResponse<unknown>): string | null {
    const messages = response.messages

    if (messages === undefined || messages === null || messages.length === 0) return null

    return messages[0]?.text ?? null
}
