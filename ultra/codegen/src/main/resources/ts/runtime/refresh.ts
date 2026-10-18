/**
 * Keeps a session alive — the piece that makes `AuthSession.isExpiring` do something.
 *
 * Without it a session simply dies: the token expires, every later call 401s, and the user is
 * "randomly logged out" with no error that names the cause. That is the failure the whole
 * `expiresAt` contract exists to prevent, and nothing was acting on it.
 *
 * ```ts
 * const stop = startAutoRefresh(session, () => client.login.refreshToken({ realm }))
 * ```
 *
 * Timers are INJECTABLE so this is testable without waiting in real time, and so a caller can drive
 * it from a framework's own scheduler.
 */
import type { ApiResponse } from './apiResponse.ts'
import type { AuthSession } from './auth.ts'
import type { LoginOutcome, SignInResult } from './login.ts'
import { applySignIn } from './login.ts'

/** Knobs for [startAutoRefresh]. Every default is a plain number so a caller can reason about it. */
export interface AutoRefreshOptions<P = unknown> {
    /** Refresh once the session is this close to expiring. Default 60s. */
    readonly leadMs?: number
    /** How often to look. Default 30s — half the lead, so a check cannot be skipped past. */
    readonly checkEveryMs?: number
    readonly now?: () => number
    readonly setTimer?: (fn: () => void, ms: number) => unknown
    readonly clearTimer?: (handle: unknown) => void
    /**
     * Called when a refresh comes back rejected.
     *
     * **Nothing is signed out automatically.** Whether a failed refresh should drop the session,
     * redirect, or show a banner is application policy, and guessing it here would take a decision
     * away from the app at the worst possible moment. The session is left exactly as it was.
     */
    readonly onFailed?: (outcome: LoginOutcome<P>) => void
}

/**
 * Starts checking [session] and refreshing it before it expires. Returns a function that stops.
 *
 * **A session with no expiry is never refreshed**, and that is correct rather than a gap: `exp` is
 * optional in RFC 7519 and the verifier treats an absent one as "no expiry check", so such a token
 * does not expire server-side either. Note this differs from the Kotlin `AuthState`, which treats
 * unknown expiry as STALE and clears the session — it had to, because it learned the expiry by
 * decoding the token, so "no expiry" and "could not decode" were indistinguishable and a session in
 * that state could never self-heal. The response states the expiry now, so the ambiguity is gone.
 *
 * Refreshes do not overlap: a check that arrives while one is in flight is skipped rather than
 * queued, because two refreshes racing would have one of them write a stale session last.
 */
export function startAutoRefresh<P>(
    session: AuthSession<P>,
    refresh: () => Promise<ApiResponse<SignInResult<P>>>,
    options: AutoRefreshOptions<P> = {},
): () => void {
    const leadMs = options.leadMs ?? 60_000
    const checkEveryMs = options.checkEveryMs ?? 30_000
    const now = options.now ?? (() => Date.now())
    const setTimer = options.setTimer ?? ((fn, ms) => setTimeout(fn, ms))
    const clearTimer = options.clearTimer ?? ((handle) => clearTimeout(handle as never))

    let handle: unknown = null
    let stopped = false
    let inFlight = false

    const tick = (): void => {
        if (stopped) return

        // Re-armed BEFORE the async work, so a refresh that hangs cannot stop the schedule.
        handle = setTimer(tick, checkEveryMs)

        if (inFlight || !session.isExpiring(leadMs, now())) return

        inFlight = true

        // Captured BEFORE the request goes out, and re-checked when it lands.
        const generation = session.generation()

        void refresh()
            .then((response) => {
                // `stopped` is not enough. Signing out does not stop the scheduler — they are
                // separate lifecycles, and nothing obliges an app to call `stop()` from its sign-out
                // handler. Without the generation check, a refresh already on the wire when the user
                // signed out applied anyway: the session came back, and a fresh full-TTL token was
                // written to `localStorage` on a machine the user had just logged out of. If a
                // DIFFERENT user signed in during the round trip, they got the first user's token,
                // permissions and id.
                if (stopped || session.generation() !== generation) return

                const outcome = applySignIn(session, response)

                if (outcome._type !== 'signed-in') options.onFailed?.(outcome)
            })
            .catch(() => {
                // A transport error is not a session decision — the network may simply be down, and
                // dropping the session for a failed fetch would log people out on a flaky connection.
                // The next tick tries again.
            })
            .finally(() => {
                inFlight = false
            })
    }

    handle = setTimer(tick, checkEveryMs)

    return () => {
        stopped = true

        if (handle !== null) clearTimer(handle)

        handle = null
    }
}
