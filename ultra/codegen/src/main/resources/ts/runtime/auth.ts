/**
 * The session half of auth — the TypeScript counterpart of `AuthState`
 * (`funktor/auth/src/jsMain/kotlin/AuthState.kt`).
 *
 * Holds what the sign-in response said, and hands the transport something to attach. Deliberately
 * framework-neutral: [AuthSession.subscribe] is a plain listener so a Vue layer can wrap it in a
 * `shallowRef` without this file knowing Vue exists.
 *
 * **Nothing here decodes a JWT.** The response states the permissions, the expiry and the user id
 * outright, so no client has to read a token. The Kotlin client deleted its own decoder for the same
 * reason — a client that parses a credential it only needs to forward is reading a format it does not
 * own, and it breaks the moment the server changes claims.
 *
 * HAND-WRITTEN AND CHECKED IN. It lives in `ultra:codegen` rather than beside the auth feature
 * because nothing in it is funktor-specific — a session, a storage strategy, a transport wrapper —
 * and because `ts-verify` cannot see another module's resources, so shipping it from
 * `funktor:codegen` would mean shipping TypeScript that nothing type-checks or executes.
 * `AuthTsContributor` still decides WHEN it ships.
 */
import type { HttpRequest, HttpResponse, HttpTransport } from './http.ts'

//  What the server said  //////////////////////////////////////////////////////////////////////////

/**
 * How the session is carried, mirroring `AuthSignInResponse.Session`.
 *
 * Structural, so the generated `AuthSignInResponseSession` satisfies it without an import — the
 * runtime must not depend on generated output, which only exists in an SDK that reached the auth
 * feature.
 *
 * **A one-member union on purpose.** Cookie mode was designed and dropped (2026-08-02: b2b2c
 * frontends run on customer-controlled domains, a different *site*, which forces `SameSite=None` and
 * loses the protection that motivated it). The server kept `Session` sealed so the discriminator
 * stays on the wire, which makes a second transport an ADDITIVE change here rather than a reshape of
 * every consumer.
 */
export type SessionCarrier = { readonly _type: 'bearer'; readonly token: string }

/**
 * The part of a successful sign-in this module needs.
 *
 * The generated `AuthSignInResponseSuccess` satisfies it. [P] is the permissions type — supply the
 * generated `UserPermissions` and the session is typed end to end, without this file importing
 * anything generated.
 */
export interface SignedIn<P = unknown> {
    readonly session: SessionCarrier
    /**
     * REQUIRED, because it is required on the wire (`Success.permissions` is non-null).
     *
     * The optionality here is what makes the parity assertion in a consuming app bite: with `?`, a
     * generated type that had lost `permissions` entirely would still satisfy this interface, and
     * the check that exists to catch exactly that kind of server change would pass. `expiresAt` and
     * `userId` below are genuinely `nullable().optional()` on the wire, so they stay optional — the
     * difference between the two is the whole strength of the check.
     */
    readonly permissions: P
    /**
     * `MpInstant`, so the epoch-millis number is `expiresAt.ts` — NOT `expiresAt` itself.
     *
     * Nullable because `exp` is optional in RFC 7519 and the verifier treats an absent one as "no
     * expiry check". Null means the session states no expiry.
     */
    readonly expiresAt?: { readonly ts: number } | null
    readonly userId?: string | null
}

//  Storage  ///////////////////////////////////////////////////////////////////////////////////////

/**
 * Where the session is kept between page loads.
 *
 * A SESSION store, not a token store — it persists the whole sign-in payload. Since nothing decodes
 * the JWT any more, a token-shaped store would lose `permissions`, `expiresAt` and `userId` on every
 * reload: the session would come back unable to say what the user may do and unable to schedule its
 * own refresh, and would then die at expiry looking like a random logout.
 */
export interface SessionStorage {
    read(): string | null
    write(serialized: string): void
    clear(): void
}

