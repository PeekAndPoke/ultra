package io.peekandpoke.karango.e2e.crud

import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.karango.aql.DOCUMENT
import io.peekandpoke.karango.aql.FOR
import io.peekandpoke.karango.aql.LET
import io.peekandpoke.karango.aql.RETURN
import io.peekandpoke.karango.e2e.database
import io.peekandpoke.karango.testdomain.testPersons
import io.peekandpoke.ultra.vault.value

@Suppress("ClassName")
class `E2E-Crud-Document-Spec` : StringSpec({

    "DOCUMENT by collection and key should return the document" {

        val coll = database.testPersons.apply {
            removeAll()
        }

        val jon = coll.insert(JonBonJovi)

        val result = coll.findList {
            val doc = LET("doc", DOCUMENT(database.testPersons, jon._key))
            RETURN(doc)
        }

        assertSoftly {
            result.size shouldBe 1
            result[0].value.name shouldBe "Jon Jovi"
        }
    }

    "DOCUMENT by collection and multiple keys should return documents" {

        val coll = database.testPersons.apply {
            removeAll()
        }

        val jon = coll.insert(JonBonJovi)
        val edgar = coll.insert(EdgarAllanPoe)

        val result = coll.findList {
            FOR(DOCUMENT(database.testPersons, jon._key, edgar._key)) { doc ->
                RETURN(doc)
            }
        }

        assertSoftly {
            result.size shouldBe 2
            result.map { it.value.name }.toSet() shouldBe setOf("Jon Jovi", "Edgar Poe")
        }
    }

    "DOCUMENT by full id should return the document" {

        val coll = database.testPersons.apply {
            removeAll()
        }

        val jon = coll.insert(JonBonJovi)

        val result = coll.findList {
            val doc = LET("doc", DOCUMENT(database.testPersons, jon._id))
            RETURN(doc)
        }

        assertSoftly {
            result.size shouldBe 1
            result[0].value.name shouldBe "Jon Jovi"
        }
    }
})
