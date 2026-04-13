package io.peekandpoke.karango.e2e.crud

import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.karango.aql.FOR
import io.peekandpoke.karango.aql.REMOVE
import io.peekandpoke.karango.e2e.database
import io.peekandpoke.karango.testdomain.testPersons
import io.peekandpoke.ultra.vault.value

@Suppress("ClassName")
class `E2E-Crud-Remove-Query-Spec` : StringSpec({

    "REMOVE by stored entity should work" {

        val coll = database.testPersons.apply {
            removeAll()
        }

        val jon = coll.insert(JonBonJovi)
        val edgar = coll.insert(EdgarAllanPoe)

        // Remove jon via REMOVE query
        coll.findList {
            REMOVE(jon) IN database.testPersons
        }

        val remaining = coll.findAll().toList()

        assertSoftly {
            remaining.size shouldBe 1
            remaining[0].value.name shouldBe "Edgar Poe"
        }
    }

    "REMOVE by string key should work" {

        val coll = database.testPersons.apply {
            removeAll()
        }

        val jon = coll.insert(JonBonJovi)
        coll.insert(EdgarAllanPoe)

        coll.findList {
            REMOVE(jon._key) IN database.testPersons
        }

        val remaining = coll.findAll().toList()

        assertSoftly {
            remaining.size shouldBe 1
            remaining[0].value.name shouldBe "Edgar Poe"
        }
    }

    "REMOVE via FOR loop should work" {

        val coll = database.testPersons.apply {
            removeAll()
        }

        coll.insert(JonBonJovi)
        coll.insert(EdgarAllanPoe)
        coll.insert(HeinzRudolfKunze)

        // Remove all via FOR loop
        coll.findList {
            FOR(database.testPersons) { doc ->
                REMOVE(doc) IN database.testPersons
            }
        }

        coll.count() shouldBe 0
    }
})
