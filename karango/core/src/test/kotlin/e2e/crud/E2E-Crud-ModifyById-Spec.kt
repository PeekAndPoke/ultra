package io.peekandpoke.karango.e2e.crud

import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.peekandpoke.karango.e2e.database
import io.peekandpoke.karango.testdomain.testPersons
import io.peekandpoke.ultra.vault.value

@Suppress("ClassName")
class `E2E-Crud-ModifyById-Spec` : StringSpec({

    "modifyById should modify and return the updated document" {

        val coll = database.testPersons.apply {
            removeAll()
        }

        val jon = coll.insert(JonBonJovi)

        val modified = coll.modifyById(jon._id) { person ->
            person.copy(name = "Jon Modified")
        }

        assertSoftly {
            modified shouldNotBe null
            modified!!.value.name shouldBe "Jon Modified"
            modified._id shouldBe jon._id
        }

        // verify the change persisted
        val reloaded = coll.findById(jon._id)
        reloaded!!.value.name shouldBe "Jon Modified"
    }

    "modifyById with non-existing id should return null" {

        val coll = database.testPersons.apply {
            removeAll()
        }

        val result = coll.modifyById("non-existing-id") { person ->
            person.copy(name = "Should not happen")
        }

        result.shouldBeNull()
    }

    "modifyByIdWhen should modify when condition is true" {

        val coll = database.testPersons.apply {
            removeAll()
        }

        val jon = coll.insert(JonBonJovi)

        val modified = coll.modifyByIdWhen(
            id = jon._id,
            condition = { it.value.name == "Jon Jovi" },
            modify = { it.copy(name = "Jon Conditional") }
        )

        assertSoftly {
            modified shouldNotBe null
            modified!!.value.name shouldBe "Jon Conditional"
        }
    }

    "modifyByIdWhen should return null when condition is false" {

        val coll = database.testPersons.apply {
            removeAll()
        }

        val jon = coll.insert(JonBonJovi)

        val result = coll.modifyByIdWhen(
            id = jon._id,
            condition = { it.value.name == "Wrong Name" },
            modify = { it.copy(name = "Should not happen") }
        )

        result.shouldBeNull()

        // verify the original was not modified
        val reloaded = coll.findById(jon._id)
        reloaded!!.value.name shouldBe "Jon Jovi"
    }

    "modifyByIdWhen with non-existing id should return null" {

        val coll = database.testPersons.apply {
            removeAll()
        }

        val result = coll.modifyByIdWhen(
            id = "non-existing-id",
            condition = { true },
            modify = { it.copy(name = "Should not happen") }
        )

        result.shouldBeNull()
    }
})
