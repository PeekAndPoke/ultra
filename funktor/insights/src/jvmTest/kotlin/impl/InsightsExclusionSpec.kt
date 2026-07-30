package io.peekandpoke.funktor.insights.impl

import impl.InsightsFull
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.funktor.insights.api.InsightsApi

/**
 * The insights recorder must not observe the insights API.
 *
 * Without this, every read of a record writes another record — one whose request and response slices
 * describe the superuser who was reading — so a browsing session inflates the depot with observations
 * of itself, each containing the reader's own headers.
 */
class InsightsExclusionSpec : StringSpec({

    "the API's own routes are excluded" {
        // Derived from the real route constants, so renaming the endpoints cannot silently
        // re-enable self-observation.
        InsightsFull.isExcluded("${InsightsApi.base}/records") shouldBe true
        InsightsFull.isExcluded("${InsightsApi.base}/records/records-2026-07-31/a.json") shouldBe true
    }

    "the exclusion survives a query string and a host prefix" {
        InsightsFull.isExcluded("${InsightsApi.base}/records?limit=10") shouldBe true
        InsightsFull.isExcluded("https://admin.example.com${InsightsApi.base}/records") shouldBe true
    }

    "favicon requests are excluded, as they always were" {
        InsightsFull.isExcluded("/favicon.ico") shouldBe true
    }

    "ordinary application routes are still recorded" {
        InsightsFull.isExcluded("/api/things") shouldBe false
        InsightsFull.isExcluded("/") shouldBe false
    }

    "an application route that merely mentions insights is still recorded" {
        // The exclusion used to be a loose `contains("/insights")`, which swallowed these: an app would
        // silently get no insights for its own dashboard. Matching the API base makes it exact.
        InsightsFull.isExcluded("/api/insights-dashboard") shouldBe false
        InsightsFull.isExcluded("/insights") shouldBe false
        InsightsFull.isExcluded("/api/insight-reports") shouldBe false
    }
})
