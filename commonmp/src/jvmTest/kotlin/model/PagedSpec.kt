package de.peekandpoke.ultra.model

import de.peekandpoke.ultra.common.model.Paged
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

class PagedSpec : FreeSpec() {

    init {
        "fullPageCount" - {

            "when fullItemCount == null" {

                val subject = Paged(
                    items = (1..10).map { it },
                    page = 1,
                    epp = 10,
                    fullItemCount = null
                )

                subject.fullPageCount shouldBe null
            }

            "when fullItemCount != null" {

                val subject01 = Paged(
                    items = (1..10).map { it },
                    page = 1,
                    epp = 10,
                    fullItemCount = 9
                )

                subject01.fullPageCount shouldBe 1L

                val subject02 = Paged(
                    items = (1..10).map { it },
                    page = 1,
                    epp = 10,
                    fullItemCount = 10
                )

                subject02.fullPageCount shouldBe 1L

                val subject03 = Paged(
                    items = (1..10).map { it },
                    page = 1,
                    epp = 10,
                    fullItemCount = 11
                )

                subject03.fullPageCount shouldBe 2L
            }
        }
    }
}
