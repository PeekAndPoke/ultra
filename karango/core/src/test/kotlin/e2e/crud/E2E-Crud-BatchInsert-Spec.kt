package io.peekandpoke.karango.e2e.crud

import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.peekandpoke.karango.e2e.database
import io.peekandpoke.karango.testdomain.TestPerson
import io.peekandpoke.karango.testdomain.testPersons
import io.peekandpoke.ultra.vault.New
import io.peekandpoke.ultra.vault.value

@Suppress("ClassName")
class `E2E-Crud-BatchInsert-Spec` : StringSpec({

    "batchInsert with multiple items should insert all" {

        val coll = database.testPersons.apply {
            removeAll()
        }

        val items = listOf(
            New(JonBonJovi),
            New(EdgarAllanPoe),
            New(HeinzRudolfKunze),
        )

        val results = coll.batchInsert(items)

        assertSoftly {
            results.size shouldBe 3

            results.forEach { stored ->
                stored._id shouldNotBe null
                stored._key shouldNotBe null
            }

            results.map { it.value.name }.toSet() shouldBe setOf("Jon Jovi", "Edgar Poe", "Heinz Kunze")
        }

        // verify they are actually in the database
        coll.count() shouldBe 3
    }

    "batchInsert with empty list should not fail" {

        val coll = database.testPersons.apply {
            removeAll()
        }

        val results = coll.batchInsert(emptyList<New<TestPerson>>())

        results.size shouldBe 0
        coll.count() shouldBe 0
    }

    "batchInsert results should be reloadable" {

        val coll = database.testPersons.apply {
            removeAll()
        }

        val items = listOf(
            New(JonBonJovi),
            New(EdgarAllanPoe),
        )

        val results = coll.batchInsert(items)

        val reloaded = coll.findAll().toList().sortedBy { it.value.name }

        assertSoftly {
            reloaded.size shouldBe 2
            reloaded[0].value.name shouldBe "Edgar Poe"
            reloaded[1].value.name shouldBe "Jon Jovi"
        }
    }
})
