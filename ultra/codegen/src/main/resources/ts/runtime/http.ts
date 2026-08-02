/**
 * The transport the generated API clients send through, and the URL builder they call.
 *
 * HAND-WRITTEN AND CHECKED IN — not generated.
 *
 * Deliberately dependency-free. A generated SDK is a library: baking in axios (or anything else)
 * forces that dependency on every consumer and collides with their own interceptors, auth refresh and
 * tracing. Everything here is expressed against an interface with a `fetch` default, so swapping the
 * transport is a one-liner.
 */

/** How a request treats credentials — cookies and TLS client certificates. Mirrors `RequestInit`. */
export type RequestCredentials = 'omit' | 'same-origin' | 'include'

/** A request about to be sent. */
export interface HttpRequest {
    method: string
    url: string
    headers: Record<string, string>
    body?: string
    signal?: AbortSignal
    /**
     * Whether the browser attaches cookies, and whether it accepts `Set-Cookie` back.
     *
     * A FIELD rather than something an auth wrapper sets on the way past, because a wrapper cannot:
     * it only sees this object, and `fetch` reads `credentials` from its own init — so a decorator has
     * nowhere to put it. Bearer auth needs no such field, which is why none existed until cookie auth
     * arrived.
     *
     * Omitted leaves `fetch`'s own default (`same-origin`) in force. `'include'` is what a
     * cross-origin cookie session needs, and it requires the server to answer with
     * `Access-Control-Allow-Credentials` — a wildcard `Allow-Origin` is refused by the browser in that
     * mode.
     */
    credentials?: RequestCredentials
}

/**
 * A response as received.
 *
 * Mirrors `RemoteResponse` (`ultra/remote/src/commonMain/kotlin/RemoteResponse.kt`) so the Kotlin and
 * TypeScript clients stay conceptually aligned.
 */
export interface HttpResponse {
    status: number
    statusText: string
    body: string
}

/** Everything the generated clients need from a transport. */
export interface HttpTransport {
    send(request: HttpRequest): Promise<HttpResponse>
}

/**
 * A `fetch`-backed transport.
 *
 * **Non-2xx responses are returned, never thrown.** That is the documented and deliberately defended
 * semantic on the Kotlin side (`ApiClient.Config`: *"Non-2xx responses are surfaced as a decoded
 * ApiResponse envelope, not thrown"*), and it is exactly why `fetch` is the right default here — not
 * rejecting on 4xx/5xx, the thing everyone complains about, is the behaviour this SDK needs. `axios`
 * would mean fighting `validateStatus` on every call.
 *
 * A rejected promise from this transport therefore means the request never completed: a network
 * failure, an abort, or a CORS rejection.
 *
 * Auth is a wrapper, not a generated concern:
 *
 * ```ts
 * const base = fetchTransport()
 *
 * const authed: HttpTransport = {
 *     send: (req) => base.send({ ...req, headers: { ...req.headers, Authorization: `Bearer ${token()}` } }),
 * }
 * ```
 *
 * @param fetchImpl the `fetch` to use — inject one to test, or to route through a proxy
 */
export function fetchTransport(fetchImpl: typeof fetch = globalThis.fetch): HttpTransport {
    return {
        async send(request: HttpRequest): Promise<HttpResponse> {
            const response = await fetchImpl(request.url, {
                method: request.method,
                headers: request.headers,
                body: request.body,
                signal: request.signal,
                // Passed through only when SET, so omitting it leaves fetch's own default rather than
                // pinning one here. Spelling `credentials: undefined` explicitly is not the same as
                // leaving the key out for every RequestInit consumer.
                ...(request.credentials !== undefined ? { credentials: request.credentials } : {}),
            })

            return {
                status: response.status,
                statusText: response.statusText,
                body: await response.text(),
            }
        },
    }
}

/** A value usable as a path or query parameter. */
export type UrlParam = string | number | boolean | null | undefined

/**
 * Builds a request URL from a route pattern.
 *
 * Mirrors the KOTLIN API CLIENT's URL builder — `buildUri`
 * (`ultra/remote/src/commonMain/kotlin/helpers.kt:87`) with `UriParamBuilder`
 * (`ultra/remote/src/commonMain/kotlin/UriParamBuilder.kt:46`) — so this client and the Kotlin one
 * produce the same URL for the same route:
 *
 * - `{name}` placeholders in [pattern] are replaced from [pathParams], percent-encoded.
 * - A query parameter whose value is `null`, `undefined` or the empty string is **omitted**, not sent
 *   empty. The Kotlin renderer drops those, and a server-side parameter with a default would receive
 *   `""` instead of its default if we sent them.
 * - Each query key appears at most once: funktor converts every route parameter to a single string
 *   (`OutgoingConverter.convert`), so repeated keys are not part of the protocol.
 *
 * Encoding uses `encodeURIComponent`, matching `encodeUriComponent`
 * (`ultra/common/src/commonMain/kotlin/strings_mp.kt`) character for character.
 *
 * **NOT `TypedRouteRenderer`**, despite what this KDoc claimed until 2026-07-30. That class is the
 * SERVER-SIDE link renderer, and it double-encodes query values: it calls
 * `encodeURLQueryComponent(encodeFull = true)` (`TypedRouteRenderer.kt:43`) and then hands the
 * already-encoded string to `toUri`, which runs `URLEncoder.encode` over it again
 * (`ultra/common/src/jvmMain/kotlin/strings.kt:33`). So `?at=2026-07-30T10:15:30Z` renders as
 * `at=2026%252D07%252D30T10%253A15%253A30Z` there and `at=2026-07-30T10%3A15%3A30Z` here. Path
 * parameters ARE equivalent on both sides; only the query diverges. Do not "fix" this file to match
 * — the double-encode is the defect, tracked separately.
 *
 * @throws if [pattern] still contains an unfilled `{placeholder}` — otherwise the literal braces would
 *   be sent and surface as a puzzling 404.
 */
export function buildUrl(
    baseUrl: string,
    pattern: string,
    pathParams: Record<string, UrlParam> = {},
    queryParams: Record<string, UrlParam> = {},
): string {
    let path = pattern

    for (const [key, value] of Object.entries(pathParams)) {
        path = path.replaceAll(`{${key}}`, encodeURIComponent(stringify(value)))
    }

    const unfilled = /\{([^}]+)}/.exec(path)

    if (unfilled) {
        throw new Error(`buildUrl: route '${pattern}' has no value for path parameter '${unfilled[1]}'`)
    }

    const query = Object.entries(queryParams)
        .filter(([, value]) => value !== null && value !== undefined && value !== '')
        .map(([key, value]) => `${encodeURIComponent(key)}=${encodeURIComponent(stringify(value))}`)

    const url = `${baseUrl.replace(/\/+$/, '')}/${path.replace(/^\/+/, '')}`

    return query.length > 0 ? `${url}?${query.join('&')}` : url
}

function stringify(value: UrlParam): string {
    return value === null || value === undefined ? '' : String(value)
}
