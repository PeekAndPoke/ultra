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
    /**
     * Attach raw response bodies to thrown errors. **Off by default, and never turn it on in
     * production.**
     *
     * Errors escape the application: a global handler hands them to Sentry or `console.error`, and
     * Sentry serialises an Error's own enumerable properties. A response body is real user data — the
     * error that reports a stale SDK fires on an ordinary successful response — so carrying it would
     * export that data to a third party without anyone writing code to send it.
     *
     * The default diagnostics name the FIELDS that drifted without their values, which is what
     * identifies a stale SDK. This flag is for reproducing a problem locally.
     */
    readonly debug?: boolean
}

/** Builds an [SdkConfig] with the `fetch` transport. */
export function sdkConfig(baseUrl: string, transport: HttpTransport = fetchTransport()): SdkConfig {
    return { baseUrl, transport }
}

/**
 * What a CALLER may pass to a generated endpoint member.
 *
 * Deliberately narrower than [RequestOptions]: path, query and body are the generator's business —
 * it derives them from the route — while cancellation is the caller's. Exposing the full
 * [RequestOptions] on a generated member would let a caller override the URL the route defines.
 */
export interface CallOptions {
    /** Aborts the request. Pass one from a Vue `onScopeDispose` to cancel on unmount. */
    readonly signal?: AbortSignal
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
    /** Size of the response body in characters. Enough to tell "empty" from "an HTML error page". */
    readonly bodyLength: number
    /**
     * Where the payload failed the generated schema — field paths and issue codes, never values.
     *
     * This is what identifies a stale SDK: `data.members.0.email` tells you exactly which field
     * moved. Empty when the body was not JSON at all.
     */
    readonly issues: readonly string[]
    /**
     * The raw body — **only when `SdkConfig.debug` is set**, otherwise `undefined`.
     *
     * Withheld by default because errors leave the application. A global handler passes them to
     * Sentry or `console.error`, and Sentry serialises an Error's own enumerable properties, so a
     * retained body is exported to a third party. This error's most common cause is a stale SDK,
     * which fires on a perfectly ordinary response full of real user data.
     */
    readonly body?: string

    constructor(message: string, status: number, bodyLength: number, issues: readonly string[], body?: string) {
        super(message)
        this.name = 'ApiProtocolError'
        this.status = status
        this.bodyLength = bodyLength
        this.issues = issues

        // Assigned CONDITIONALLY, not set to undefined: an own property named `body` would still be
        // serialised by some reporters, and `undefined` round-trips as `null` through JSON.
        if (body !== undefined) {
            this.body = body
        }
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
            `${method} ${pattern} answered ${response.status} with a body that is not JSON ` +
                `(${response.body.length} characters). A proxy error page is the usual cause; its ` +
                `text is withheld because it commonly names internal hosts. Set debug: true on ` +
                `SdkConfig to include it.`,
            response.status,
            response.body.length,
            [],
            config.debug ? response.body : undefined,
        )
    }

    const result = apiResponse(data).safeParse(parsed)

    if (!result.success) {
        // Paths and CODES, not `issue.message`.
        //
        // MEASURED, not assumed (zod 4.4.3, 2026-07-31): zod messages describe the EXPECTATION and
        // never quote the received value — literal, enum, invalid_type, discriminator and size issues
        // were all checked and none leaked. So using `issue.message` would be safe TODAY. Path plus
        // code is preferred anyway for two reasons that do not depend on that: it is structural
        // rather than human prose, so it survives a zod upgrade changing the wording, and it keeps
        // the guarantee independent of a library decision this code does not control.
        const issues = result.error.issues.map(
            (issue) => `${issue.path.join('.') || '(root)'} (${issue.code})`,
        )

        throw new ApiProtocolError(
            `${method} ${pattern} answered ${response.status} with a payload that does not match the ` +
                `generated schema — the SDK is probably stale. Fields: ${issues.join('; ')}. ` +
                `Response body withheld (${response.body.length} characters); set debug: true on ` +
                `SdkConfig to include it.`,
            response.status,
            response.body.length,
            issues,
            config.debug ? response.body : undefined,
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