/**
 * `localStorage`, mirroring what `AuthState` does today.
 *
 * **A known, accepted weakness — and the settled design, not a placeholder.** Any script on the
 * origin can read this and replay it. The httpOnly-cookie alternative was designed in full and
 * dropped (2026-08-02): b2b2c frontends run on customer-controlled domains, which are a different
 * *site*, so it would have forced `SameSite=None` and lost its own main protection exactly where it
 * was wanted. Reasoning in `.claude/tasks-archive/2026-07/20260719-token-storage-hardening.md`.
 *
 * So the mitigations are the ones that actually apply to XSS — CSP and Trusted Types, a short token
 * TTL, session revocation — rather than a different container. Storage stays injected because tests
 * need [inMemorySession], not because a swap is pending.
 *
 * Degrades to in-memory when `localStorage` throws — Safari private mode and SSR both do — because a
 * login that cannot persist is far better than one that cannot happen.
 */
export function localStorageSession(key: string = 'funktor.auth.session'): SessionStorage {
    const fallback = inMemorySession()

    const store = (): Storage | null => {
        try {
            // Touched, not merely read: some environments expose the object and throw on ACCESS.
            const s = globalThis.localStorage
            const probe = '__funktor_probe__'
            s.setItem(probe, '1')
            s.removeItem(probe)
            return s
        } catch {
            return null
        }
    }

    return {
        read: () => {
            const s = store()
            if (s === null) return fallback.read()
            try {
                return s.getItem(key)
            } catch {
                return fallback.read()
            }
        },
        write: (serialized) => {
            const s = store()
            if (s === null) return fallback.write(serialized)
            try {
                s.setItem(key, serialized)
            } catch {
                fallback.write(serialized)
            }
        },
        clear: () => {
            const s = store()
            fallback.clear()
            if (s === null) return
            try {
                s.removeItem(key)
            } catch {
                /* already cleared from the fallback */
            }
        },
    }
}

/** Keeps the session in a closure. Lost on reload — what tests use, and the fallback when storage throws. */
export function inMemorySession(): SessionStorage {
    let value: string | null = null

    return {
        read: () => value,
        write: (serialized) => {
            value = serialized
        },
        clear: () => {
            value = null
        },
    }
}

//  Session  ///////////////////////////////////////////////////////////////////////////////////////

/** What a view needs to render. Immutable — a new object is published on every change. */
export interface AuthSessionState<P = unknown> {
    readonly carrier: SessionCarrier | null
    /** The bearer token, or `null` when logged out. */
    readonly token: string | null
    readonly permissions: P | null
    /** Epoch milliseconds, or `null` when the session states no expiry. */
    readonly expiresAt: number | null
    readonly userId: string | null
    readonly isLoggedIn: boolean
}

/** Cancels a [AuthSession.subscribe]. */
export type Unsubscribe = () => void

const EMPTY: AuthSessionState<never> = {
    carrier: null,
    token: null,
    permissions: null,
    expiresAt: null,
    userId: null,
    isLoggedIn: false,
}

function empty<P>(): AuthSessionState<P> {
    return EMPTY as AuthSessionState<P>
}

/**
 * The current session, and the only thing that writes storage.
 *
 * Restores from storage on construction, so a reload keeps the user logged in.
 */
export class AuthSession<P = unknown> {
    private readonly storage: SessionStorage
    private readonly listeners = new Set<(state: AuthSessionState<P>) => void>()
    private current: AuthSessionState<P>
    private gen = 0

    constructor(storage: SessionStorage = localStorageSession()) {
        this.storage = storage
        this.current = restore<P>(storage.read())
    }

    /** The current state. */
    readonly state = (): AuthSessionState<P> => this.current

