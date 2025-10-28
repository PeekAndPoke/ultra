package de.peekandpoke.karango.e2e.crud

import de.peekandpoke.karango.aql.FOR
import de.peekandpoke.karango.aql.INSERT
import de.peekandpoke.karango.aql.LET
import de.peekandpoke.karango.e2e.database
import de.peekandpoke.karango.testdomain.testPersons
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain

@Suppress("ClassName")
class `E2E-Crud-Insert-Spec` : StringSpec({

    "Inserting documents via save() must work" {

        // get coll and clear all entries
        val coll = database.testPersons.apply {
            removeAll()
        }

        // create some entries
        val jonSaved = coll.insert(JonBonJovi)
        val edgarSaved = coll.insert(EdgarAllanPoe)

        // reload them
        val reloaded = coll.findAll().toList().sortedBy { it.value.name }
        val (edgarReloaded, jonReloaded) = reloaded

        assertSoftly {

            // check that things where saved correctly
            jonSaved._id shouldNotBe null
            jonSaved._key shouldNotBe null
            jonSaved.value.name shouldBe JonBonJovi.name
            jonSaved.value.details shouldBe JonBonJovi.details

            edgarSaved._id shouldNotBe null
            edgarSaved._key shouldNotBe null
            edgarSaved.value.name shouldBe EdgarAllanPoe.name
            edgarSaved.value.details shouldBe EdgarAllanPoe.details

            // check that things where reloaded correctly
            reloaded.size shouldBe 2

            jonReloaded._id shouldNotBe null
            jonReloaded._key shouldNotBe null
            jonReloaded.value.name shouldBe JonBonJovi.name
            jonReloaded.value.details shouldBe JonBonJovi.details

            edgarReloaded._id shouldNotBe null
            edgarReloaded._key shouldNotBe null
            edgarReloaded.value.name shouldBe EdgarAllanPoe.name
            edgarReloaded.value.details shouldBe EdgarAllanPoe.details
        }
    }

    "Inserting documents via query must work" {

        // get coll and clear all entries
        val coll = database.testPersons.apply {
            removeAll()
        }

        // insert data
        val (jonSaved, edgarSaved) = coll.findList {
            val docs = LET("docs", listOf(JonBonJovi, EdgarAllanPoe))

            FOR(docs) { doc ->
                INSERT(doc) INTO database.testPersons
            }
        }

        // reload them
        val reloaded = coll.findAll().toList().sortedBy { it.value.name }
        val (edgarReloaded, jonReloaded) = reloaded

        assertSoftly {

            // check that things where saved correctly
            jonSaved._id shouldNotBe null
            jonSaved._key shouldNotBe null
            jonSaved.value.name shouldBe JonBonJovi.name
            jonSaved.value.details shouldBe JonBonJovi.details

            edgarSaved._id shouldNotBe null
            edgarSaved._key shouldNotBe null
            edgarSaved.value.name shouldBe EdgarAllanPoe.name
            edgarSaved.value.details shouldBe EdgarAllanPoe.details

            // check that things where reloaded correctly
            reloaded.size shouldBe 2

            jonReloaded._id shouldNotBe null
            jonReloaded._key shouldNotBe null
            jonReloaded.value.name shouldBe JonBonJovi.name
            jonReloaded.value.details shouldBe JonBonJovi.details

            edgarReloaded._id shouldNotBe null
            edgarReloaded._key shouldNotBe null
            edgarReloaded.value.name shouldBe EdgarAllanPoe.name
            edgarReloaded.value.details shouldBe EdgarAllanPoe.details
        }
    }

    "Inserting an already existing key must fail" {

        // get coll and clear all entries
        val coll = database.testPersons.apply {
            removeAll()
        }

        // create some entries
        val jonSaved = coll.insert(JonBonJovi)

        assertSoftly {
            // trying to save jon again with the same key must fail
            // TODO: we need a more granular exception
            val error = shouldThrow<Throwable> {
                coll.insert(jonSaved._key, jonSaved.value)
            }

            error.message shouldContain ("unique constraint violated")
            error.message shouldContain ("_key")
        }
    }
})
