/**
 * Shared prop types for the funktor/ui primitives.
 *
 * A separate module rather than exports from the components, for two reasons: `<script setup>` cannot
 * contain ES module exports at all, and a tab that only wants to BUILD a `StatCell[]` should not have to
 * import the component that renders it.
 *
 * Siblings are imported WITH the `.ts` extension throughout the generated SDK -- an extensionless
 * specifier resolves only under `moduleResolution: bundler` and is ERR_MODULE_NOT_FOUND everywhere else,
 * while `tsc` stays silent about it.
 */

/** Severity, used wherever a value carries meaning rather than decoration. */
export type FkTone = 'ok' | 'warn' | 'error' | 'neutral'

/**
 * The server's redaction placeholder -- a VALUE to render, not an error state.
 *
 * Mirrors two Kotlin constants that already agree on the text: `HeaderLogging.REDACTED`
 * (`funktor/insights/src/jvmMain/kotlin/HeaderLogging.kt:71`) and `Redacted.PLACEHOLDER`
 * (`ultra/common/src/commonMain/kotlin/model/Redacted.kt:79`). It is deliberately fixed so it cannot be
 * mistaken for content -- and the reason the UI marks it is the opposite mistake: a reader must not
 * conclude the app's actual password is the literal string below.
 */
export const FK_REDACTED = '***redacted***'

/**
 * One row of a `FactList`. Order is preserved and meaningful, so this is a list, not a map.
 *
 * A `tone` renders the value as a label rather than plain text -- use it only where the value carries
 * severity (a status code), never merely to draw attention.
 */
export interface Fact {
    key: string
    value: string | number | null | undefined
    tone?: FkTone
}

/** One cell of a `StatStrip`. `value` is pre-formatted: units and precision are the caller's business. */
export interface StatCell {
    label: string
    value: string | number | null | undefined
    tone?: FkTone
    /** Optional second line, for a cell that needs one -- `open:` / `max:` on file descriptors. */
    hint?: string
}

/**
 * Map a numeric threshold onto a tone, ascending-worse.
 *
 * Extracted because the old GUI applied the same shape to response time, view render time and container
 * age, each with its own numbers, and getting the boundary conditions subtly different per tab is exactly
 * the sort of drift nobody notices.
 *
 * Boundaries are EXCLUSIVE, matching the old thresholds ("red above 300ms"), and a null value is
 * `neutral` rather than `ok` -- an unknown figure is not a good one.
 */
export function toneAbove(
    value: number | null | undefined,
    thresholds: { warn: number; error: number },
): FkTone {
    if (value === null || value === undefined || Number.isNaN(value)) return 'neutral'
    if (value > thresholds.error) return 'error'
    if (value > thresholds.warn) return 'warn'
    return 'ok'
}

/**
 * Tone for an HTTP status code.
 *
 * 1xx/3xx are `neutral`, not `ok`: a redirect is not a success, and colouring it green makes a redirect
 * loop look healthy. Null is `neutral` -- a record can carry no status at all.
 */
export function toneForStatus(status: number | null | undefined): FkTone {
    if (status === null || status === undefined) return 'neutral'
    if (status >= 200 && status < 300) return 'ok'
    if (status >= 400 && status < 500) return 'warn'
    if (status >= 500) return 'error'
    return 'neutral'
}
