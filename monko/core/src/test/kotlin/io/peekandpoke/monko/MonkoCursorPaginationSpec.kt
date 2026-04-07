package io.peekandpoke.monko

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.peekandpoke.monko.vault.vault.MongoTypedQuery
import io.peekandpoke.ultra.reflection.kType
import io.peekandpoke.ultra.vault.NullEntityCache
import io.peekandpoke.ultra.vault.Stored
import io.peekandpoke.ultra.vault.TypedQuery

class MonkoCursorPaginationSpec : FreeSpec() {

    data class Item(val name: String)

    init {
        "MonkoCursor pagination scenarios" - {

            "page 1 of 3 pages (epp=2, total=5)" {
                val pageItems = listOf(
                    stored("1", Item("A")),
                    stored("2", Item("B")),
                )
                val cursor = createCursor(pageItems, fullCount = 5)

                cursor.count shouldBe 2
                cursor.fullCount shouldBe 5
            }

            "page 2 of 3 pages (epp=2, total=5)" {
                val pageItems = listOf(
                    stored("3", Item("C")),
                    stored("4", Item("D")),
                )
                val cursor = createCursor(pageItems, fullCount = 5)

                cursor.count shouldBe 2
                cursor.fullCount shouldBe 5
            }

            "last page (epp=2, total=5) has only 1 item" {
                val pageItems = listOf(
                    stored("5", Item("E")),
                )
                val cursor = createCursor(pageItems, fullCount = 5)

                cursor.count shouldBe 1
                cursor.fullCount shouldBe 5
            }

            "empty page when beyond range" {
                val cursor = createCursor<Stored<Item>>(emptyList(), fullCount = 5)

                cursor.count shouldBe 0
                cursor.fullCount shouldBe 5
            }

            "no fullCount when no filter is applied" {
                val items = listOf(
                    stored("1", Item("A")),
                    stored("2", Item("B")),
                )
                val cursor = createCursor(items, fullCount = null)

                cursor.count shouldBe 2
                cursor.fullCount.shouldBeNull()
            }

            "fullCount of 0 with empty result" {
                val cursor = createCursor<Stored<Item>>(emptyList(), fullCount = 0)

                cursor.count shouldBe 0
                cursor.fullCount shouldBe 0
            }

            "single item page with large fullCount" {
                val items = listOf(stored("1", Item("Only")))
                val cursor = createCursor(items, fullCount = 10_000)

                cursor.count shouldBe 1
                cursor.fullCount shouldBe 10_000
            }
        }
    }

    companion object {
        private fun <T> stored(key: String, value: T): Stored<T> = Stored(
            _id = "items/$key",
            _key = key,
            _rev = "",
            value = value,
        )

        @Suppress("UNCHECKED_CAST")
        private fun <T> createCursor(
            entries: List<T>,
            fullCount: Long? = null,
        ): MonkoCursor<T> = MonkoCursor(
            entries = entries,
            query = MongoTypedQuery.of(type = kType<Any>()) as TypedQuery<T>,
            entityCache = NullEntityCache,
            _fullCount = fullCount,
        )
    }
}
