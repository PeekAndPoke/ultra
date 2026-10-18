package io.peekandpoke.funktor.insights.collectors

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

/**
 * Pins [VaultCollector.KEY] to the literal the frontend registries key on.
 *
 * The frontend maps `"vault"` to the tab that withholds bind values, and an UNREGISTERED key falls
 * through to a raw JSON view of the slice. So renaming this constant does not break a build anywhere —
 * it silently swaps a redacting view for a dumping one, in a page whose whole point is that the data is
 * sensitive. The tab's display label is already "Database", which makes a tidy-up rename plausible.
 *
 * The frontend lives in `funktor/codegen`, which deliberately does not depend on this module, so no
 * compiler can link the two. This spec is the link. Raised by the `/feature-review` gate, 2026-08-24.
 */
class VaultCollectorKeySpec : FreeSpec() {

    init {
        "the collector key matches what the frontend tab registry expects" {
            withClue(
                "InsightsDetailPage.vue maps this exact string to VaultTab. Change it and the tab stops " +
                        "matching, and the slice renders through the raw JsonTree fallback instead — " +
                        "which is the disclosure VaultTab exists to prevent."
            ) {
                VaultCollector.KEY shouldBe "vault"
            }
        }
    }
}
