/**
 * TypeScript mirror of the `ApiResponse<T>` envelope every funktor endpoint answers with.
 *
 * HAND-WRITTEN AND CHECKED IN — not generated. The envelope is generic, and TypeScript has real
 * generics, so monomorphizing it the way the model emitter monomorphizes `PageOf<Talk>` would produce
 * one `ApiResponseTalk` per payload type for no gain. Only the payload is generated; this stays.
 *
 * Mirrors `ultra/remote/src/commonMain/kotlin/ApiResponse.kt`. `ApiResponseParitySpec` slumbers a real
 * envelope and fails if the two drift.
 */
import { z } from 'zod'

/**
 * Whether a field is optional follows the same rule the generator applies to generated types:
 * **optional in TypeScript iff the Kotlin constructor parameter has a default.** Slumber writes every
 * key including nulls, so the server always sends them — but the Kotlin awaker accepts them missing,
 * and the client should too.
 */

/** A status code and its reason phrase. Slumbers as an object, not a bare number. */
export const HttpStatusCode = z.object({
    value: z.number(),
    description: z.string(),
})
export type HttpStatusCode = z.infer<typeof HttpStatusCode>

/**
 * The wire shape of an `MpInstant`, as carried by `Message.ts`.
 *
 * Declared here rather than imported from `./datetime.ts` so this module stands alone — an SDK whose
 * API never mentions a datetime type still needs the envelope, and `datetime.ts` ships only when a
 * datetime type is actually reachable. `ApiResponseParitySpec` pins the two against the same codec, so
 * the duplication cannot drift apart silently.
 */
const instant = z.object({
    ts: z.number(),
    timezone: z.string(),
    human: z.string(),
})

/** A message sent alongside the payload — the mechanism behind `withInfo` / `withWarning` / `withError`. */
export const Message = z.object({
    type: z.enum(['info', 'warning', 'error']),
    text: z.string(),
    ts: instant.nullish(),
})
export type Message = z.infer<typeof Message>

/** Server-side timing and tracing data. Present only when the server is configured to emit it. */
export const Insights = z.object({
    ts: z.number(),
    method: z.string(),
    url: z.string(),
    server: z.string(),
    status: HttpStatusCode,
    durationMs: z.number().nullable(),
    detailsUri: z.string().nullable(),
    detailsUrl: z.string().nullable(),
})
export type Insights = z.infer<typeof Insights>

/**
 * The envelope.
 *
 * `data` is nullable on every response, including successful ones — `noContent()` and `okOrNotFound()`
 * both send `null`.
 */
export interface ApiResponse<T> {
    status: HttpStatusCode
    data: T | null
    messages?: Message[] | null
    insights?: Insights | null
}

/**
 * Builds the schema for an envelope carrying [data].
 *
 * A function rather than a value because the envelope is generic: `apiResponse(Talk)` parses a
 * `ApiResponse<Talk>`.
 */
export function apiResponse<T>(data: z.ZodType<T>): z.ZodType<ApiResponse<T>> {
    return z.object({
        status: HttpStatusCode,
        data: data.nullable(),
        messages: z.array(Message).nullish(),
        insights: Insights.nullish(),
    })
}

/**
 * `true` for 2xx.
 *
 * Mirrors `ApiResponse.isSuccess()`. Worth having because a non-2xx response is delivered normally
 * rather than thrown — see `http.ts` — so callers must branch on this rather than on a rejected
 * promise.
 */
export function isSuccess(response: ApiResponse<unknown>): boolean {
    return response.status.value >= 200 && response.status.value <= 299
}