    /**
     * Bumped by every [signedIn] and [signOut]. Compare it to decide whether an ASYNC result is
     * still relevant.
     *
     * The problem it solves: a request issued against one session can land after that session ended,
     * and applying it then is not a stale render, it is a security bug. An auto-refresh in flight
     * when the user signs out used to resurrect the session and write a fresh full-TTL token back to
     * storage — on a machine the user believed was logged out. Worse, if another user signed in
     * meanwhile, the first user's token and permissions landed in the second user's session.
     *
     * `isLoggedIn` cannot express this: it is true again after the second sign-in, so it says
     * "someone is logged in" when the question is "is this still the SAME session".
     *
     * ```ts
     * const gen = session.generation()
     * const result = await call()
     * if (session.generation() !== gen) return   // superseded — drop it
     * ```
     */
    readonly generation = (): number => this.gen

    /**
     * Registers [listener] and returns its canceller.
     *
     * Called IMMEDIATELY with the current state, so a subscriber never renders a stale first frame —
     * the single most common bug when wiring a store into a component.
     */
    readonly subscribe = (listener: (state: AuthSessionState<P>) => void): Unsubscribe => {
        this.listeners.add(listener)
        listener(this.current)

        return () => {
            this.listeners.delete(listener)
        }
    }

    /**
     * Records a successful sign-in — or a refresh, which returns the same payload.
     *
     * Takes the RESPONSE, not a token: everything worth keeping is stated by the server rather than
     * dug out of a JWT, so narrowing this to a token string would discard `permissions`, `expiresAt`
     * and `userId` at the door.
     *
     * **A payload without a usable token is REFUSED**, and signs out rather than half-succeeding.
     * [restore] has always refused one; this did not, so the same malformed carrier was rejected
     * coming out of storage and accepted coming off the wire. The asymmetry showed up as a session
     * that worked until the first reload.
     */
    readonly signedIn = (payload: SignedIn<P>): void => {
        if (!isUsableCarrier(payload.session)) {
            this.signOut()
            return
        }

        this.gen++
        this.storage.write(JSON.stringify(payload))
        this.publish(stateOf<P>(payload))
    }

    /**
     * Drops the session LOCALLY.
     *
     * The token itself stays valid server-side until it expires — there is no revocation endpoint
     * today. So this ends the session on this device and nowhere else, and a copy taken beforehand
     * still works. Shortening the TTL is the mitigation; revocation is tracked in
     * `.claude/tasks-archive/2026-07/20260719-token-storage-hardening.md`.
     */
    readonly signOut = (): void => {
        this.gen++
        this.storage.clear()
        this.publish(empty<P>())
    }

    /**
     * True when there is a session and it expires within [withinMs].
     *
     * A session with no expiry returns FALSE. That is load-bearing rather than defensive: `exp` is
     * optional in RFC 7519, so `expiresAt` is genuinely absent sometimes, and answering true would
     * refresh on every call forever.
     */
    readonly isExpiring = (withinMs: number, now: number = Date.now()): boolean => {
        const { expiresAt, isLoggedIn } = this.current

        return isLoggedIn && expiresAt !== null && expiresAt - now <= withinMs
    }

    private publish(next: AuthSessionState<P>): void {
        this.current = next
        // Over a COPY: a listener that unsubscribes itself while being notified would otherwise
        // mutate the set mid-iteration.
        for (const listener of [...this.listeners]) listener(next)
    }
}

function stateOf<P>(payload: SignedIn<P>): AuthSessionState<P> {
    return {
        carrier: payload.session,
        token: payload.session.token,
        permissions: payload.permissions ?? null,
        expiresAt: payload.expiresAt?.ts ?? null,
        userId: payload.userId ?? null,
        isLoggedIn: true,
    }
}

/**
 * Whether [carrier] names a transport this client speaks AND carries what that transport needs.
 *
 * Both halves matter, and the token half is the subtle one: a carrier that declares `bearer` with no
 * token string produces `isLoggedIn: true` with nothing to send, so every request goes out anonymous
 * while the UI shows a signed-in user. That does not fail — it just quietly does not authenticate.
 *
 * Shared by [AuthSession.signedIn] and [restore] so the wire and storage are held to one standard.
 */
