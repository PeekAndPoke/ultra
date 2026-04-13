package io.peekandpoke.karango.e2e.crud

import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.peekandpoke.karango.aql.INTO
import io.peekandpoke.karango.aql.UPSERT
import io.peekandpoke.karango.aql.UPSERT_REPLACE
import io.peekandpoke.karango.e2e.database
import io.peekandpoke.karango.testdomain.TestPerson
import io.peekandpoke.karango.testdomain.TestPersonDetails
import io.peekandpoke.karango.testdomain.testPersons
import io.peekandpoke.ultra.vault.New
import io.peekandpoke.ultra.vault.Stored
import io.peekandpoke.ultra.vault.value

@Suppress("ClassName")
class `E2E-Crud-Upsert-Spec` : StringSpec({

    "UPSERT inserts a new document when it does not exist" {

        val coll = database.testPersons.apply {
            removeAll()
        }

        val person = New(JonBonJovi)

        val result = coll.findList {
            UPSERT(person) INTO database.testPersons
        }

        assertSoftly {
            result.size shouldBe 1
            result[0]._key shouldNotBe ""
            result[0].value.name shouldBe "Jon Jovi"
        }

        coll.count() shouldBe 1
    }

    "UPSERT updates an existing document" {

        val coll = database.testPersons.apply {
            removeAll()
        }

        // First insert
        val stored = coll.insert(JonBonJovi)

        // Now upsert with same key but different data
        val updated = Stored(
            value = TestPerson(
                name = "Jon Updated",
                details = TestPersonDetails(title = "Mr.", middleName = "Bon"),
                addresses = listOf(),
            ),
            _id = stored._id,
            _key = stored._key,
            _rev = stored._rev,
        )

        val result = coll.findList {
            UPSERT(updated) INTO database.testPersons
        }

        assertSoftly {
            result.size shouldBe 1
            result[0].value.name shouldBe "Jon Updated"
            result[0]._key shouldBe stored._key
        }

        coll.count() shouldBe 1
    }

    "UPSERT_REPLACE replaces an existing document entirely" {

        val coll = database.testPersons.apply {
            removeAll()
        }

        val stored = coll.insert(JonBonJovi)

        val replaced = Stored(
            value = TestPerson(
                name = "Jon Replaced",
                details = TestPersonDetails(middleName = "New"),
                addresses = listOf(),
            ),
            _id = stored._id,
            _key = stored._key,
            _rev = stored._rev,
        )

        val result = coll.findList {
            UPSERT_REPLACE(replaced) INTO database.testPersons
        }

        assertSoftly {
            result.size shouldBe 1
            result[0].value.name shouldBe "Jon Replaced"
            result[0].value.details.middleName shouldBe "New"
            result[0]._key shouldBe stored._key
        }

        coll.count() shouldBe 1
    }
})
