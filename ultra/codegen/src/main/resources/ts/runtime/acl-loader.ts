/**
 * Loading the access matrix, and the three states a view has to render.
 *
 * The matrix is fetched separately rather than carried in the sign-in response: it is one entry per
 * non-denied route — thousands of bytes for a privileged account — and it would land in
 * `localStorage` alongside the session, which is the wrong place for it. So it is re-fetched per tab
 * rather than persisted, and that makes the LOADING state real rather than theoretical.
 *
 * The state a view must not skip is the middle one. Rendering `denied` while the matrix is in flight
 * makes controls appear one by one as it lands, which reads as broken.
 *
 * ```ts
 * const acl = new AclLoader(session, () => client.login.getMyApiAccess())
 * acl.subscribe((s) => { ... })   // 'absent' | 'loading' | 'ready'
 * ```
 */
import type { ApiResponse } from './apiResponse.ts'
import type { AuthSession, Unsubscribe } from './auth.ts'
import { ApiAcl } from './acl.ts'
import type { AccessMatrix } from './acl.ts'

/** What a view renders. `ready` is the only state with an [ApiAcl]. */
export type AclState =
    /** No session, or the session ended. Nothing to load. */
    | { readonly _type: 'absent' }
    /**
     * A session exists and the matrix is in flight.
     *
     * **Withhold permission-gated controls; do not render them denied.** [stale] carries a previous
     * matrix when this is a re-load, so a refresh does not blank the UI.
     */
    | { readonly _type: 'loading'; readonly stale: ApiAcl | null }
    | { readonly _type: 'ready'; readonly acl: ApiAcl }

/** Knobs for [AclLoader]. */
export interface AclLoaderOptions {
    /** Attempts per load, including the first. Default 3. */
    readonly attempts?: number
    /** Backoff before each retry, in milliseconds. Default 200 / 1000 / 3000. */
    readonly backoffMs?: readonly number[]
    readonly setTimer?: (fn: () => void, ms: number) => unknown
    /**
     * Called when a load gives up with NOTHING to fall back on.
     *
     * That is the only fatal case: no matrix at all and no way to get one leaves a session that
     * cannot render anything, so the honest move is to end it. Wire this to `session.signOut()`.
     * A load that fails while a previous matrix exists keeps the stale one and never calls this —
     * permissions changing AND the fetch failing at the same moment is rare enough that logging the
     * user out would cost more than the staleness.
     */
    readonly onUnavailable?: (reason: 'rejected' | 'exhausted') => void
}

const ABSENT: AclState = { _type: 'absent' }

/**
 * Keeps an [ApiAcl] in step with a session.
 *
 * Load whenever there is a session and no matrix. "After login" and "a new tab opened" are both
 * instances of that, so the invariant is the thing to implement rather than the event list.
 *
 * **Call [load] after a refresh too.** A refresh mints a new token and restates `permissions`, so a
 * matrix from before it is a second, disagreeing source of truth for the same fact. The matrix
 * belongs to the current token.
 */
export class AclLoader<P> {
    private readonly session: AuthSession<P>
    private readonly fetchMatrix: () => Promise<ApiResponse<AccessMatrix>>
    private readonly attempts: number
    private readonly backoffMs: readonly number[]
    private readonly setTimer: (fn: () => void, ms: number) => unknown
    private readonly onUnavailable?: (reason: 'rejected' | 'exhausted') => void

    private readonly listeners = new Set<(state: AclState) => void>()
    private current: AclState = ABSENT
    private inFlight = false
    /**
     * Bumped by [load] and [clear]; captured per attempt and re-checked before publishing.
     *
     * A fetch cannot be cancelled, so the only way to stop a superseded one from landing is to
     * recognise it when it does.
     */
    private generation = 0

    constructor(
        session: AuthSession<P>,
        fetchMatrix: () => Promise<ApiResponse<AccessMatrix>>,
        options: AclLoaderOptions = {},
    ) {
        this.session = session
        this.fetchMatrix = fetchMatrix
        this.attempts = options.attempts ?? 3
        this.backoffMs = options.backoffMs ?? [200, 1_000, 3_000]
        this.setTimer = options.setTimer ?? ((fn, ms) => setTimeout(fn, ms))
        this.onUnavailable = options.onUnavailable
    }