function isUsableCarrier(carrier: unknown): boolean {
    if (typeof carrier !== 'object' || carrier === null) return false

    // Cookie mode was dropped, so a payload naming it is either an older SDK's or hand-written.
    if ((carrier as { _type?: unknown })._type !== 'bearer') return false

    return typeof (carrier as { token?: unknown }).token === 'string'
}

/**
 * Rebuilds the session from what was persisted.
 *
 * Anything unreadable restores to logged-out rather than throwing: a truncated `localStorage` value
 * and a payload from an older SDK are both ordinary, and throwing here would take down a page render.
 *
 * **An ALREADY-EXPIRED session restores as logged out**, matching the Kotlin `AuthState`
 * (`funktor/auth/src/jsMain/kotlin/AuthState.kt`). Not defensive tidiness — a dead token cannot be
 * refreshed, because the refresh endpoint needs a live one. Restoring it would leave a shell that
 * renders the stored `permissions` as though the user still held them while every call 401s, and
 * `startAutoRefresh` would retry every 30s for as long as the tab stayed open, forever. The honest
 * state is logged out, which the app already knows how to render.
 */
function restore<P>(serialized: string | null, now: number = Date.now()): AuthSessionState<P> {
    if (serialized === null || serialized.length === 0) return empty<P>()

    try {
        const parsed: unknown = JSON.parse(serialized)

        if (typeof parsed !== 'object' || parsed === null) return empty<P>()

        if (!isUsableCarrier((parsed as { session?: unknown }).session)) return empty<P>()

        const state = stateOf<P>(parsed as SignedIn<P>)

        // `null` means the session states NO expiry, which is not the same as expired — `exp` is
        // optional in RFC 7519 and the verifier treats an absent one as "no expiry check".
        if (state.expiresAt !== null && state.expiresAt <= now) return empty<P>()

        return state
    } catch {
        return empty<P>()
    }
}

//  Transport  /////////////////////////////////////////////////////////////////////////////////////

/**
 * Wraps [inner] so every request carries the session.
 *
 * **Auth is a transport wrapper, not an `SdkConfig` field** — the idiom `runtime/http.ts` documents,
 * and the same shape the Kotlin client uses via a Ktor `defaultRequest` closure.
 *
 * ```ts
 * const session = new AuthSession<UserPermissions>()
 * const config = sdkConfig('https://api.example.com', authTransport(fetchTransport(), session))
 * ```
 *
 * Attaches `Authorization: Bearer <token>`. **An explicit header on the request always wins**, so a
 * caller can still do something special for one call.
 *
 * Attaching on PUBLIC routes too is safe and deliberate: funktor degrades an unverifiable token to an
 * anonymous caller rather than rejecting it — `tryJwtCaller` returns null on a failed verify and Ktor
 * falls through to the terminal `anonymous` provider — so a stale token on `signIn` behaves exactly
 * as if none were sent.
 */
export function authTransport<P>(inner: HttpTransport, session: AuthSession<P>): HttpTransport {
    return {
        send: (request: HttpRequest): Promise<HttpResponse> => {
            // Read PER REQUEST, not at wrap time: a transport built before login must still
            // authenticate afterwards, and a sign-out must take effect immediately.
            const { carrier, token } = session.state()

            if (carrier === null) return inner.send(request)

            // Case-INSENSITIVE: HTTP header names are, and `fetch` merges a differently-cased
            // duplicate into one comma-joined value rather than letting the caller's win. A
            // decorator that set `authorization` used to get `Bearer explicit, Bearer <session>`,
            // which is not a credential the server can parse — so the caller's override silently
            // became a malformed header instead of an override.
            const hasAuthHeader = Object.keys(request.headers)
                .some((name) => name.toLowerCase() === 'authorization')

            if (token === null || hasAuthHeader) return inner.send(request)

            return inner.send({
                ...request,
                headers: { ...request.headers, Authorization: `Bearer ${token}` },
            })
        },
    }
}
