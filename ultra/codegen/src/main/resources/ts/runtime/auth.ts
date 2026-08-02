/**
 * The session half of auth — the TypeScript counterpart of `AuthState`
 * (`funktor/auth/src/jsMain/kotlin/AuthState.kt`).
 *
 * Holds what the sign-in response said, and hands the transport something to attach. Deliberately
 * framework-neutral: [AuthSession.subscribe] is a plain listener so a Vue layer can wrap it in a
 * `shallowRef` without this file knowing Vue exists.
 *
 * **Nothing here decodes a JWT.** The response states the permissions, the expiry and the user id
 * outright, so no client has to read a token — which is what makes the same code work when the token
 * is an httpOnly cookie the browser will not let JavaScript see. The Kotlin client dropped its own
 * decoder for the same reason.
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
 * Sealed on the server precisely so the token's EXISTENCE is tied to the transport: in cookie mode
 * there is no token in the response, and that must not type-check.
 */
export type SessionCarrier =
    | { readonly _type: 'bearer'; readonly token: string }
    | { readonly _type: 'cookie' }

/**
 * The part of a successful sign-in this module needs.
 *
 * The generated `AuthSignInResponseSuccess` satisfies it. [P] is the permissions type — supply the
 * generated `UserPermissions` and the session is typed end to end, without this file importing
 * anything generated.
 */
export interface SignedIn<P = unknown> {
    readonly session: SessionCarrier
    readonly permissions?: P
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
 * A SESSION store, not a token store. In cookie mode there is no token to keep — the browser holds it
 * and JavaScript cannot read it — so anything shaped around a token string cannot express the state
 * that actually needs persisting.
 */
export interface SessionStorage {
    read(): string | null
    write(serialized: string): void
    clear(): void
}

/**
 * `localStorage`, mirroring what `AuthState` does today.
 *
 * **A known, accepted weakness, not an oversight.** Any script on the origin can read this and replay
 * it — tracked as `.claude/tasks/20260719-token-storage-hardening.md`. It is the default ON PURPOSE:
 * the Kotlin and TypeScript clients deliberately share the same behaviour so the fix is designed once
 * and lands in both, rather than one client looking solved while the other is not. Storage is
 * injected precisely so that fix is a changed default rather than a rewrite.
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

/** Keeps the session in a closure. Lost on reload — what tests use, and the eventual hardened default. */
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
    /** The bearer token, or `null` in cookie mode AND when logged out. Never assume it exists. */
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
 * Restores from storage on construction, so a reload keeps the user logged in. Cookie mode has
 * nothing to restore — the browser holds the credential and JS cannot see it — so an app in that mode
 * must re-establish state on boot by calling the refresh endpoint, which returns the same payload
 * [signedIn] takes.
 */
export class AuthSession<P = unknown> {
    private readonly storage: SessionStorage
    private readonly listeners = new Set<(state: AuthSessionState<P>) => void>()
    private current: AuthSessionState<P>

    constructor(storage: SessionStorage = localStorageSession()) {
        this.storage = storage
        this.current = restore<P>(storage.read())
    }

    /** The current state. */
    readonly state = (): AuthSessionState<P> => this.current

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
     * Takes the RESPONSE, not a token: in cookie mode there is no token, and everything worth keeping
     * is stated by the server rather than dug out of a JWT.
     */
    readonly signedIn = (payload: SignedIn<P>): void => {
        this.storage.write(JSON.stringify(payload))
        this.publish(stateOf<P>(payload))
    }

    /** Drops the session locally. A bearer JWT stays valid until it expires; a cookie needs the server. */
    readonly signOut = (): void => {
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
        token: payload.session._type === 'bearer' ? payload.session.token : null,
        permissions: payload.permissions ?? null,
        expiresAt: payload.expiresAt?.ts ?? null,
        userId: payload.userId ?? null,
        isLoggedIn: true,
    }
}

/**
 * Rebuilds the session from what was persisted.
 *
 * Anything unreadable restores to logged-out rather than throwing: a truncated `localStorage` value
 * and a payload from an older SDK are both ordinary, and throwing here would take down a page render.
 */
function restore<P>(serialized: string | null): AuthSessionState<P> {
    if (serialized === null || serialized.length === 0) return empty<P>()

    try {
        const parsed: unknown = JSON.parse(serialized)

        if (typeof parsed !== 'object' || parsed === null) return empty<P>()

        const session: unknown = (parsed as { session?: unknown }).session

        if (typeof session !== 'object' || session === null) return empty<P>()

        const kind: unknown = (session as { _type?: unknown })._type

        if (kind !== 'bearer' && kind !== 'cookie') return empty<P>()

        return stateOf<P>(parsed as SignedIn<P>)
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
 * Bearer mode attaches `Authorization`. **Cookie mode attaches nothing and permits the cookie to
 * travel instead** — the browser holds the credential, and setting `credentials` is the only thing
 * the client can do. An explicit value on the request always wins either way, so a caller can still
 * do something special for one call.
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

            if (carrier._type === 'cookie') {
                return inner.send({ ...request, credentials: request.credentials ?? 'include' })
            }

            if (token === null || 'Authorization' in request.headers) return inner.send(request)

            return inner.send({
                ...request,
                headers: { ...request.headers, Authorization: `Bearer ${token}` },
            })
        },
    }
}
