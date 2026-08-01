/**
 * The session half of auth — the TypeScript counterpart of `AuthState`
 * (`funktor/auth/src/jsMain/kotlin/AuthState.kt`).
 *
 * Holds the JWT, decodes it for display, and hands the transport something to attach. Deliberately
 * framework-neutral: [AuthSession.subscribe] is a plain listener so a Vue layer can wrap it in a
 * `shallowRef` without this file knowing Vue exists.
 *
 * HAND-WRITTEN AND CHECKED IN. It lives in `ultra:codegen` rather than beside the auth feature
 * because NOTHING here is funktor-specific — a bearer token, a storage strategy, a transport wrapper.
 * The funktor-specific part of login is the realm/provider flow, and that is GENERATED.
 *
 * The decisive reason, though, is verification: `ts-verify` lives in this module and cannot see
 * another module's resources, so shipping it from `funktor:codegen` would mean shipping TypeScript
 * that nothing type-checks or executes. `AuthTsContributor` still decides WHEN it ships.
 */
import type { HttpRequest, HttpResponse, HttpTransport } from './http.ts'

//  Storage  ///////////////////////////////////////////////////////////////////////////////////////

/** Where the token is kept between page loads. */
export interface TokenStorage {
    read(): string | null
    write(token: string): void
    clear(): void
}

/**
 * `localStorage`, mirroring what `AuthState` does today.
 *
 * **This is a known, accepted weakness, not an oversight.** Any script on the origin can read the
 * token and replay it elsewhere — tracked as `.claude/tasks/20260719-token-storage-hardening.md`.
 * It is the default here ON PURPOSE: the Kotlin and TypeScript clients deliberately share the same
 * behaviour so the fix is designed once and lands in both, rather than one client looking solved
 * while the other is not.
 *
 * Storage is injected precisely so that fix is a changed default rather than a rewrite.
 *
 * Degrades to in-memory when `localStorage` throws — Safari private mode and SSR both do — because a
 * login that cannot persist is far better than one that cannot happen.
 */
