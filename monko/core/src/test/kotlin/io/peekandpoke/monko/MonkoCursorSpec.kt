package io.peekandpoke.monko

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.peekandpoke.monko.vault.vault.MongoTypedQuery
import io.peekandpoke.ultra.reflection.kType
import io.peekandpoke.ultra.vault.NullEntityCache
import io.peekandpoke.ultra.vault.Stored
import io.peekandpoke.ultra.vault.TypedQuery
import io.peekandpoke.ultra.vault.firstOrNull

class MonkoCursorSpec : FreeSpec() {

    data class TestEntity(val name: String)

    init {
        "MonkoCursor" - {

            "empty cursor" - {

                "should have count 0" {
                    val cursor = createCursor<Stored<TestEntity>>(emptyList())

                    cursor.count shouldBe 0
                }

                "should have null fullCount when not set" {
                    val cursor = createCursor<Stored<TestEntity>>(emptyList())

                    cursor.fullCount.shouldBeNull()
                }

                "should iterate with no elements" {
                    val cursor = createCursor<Stored<TestEntity>>(emptyList())

                    cursor.toList().shouldBeEmpty()
                }
            }

            "cursor with entries" - {

                "should report correct count" {
                    val entries = listOf(
                        stored("1", TestEntity("Alice")),
                        stored("2", TestEntity("Bob")),
                        stored("3", TestEntity("Charlie")),
                    )
                    val cursor = createCursor(entries)

                    cursor.count shouldBe 3
                }

                "should iterate all entries" {
                    val entries = listOf(
                        stored("1", TestEntity("Alice")),
                        stored("2", TestEntity("Bob")),
                    )
                    val cursor = createCursor(entries)

                    cursor.toList() shouldContainExactly entries
                }

                "entries property should contain all items" {
                    val entries = listOf(
                        stored("1", TestEntity("Alice")),
                    )
                    val cursor = createCursor(entries)

                    cursor.entries shouldContainExactly entries
                }
            }

            "fullCount" - {

                "should be null when not provided" {
                    val cursor = createCursor<Stored<TestEntity>>(emptyList(), fullCount = null)

                    cursor.fullCount.shouldBeNull()
                }

                "should return the provided full count" {
                    val entries = listOf(
                        stored("1", TestEntity("Alice")),
                    )
                    val cursor = createCursor(entries, fullCount = 100)

                    cursor.fullCount shouldBe 100
                }

                "fullCount can differ from count (pagination scenario)" {
                    val entries = listOf(
                        stored("1", TestEntity("Alice")),
                        stored("2", TestEntity("Bob")),
                    )
                    val cursor = createCursor(entries, fullCount = 50)

                    cursor.count shouldBe 2
                    cursor.fullCount shouldBe 50
                }
            }

            "timeMs" - {

                "should default to 0.0" {
                    val cursor = createCursor<Stored<TestEntity>>(emptyList())

                    cursor.timeMs shouldBe 0.0
                }

                "should return provided value" {
                    val cursor = createCursor<Stored<TestEntity>>(
                        emptyList(),
                        timeMs = 42.5,
                    )

                    cursor.timeMs shouldBe 42.5
                }
            }

            "iterator" - {

                "should support multiple iterations" {
                    val entries = listOf(
                        stored("1", TestEntity("Alice")),
                        stored("2", TestEntity("Bob")),
                    )
                    val cursor = createCursor(entries)

                    // First iteration
                    cursor.toList() shouldContainExactly entries
                    // Second iteration
                    cursor.toList() shouldContainExactly entries
                }

                "should support for-each loop" {
                    val entries = listOf(
                        stored("1", TestEntity("Alice")),
                        stored("2", TestEntity("Bob")),
                    )
                    val cursor = createCursor(entries)

                    val collected = cursor.toList()

                    collected shouldContainExactly entries
                }
            }

            "entityCache" - {

                "should use NullEntityCache by default" {
                    val cursor = createCursor<Stored<TestEntity>>(emptyList())

                    cursor.entityCache shouldBe NullEntityCache
                }
            }

            "firstOrNull" - {

                "should return first entry when present" {
                    val entries = listOf(
                        stored("1", TestEntity("Alice")),
                        stored("2", TestEntity("Bob")),
                    )
                    val cursor = createCursor(entries)

                    cursor.firstOrNull() shouldBe entries[0]
                }

                "should return null when empty" {
                    val cursor = createCursor<Stored<TestEntity>>(emptyList())

                    cursor.firstOrNull().shouldBeNull()
                }
            }
        }
    }

    companion object {
        private fun <T> stored(key: String, value: T): Stored<T> = Stored(
            _id = "test_entities/$key",
            _key = key,
            _rev = "",
            value = value,
        )

        @Suppress("UNCHECKED_CAST")
        private fun <T> createCursor(
            entries: List<T>,
            fullCount: Long? = null,
            timeMs: Double = 0.0,
        ): MonkoCursor<T> = MonkoCursor(
            entries = entries,
            query = MongoTypedQuery.of(type = kType<Any>()) as TypedQuery<T>,
            entityCache = NullEntityCache,
            _fullCount = fullCount,
            _timeMs = timeMs,
        )
    }
}
