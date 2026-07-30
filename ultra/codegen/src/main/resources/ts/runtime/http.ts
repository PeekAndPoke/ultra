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

/** A request about to be sent. */
export interface HttpRequest {
    method: string
    url: string
    headers: Record<string, string>
    body?: string
    signal?: AbortSignal
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
 * Mirrors `TypedRouteRenderer` (`funktor/core/src/jvmMain/kotlin/broker/TypedRouteRenderer.kt:28`)
 * so this client and the Kotlin one produce the same URL for the same route:
 *
 * - `{name}` placeholders in [pattern] are replaced from [pathParams], percent-encoded.
 * - A query parameter whose value is `null`, `undefined` or the empty string is **omitted**, not sent
 *   empty. The Kotlin renderer drops those, and a server-side parameter with a default would receive
 *   `""` instead of its default if we sent them.
 * - Each query key appears at most once: funktor converts every route parameter to a single string
 *   (`OutgoingConverter.convert`), so repeated keys are not part of the protocol.
 *
 * Encoding uses `encodeURIComponent`. It leaves a handful of sub-delimiters unescaped that Kotlin's
 * `encodeURLQueryComponent(encodeFull = true)` escapes, but both decode to the same string server
 * side, so the two clients remain interchangeable.
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