export function localStorageTokens(key: string = 'funktor.auth.token'): TokenStorage {
    const fallback = inMemoryTokens()

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
        write: (token) => {
            const s = store()
            if (s === null) return fallback.write(token)
            try {
                s.setItem(key, token)
            } catch {
                fallback.write(token)
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

/** Keeps the token in a closure. Lost on reload — what tests use, and the eventual hardened default. */
export function inMemoryTokens(): TokenStorage {
    let token: string | null = null

    return {
        read: () => token,
        write: (value) => {
            token = value
        },
        clear: () => {
            token = null
        },
    }
}

//  Claims  ////////////////////////////////////////////////////////////////////////////////////////

/**
 * A JWT's payload, decoded but **NOT VERIFIED**.
 *
 * A client cannot verify a signature — it has no key, and shipping one would be worse than not
 * checking. So treat every field here as **display data only**: render a name, decide when to
 * refresh, grey out a menu. Never make an authorization decision from it. The server re-derives
 * everything from the token it verifies itself, and it is the only authority.
 *
 * Anyone can hand-edit a JWT payload and this function will happily decode it.
 */
export type JwtClaims = Readonly<Record<string, unknown>>

/**
 * Decodes a JWT's payload segment, or `null` when it is not a readable JWT.
 *
 * Never throws: a malformed token is an ordinary state (a truncated `localStorage` value, a token
 * from an older server), and throwing here would take down a page render.
 */
export function decodeJwtClaims(token: string): JwtClaims | null {
    const segments = token.split('.')
    if (segments.length !== 3) return null

    const payload = segments[1]
    if (payload === undefined || payload.length === 0) return null

    try {
        // base64url -> base64, then pad to a multiple of 4. `atob` rejects both otherwise.
        const base64 = payload.replace(/-/g, '+').replace(/_/g, '/')
        const padded = base64 + '='.repeat((4 - (base64.length % 4)) % 4)

        // Round-trip through percent-encoding so multi-byte UTF-8 survives; `atob` yields latin1.
        const json = decodeURIComponent(
            atob(padded)
                .split('')
                .map((c) => '%' + c.charCodeAt(0).toString(16).padStart(2, '0'))
                .join(''),
        )

        const parsed: unknown = JSON.parse(json)

        return typeof parsed === 'object' && parsed !== null && !Array.isArray(parsed)
            ? (parsed as JwtClaims)
            : null
    } catch {
        return null
    }
}

/** The `exp` claim as epoch MILLISECONDS, or `null` when absent or not a number. JWT `exp` is seconds. */
export function expiryOf(claims: JwtClaims | null): number | null {
    const exp = claims?.['exp']

    return typeof exp === 'number' && Number.isFinite(exp) ? exp * 1000 : null
}

//  Session  ///////////////////////////////////////////////////////////////////////////////////////

/** What a view needs to render. Immutable — a new object is published on every change. */
export interface AuthSessionState {
    readonly token: string | null
    /** Decoded, UNVERIFIED. Display only — see [JwtClaims]. */
    readonly claims: JwtClaims | null
    /** Epoch milliseconds, or `null` when the token carries no usable `exp`. */
    readonly expiresAt: number | null
    readonly isLoggedIn: boolean
}

/** Cancels a [AuthSession.subscribe]. */
export type Unsubscribe = () => void

/**
 * The current session, and the only thing that writes token storage.
 *
 * Restores from storage on construction, so a reload keeps the user logged in.
 */
export class AuthSession {
    private readonly storage: TokenStorage
    private readonly listeners = new Set<(state: AuthSessionState) => void>()
    private current: AuthSessionState

    constructor(storage: TokenStorage = localStorageTokens()) {
        this.storage = storage
        this.current = stateOf(storage.read())
    }

    /** The current state. Cheap — nothing is decoded here, only on change. */
    readonly state = (): AuthSessionState => this.current

    /**
     * Registers [listener] and returns its canceller.
     *
     * Called IMMEDIATELY with the current state, so a subscriber never renders a stale first frame
     * — the single most common bug when wiring a store into a component.
     */
    readonly subscribe = (listener: (state: AuthSessionState) => void): Unsubscribe => {
        this.listeners.add(listener)
        listener(this.current)

        return () => {
            this.listeners.delete(listener)
        }
    }

    /** Records a successful sign-in. Pass `AuthSignInResponseToken.token`. */
    readonly signedIn = (token: string): void => {
        this.storage.write(token)
        this.publish(stateOf(token))
    }

    /** Drops the session locally. The server is not told — a JWT stays valid until it expires. */
    readonly signOut = (): void => {
        this.storage.clear()
        this.publish(stateOf(null))
    }

    /**
     * True when there is a token and it expires within [withinMs].
     *
     * A token with no readable `exp` returns FALSE: with nothing to compare, "expiring" is unknowable,
     * and answering true would refresh on every call.
     */
    readonly isExpiring = (withinMs: number, now: number = Date.now()): boolean => {
        const { expiresAt, isLoggedIn } = this.current

        return isLoggedIn && expiresAt !== null && expiresAt - now <= withinMs
    }

    private publish(next: AuthSessionState): void {
        this.current = next
        // Over a COPY: a listener that unsubscribes itself while being notified would otherwise
        // mutate the set mid-iteration.
        for (const listener of [...this.listeners]) listener(next)
    }
}

function stateOf(token: string | null): AuthSessionState {
    if (token === null || token.length === 0) {
        return { token: null, claims: null, expiresAt: null, isLoggedIn: false }
    }

    const claims = decodeJwtClaims(token)

    return { token, claims, expiresAt: expiryOf(claims), isLoggedIn: true }
}

//  Transport  /////////////////////////////////////////////////////////////////////////////////////

/**
 * Wraps [inner] so every request carries the session's token.
 *
 * **Auth is a transport wrapper, not an `SdkConfig` field** — the idiom `runtime/http.ts` documents,
 * and the same shape the Kotlin client uses via a Ktor `defaultRequest` closure.
 *
 * ```ts
 * const session = new AuthSession()
 * const config = sdkConfig('https://api.example.com', authTransport(fetchTransport(), session))
 * ```
 *
 * **Attaches to every request, including public ones.** The transport sees a built URL, not a route
 * pattern, so it cannot tell them apart — `RouteRef.isPublic` lives one layer up. That mirrors the
 * Kotlin client, and it is harmless for the routes in question: a `public()` rule grants regardless
 * of who is asking. If a stale token ever turns out to make a public endpoint fail, this is the
 * place that has to change, and it needs route information plumbed down to it.
 *
 * An existing `Authorization` header is never overwritten, so a caller can still do something
 * special for one request.
 */
export function authTransport(inner: HttpTransport, session: AuthSession): HttpTransport {
    return {
        send: (request: HttpRequest): Promise<HttpResponse> => {
            const { token } = session.state()

            if (token === null || 'Authorization' in request.headers) {
                return inner.send(request)
            }

            return inner.send({
                ...request,
                headers: { ...request.headers, Authorization: `Bearer ${token}` },
            })
        },
    }
}
