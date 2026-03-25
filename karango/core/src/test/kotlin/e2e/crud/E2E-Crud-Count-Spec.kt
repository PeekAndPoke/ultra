package io.peekandpoke.karango.e2e.crud

import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.karango.e2e.database
import io.peekandpoke.karango.testdomain.testPersons

@Suppress("ClassName")
class `E2E-Crud-Count-Spec` : StringSpec({

    "count() on empty collection should return 0" {

        val coll = database.testPersons.apply {
            removeAll()
        }

        coll.count() shouldBe 0
    }

    "count() after inserts should return correct count" {

        val coll = database.testPersons.apply {
            removeAll()
        }

        coll.insert(JonBonJovi)
        coll.insert(EdgarAllanPoe)

        coll.count() shouldBe 2
    }

    "count() after insert and remove should return correct count" {

        val coll = database.testPersons.apply {
            removeAll()
        }

        val jon = coll.insert(JonBonJovi)
        coll.insert(EdgarAllanPoe)

        coll.remove(jon)

        coll.count() shouldBe 1
    }

    "count() after removeAll should return 0" {

        val coll = database.testPersons.apply {
            removeAll()
        }

        coll.insert(JonBonJovi)
        coll.insert(EdgarAllanPoe)
        coll.insert(HeinzRudolfKunze)

        assertSoftly {
            coll.count() shouldBe 3

            coll.removeAll()

            coll.count() shouldBe 0
        }
    }
})
