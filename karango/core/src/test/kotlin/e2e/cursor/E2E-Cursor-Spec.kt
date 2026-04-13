package io.peekandpoke.karango.e2e.cursor

import io.kotest.assertions.assertSoftly
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeSameSizeAs
import io.kotest.matchers.shouldBe
import io.peekandpoke.karango.e2e.database
import io.peekandpoke.karango.e2e.karangoDriver
import io.peekandpoke.karango.testdomain.TestPerson
import io.peekandpoke.karango.testdomain.testPersons
import io.peekandpoke.ultra.vault.map
import io.peekandpoke.ultra.vault.value

@Suppress("ClassName")
class `E2E-Cursor-Spec` : StringSpec() {

    private val coll get() = database.testPersons
    private val numDocs = 12_345

    init {
        "Setup by clearing the collection" {
            // get coll and clear all entries
            coll.removeAll()
        }

        "Inserting a huge many of entities and retrieving all of them" {

            val original = (0..<numDocs).map {
                TestPerson(name = "p$it")
            }

            val saved = coll.batchInsertValues(original)

            withClue("Number of inserted documents must be correct") {
                original.size shouldBe numDocs
                saved shouldBeSameSizeAs original
            }

            karangoDriver.codec.entityCache.clear()

            val cursor = coll.findAll()
            val reloaded = cursor.toList()

            assertSoftly {
                withClue("totalCount must be correct") {
                    cursor.count shouldBe numDocs
                    cursor.fullCount shouldBe numDocs
                }

                withClue("Number of reloaded documents must be correct") {
                    original shouldBeSameSizeAs reloaded
                }

                withClue("Inserted data must be correct") {
                    original.map { it.name } shouldBe reloaded.map { it.value.name }
                }
            }
        }

        "Iterating a cursor multiple times must work (using map)" {
            val cursor = coll.findAll()

            val first = cursor.map { it }
            val second = cursor.map { it }

            assertSoftly {
                first shouldBeSameSizeAs second
                first.map { it.value.name } shouldBe second.map { it.value.name }
            }
        }

        "Iterating a cursor multiple times must work (using toList)" {
            val cursor = coll.findAll()

            val first = cursor.toList()
            val second = cursor.toList()

            assertSoftly {
                first shouldBeSameSizeAs second
                first.map { it.value.name } shouldBe second.map { it.value.name }
            }
        }

        "Iterating a cursor multiple times must work (mixing map and toList)" {
            val cursor = coll.findAll()

            val first = cursor.map { it }
            val second = cursor.toList()

            assertSoftly {
                first shouldBeSameSizeAs second
                first shouldBeSameSizeAs second
                first.map { it.value.name } shouldBe second.map { it.value.name }
            }
        }
    }
}
