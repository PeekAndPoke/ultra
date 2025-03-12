package de.peekandpoke.karango.e2e.crud

import de.peekandpoke.karango.e2e.database
import de.peekandpoke.karango.testdomain.testPersons
import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

@Suppress("ClassName")
class `E2E-Crud-Remove-Spec` : StringSpec({

    "Removing an entity via remove(entity) must work" {

        // get coll and clear all entries
        val coll = database.testPersons.apply {
            removeAll()
        }

        // create some entries
        val jonSaved = coll.insert(JonBonJovi)
        val edgarSaved = coll.insert(EdgarAllanPoe)
        val heinzSaved = coll.insert(HeinzRudolfKunze)

        // remove on entry
        val removeResult = coll.remove(jonSaved)

        // reload all remaining
        val allReloaded = coll.findAll().toList().sortedBy { it.value.name }
        val (edgarReloaded, heinzReloaded) = allReloaded

        assertSoftly {

            // check the removed entity
            removeResult.count shouldBe 1

            // check reloaded
            allReloaded.size shouldBe 2

            edgarReloaded._id shouldBe edgarSaved._id
            heinzReloaded._id shouldBe heinzSaved._id
        }
    }

    "Removing an entity via remove(String) must work" {

        // get coll and clear all entries
        val coll = database.testPersons.apply {
            removeAll()
        }

        // create some entries
        val jonSaved = coll.insert(JonBonJovi)
        val edgarSaved = coll.insert(EdgarAllanPoe)
        val heinzSaved = coll.insert(HeinzRudolfKunze)

        // remove on entry
        val removeResult = coll.remove(jonSaved._id)

        // reload all remaining
        val allReloaded = coll.findAll().toList().sortedBy { it.value.name }
        val (edgarReloaded, heinzReloaded) = allReloaded

        assertSoftly {

            // check the removed entity
            removeResult.count shouldBe 1

            // check reloaded
            allReloaded.size shouldBe 2

            edgarReloaded._id shouldBe edgarSaved._id
            heinzReloaded._id shouldBe heinzSaved._id
        }
    }

    "Trying to delete a non-existing" {

        // get coll and clear all entries
        val coll = database.testPersons.apply {
            removeAll()
        }

        val removeResult = coll.remove("abc123")

        assertSoftly {
            removeResult.count shouldBe 0
        }
    }
})
