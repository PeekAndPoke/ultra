package io.peekandpoke.funktor.insights

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.funktor.insights.api.InsightsRecordRef

/** What the record endpoint and the list accept as a record (ULTRA-00304). */
class InsightsRecordRefSpec : StringSpec({

    "the names the writer produces are records" {
        InsightsRecordRef("records-2026-11-11", "2026-11-11T09:06:16.123456789.json").isRecord() shouldBe true
        InsightsRecordRef("records-2026-11-11", "2026-11-11T09:06.json").isRecord() shouldBe true
        InsightsRecordRef("records-2026-11-11", "x..json").isRecord() shouldBe true
    }

    "anything else is not" {
        listOf(
            InsightsRecordRef("elsewhere", "a.json"),
            InsightsRecordRef("records-2026-1-1", "a.json"),
            InsightsRecordRef("records-2026-11-11", "a.txt"),
            InsightsRecordRef("records-2026-11-11", "sub/a.json"),
            InsightsRecordRef("records-2026-11-11", "a\\b.json"),
            InsightsRecordRef("records-2026-11-11", "x\\..\\y.json"),
            InsightsRecordRef("records-2026-11-11", "a${Char(0)}.json"),
            InsightsRecordRef("records-2026-11-11", "a${Char(1)}.json"),
        ).forEach { ref -> ref.isRecord() shouldBe false }
    }
})