    readonly state = (): AclState => this.current

    /** The current matrix, or `ApiAcl.empty` — which denies everything, including routes you may hold. */
    readonly acl = (): ApiAcl =>
        this.current._type === 'ready' ? this.current.acl : ApiAcl.empty

    /** Fires immediately with the current state, so a subscriber never renders a stale first frame. */
    readonly subscribe = (listener: (state: AclState) => void): Unsubscribe => {
        this.listeners.add(listener)
        listener(this.current)

        return () => {
            this.listeners.delete(listener)
        }
    }

    /**
     * Drops the matrix. Call on sign-out — a matrix outliving its session is a stale grant.
     *
     * **Also abandons any load in flight.** It used to only publish `absent`, which left two holes
     * that the next user fell into: the pending fetch republished `ready` with the departed user's
     * matrix, and `inFlight` stayed true so the next `load()` was silently dropped and never
     * re-armed. The second user's UI was then gated by the first user's permissions — permanently,
     * and the matrix itself is withheld data, since the server omits `Denied` rows precisely so it
     * does not disclose an API surface the caller cannot reach.
     */
    readonly clear = (): void => {
        this.generation++
        this.inFlight = false
        this.publish(ABSENT)
    }

    /**
     * Loads the matrix if there is a session.
     *
     * Idempotent while in flight: a second call is ignored rather than queued, so mounting two
     * components does not fetch twice.
     */
    readonly load = (): void => {
        if (this.inFlight) return

        if (!this.session.state().isLoggedIn) {
            this.publish(ABSENT)
            return
        }

        this.inFlight = true
        this.publish({ _type: 'loading', stale: this.readyAcl() })
        this.attempt(0, ++this.generation)
    }

    private attempt(index: number, generation: number): void {
        // TWO-ARGUMENT `then`, not `.then().catch()`. With a trailing `catch`, an exception thrown by
        // a SUBSCRIBER inside `publish` was caught here and treated as a failed fetch: a component
        // whose render handler threw sent the loader into its full retry schedule and then into
        // `onUnavailable('exhausted')`, which apps are told to wire to `signOut()`. A render bug
        // became three extra requests and a forced logout. This form catches only the fetch.
        void this.fetchMatrix().then(
            (response) => {
                if (generation !== this.generation) return

                const status = response.status.value

                if (status === 401 || status === 403) {
                    // The session is gone. Retrying only delays the inevitable, so do not.
                    return this.giveUp('rejected')
                }

                if (status < 200 || status > 299 || response.data === null) {
                    return this.retryOrGiveUp(index, generation)
                }

                this.inFlight = false
                this.publish({ _type: 'ready', acl: new ApiAcl(response.data) })
            },
            () => {
                // A transport error is not a session decision — the network may simply be down.
                if (generation !== this.generation) return

                this.retryOrGiveUp(index, generation)
            },
        )
    }

    private retryOrGiveUp(index: number, generation: number): void {
        const next = index + 1

        if (next >= this.attempts) return this.giveUp('exhausted')

        const delay = this.backoffMs[Math.min(index, this.backoffMs.length - 1)] ?? 0

        this.setTimer(() => {
            if (generation !== this.generation) return

            this.attempt(next, generation)
        }, delay)
    }

    /**
     * Ends the load.
     *
     * **Keeps a previous matrix if there is one** — the maintainer's rule: sign out only when there
     * is nothing to fall back on. A stale matrix is a far better answer than an empty one, because
     * empty denies everything.
     */
    private giveUp(reason: 'rejected' | 'exhausted'): void {
        this.inFlight = false

        const stale = this.staleAcl()

        if (stale !== null) {
            this.publish({ _type: 'ready', acl: stale })
            return
        }

        this.publish(ABSENT)
        this.onUnavailable?.(reason)
    }

    private readyAcl(): ApiAcl | null {
        return this.current._type === 'ready' ? this.current.acl : null
    }

    private staleAcl(): ApiAcl | null {
        if (this.current._type === 'ready') return this.current.acl
        if (this.current._type === 'loading') return this.current.stale

        return null
    }

    private publish(next: AclState): void {
        this.current = next
        // Over a COPY: a listener unsubscribing itself while notified would mutate the set mid-iteration.
        for (const listener of [...this.listeners]) listener(next)
    }
}
