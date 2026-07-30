/**
 * What a generated API client is built on: the config it holds and the call it makes.
 *
 * HAND-WRITTEN AND CHECKED IN — not generated.
 *
 * Generated clients are deliberately thin. Each member is a one-line call to [request], so everything
 * that could be got wrong once — URL building, the JSON body, envelope validation, the non-2xx
 * contract — lives here and is verified here, rather than being re-emitted per endpoint where a bug
 * would be copied N times.
 */
import { z } from 'zod'
import { type ApiResponse, apiResponse } from './apiResponse.ts'
import { type HttpTransport, type UrlParam, buildUrl, fetchTransport } from './http.ts'

/**
 * What every generated client needs.
 *
 * Deliberately just these two. **Auth is a transport wrapper, not a config field** — that is the
 * documented idiom in `http.ts`, and a competing `headers` hook here would give two ways to do it that
 * disagree the moment both are used.
 */
export interface SdkConfig {
    /** Base URL of the API, e.g. `https://api.example.com`. Trailing slashes are tolerated. */
    readonly baseUrl: string
    /** Where requests are sent. */
    readonly transport: HttpTransport
}

/** Builds an [SdkConfig] with the `fetch` transport. */
export function sdkConfig(baseUrl: string, transport: HttpTransport = fetchTransport()): SdkConfig {
    return { baseUrl, transport }
}

/** The per-call inputs a generated member passes through. */
export interface RequestOptions {
    /** Values for `{name}` placeholders in the route pattern. */
    readonly path?: Record<string, UrlParam>
    /** Query parameters. Null, undefined and empty values are omitted — see `buildUrl`. */
    readonly query?: Record<string, UrlParam>
    /** Request body, JSON-encoded. Omitted entirely when `undefined`. */
    readonly body?: unknown
    /** Aborts the request — pass one from a Vue `onScopeDispose` to cancel on unmount. */
    readonly signal?: AbortSignal
}

/**
 * The server answered something that is not an `ApiResponse` envelope.
 *
 * Distinct from a non-2xx response, which IS an envelope and is returned normally. This means the
 * contract itself was broken: a proxy's HTML 502, a truncated body, or a payload that does not match
 * the schema the generator derived from the Kotlin type. The last case is the one worth catching — it
 * means the SDK is stale relative to the server.
 */
export class ApiProtocolError extends Error {
    /** HTTP status of the offending response. */
    readonly status: number
    /** The raw body, truncated for the message but kept whole here. */
    readonly body: string

    constructor(message: string, status: number, body: string) {
        super(message)
        this.name = 'ApiProtocolError'
        this.status = status
        this.body = body
    }
}

/** A non-2xx envelope, thrown only by [unwrap]. Never thrown by [request]. */
export class ApiError extends Error {
    /** The full envelope, so `messages` and `insights` survive the throw. */
    readonly response: ApiResponse<unknown>

    constructor(response: ApiResponse<unknown>) {
        super(
            response.messages?.find((it) => it.type === 'error')?.text ??
                `${response.status.value} ${response.status.description}`,
        )
        this.name = 'ApiError'
        this.response = response
    }
}

/**
 * Sends one request and validates the envelope around [data].
 *
 * **A non-2xx response is returned, not thrown** — the contract `http.ts` documents and the Kotlin
 * `ApiClient` defends. Callers branch on `isSuccess`, or opt into throwing with [unwrap].
 *
 * @throws ApiProtocolError when the response is not a valid envelope for [data].
 */
export async function request<T>(
    config: SdkConfig,
    method: string,
    pattern: string,
    data: z.ZodType<T>,
    options: RequestOptions = {},
): Promise<ApiResponse<T>> {
    const hasBody = options.body !== undefined

    const response = await config.transport.send({
        method,
        url: buildUrl(config.baseUrl, pattern, options.path ?? {}, options.query ?? {}),
        // Accept is sent always; Content-Type only with a body, because a bodiless request declaring
        // a content type is what makes some proxies and CORS preflights behave oddly.
        headers: hasBody
            ? { 'Accept': 'application/json', 'Content-Type': 'application/json' }
            : { 'Accept': 'application/json' },
        body: hasBody ? JSON.stringify(options.body) : undefined,
        signal: options.signal,
    })

    let parsed: unknown

    try {
        parsed = JSON.parse(response.body)
    } catch {
        throw new ApiProtocolError(
            `${method} ${pattern} answered ${response.status} with a body that is not JSON: ` +
                `${excerpt(response.body)}`,
            response.status,
            response.body,
        )
    }

    const result = apiResponse(data).safeParse(parsed)

    if (!result.success) {
        throw new ApiProtocolError(
            `${method} ${pattern} answered ${response.status} with a payload that does not match the ` +
                `generated schema — the SDK is probably stale. ${result.error.issues
                    .map((issue) => `${issue.path.join('.') || '(root)'}: ${issue.message}`)
                    .join('; ')}`,
            response.status,
            response.body,
        )
    }

    return result.data
}

/**
 * Returns the payload, throwing [ApiError] when the envelope is not 2xx.
 *
 * The return type stays `T | null` on purpose: `data` is nullable on success too — `noContent()` and
 * `okOrNotFound()` both send `null` — so throwing on failure does not remove the null check, and
 * pretending otherwise would push a lie into every call site.
 */
export function unwrap<T>(response: ApiResponse<T>): T | null {
    if (response.status.value < 200 || response.status.value > 299) {
        throw new ApiError(response)
    }

    return response.data
}

/** First 200 characters of [body], for an error message that stays readable. */
function excerpt(body: string): string {
    const trimmed = body.trim()

    return trimmed.length > 200 ? `${trimmed.slice(0, 200)}…` : trimmed
}
