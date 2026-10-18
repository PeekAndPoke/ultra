package io.peekandpoke.funktor.insights

import io.peekandpoke.ultra.slumber.Codec
import io.peekandpoke.ultra.slumber.SlumberConfig

/**
 * The [Codec] the insights write path serializes with.
 *
 * A distinct type rather than a bare `Codec` binding, for the same reason `InsightsMapper` was a
 * distinct `ObjectMapper` subclass: registering `Codec` itself would claim a very general type in every
 * application's kontainer, for one module's benefit.
 *
 * Replaces `InsightsMapper` (Jackson). That matters beyond tidiness — Slumber is what knows about
 * `Redacted<T>`, so config secrets are written as `***redacted***`. Jackson had no idea the type existed
 * and wrote `{"value": "<the secret>"}`, which is why a name-based redaction had to exist alongside it.
 */
class InsightsCodec : Codec(config = SlumberConfig.default)
