package io.peekandpoke.karango.e2e.crud

import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.peekandpoke.karango.e2e.database
import io.peekandpoke.karango.testdomain.testPersons

@Suppress("ClassName")
class `E2E-Crud-FindByIds-Spec` : StringSpec({

    "findByIds with varargs should return matching documents" {

        val coll = database.testPersons.apply {
            removeAll()
        }

        val jon = coll.insert(JonBonJovi)
        val edgar = coll.insert(EdgarAllanPoe)
        coll.insert(HeinzRudolfKunze)

        val found = coll.findByIds(jon._id, edgar._id).toList()

        assertSoftly {
            found.size shouldBe 2
            found.map { it.value.name }.toSet() shouldBe setOf("Jon Jovi", "Edgar Poe")
        }
    }

    "findByIds with collection should return matching documents" {

        val coll = database.testPersons.apply {
            removeAll()
        }

        val jon = coll.insert(JonBonJovi)
        val edgar = coll.insert(EdgarAllanPoe)

        val found = coll.findByIds(listOf(jon._id, edgar._id)).toList()

        assertSoftly {
            found.size shouldBe 2
            found.map { it.value.name }.toSet() shouldBe setOf("Jon Jovi", "Edgar Poe")
        }
    }

    "findByIds with non-existing ids should return empty" {

        val coll = database.testPersons.apply {
            removeAll()
        }

        coll.insert(JonBonJovi)

        val found = coll.findByIds("non-existing-id-1", "non-existing-id-2").toList()

        found.size shouldBe 0
    }

    "findByIds with keys should work" {

        val coll = database.testPersons.apply {
            removeAll()
        }

        val jon = coll.insert(JonBonJovi)

        val found = coll.findByIds(jon._key).toList()

        assertSoftly {
            found.size shouldBe 1
            found[0].value.name shouldBe "Jon Jovi"
        }
    }

    "findById should return a single document" {

        val coll = database.testPersons.apply {
            removeAll()
        }

        val jon = coll.insert(JonBonJovi)
        val found = coll.findById(jon._id)

        assertSoftly {
            found shouldNotBe null
            found!!.value.name shouldBe "Jon Jovi"
        }
    }

    "findById with non-existing id should return null" {

        val coll = database.testPersons.apply {
            removeAll()
        }

        val found = coll.findById("non-existing-id")

        found.shouldBeNull()
    }
})
