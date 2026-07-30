/**
 * Stand-in for a THIRD-PARTY runtime module in the extension-point test.
 *
 * Mirrors a custom Slumber codec that writes `{cents, currency}`, exactly as a downstream project
 * would hand-write for one of its own types.
 */
import { z } from 'zod'

export const Money = z.object({
    cents: z.number(),
    currency: z.string(),
})
export type Money = z.infer<typeof Money>

export function formatMoney(value: Money): string {
    return `${(value.cents / 100).toFixed(2)} ${value.currency}`
}
