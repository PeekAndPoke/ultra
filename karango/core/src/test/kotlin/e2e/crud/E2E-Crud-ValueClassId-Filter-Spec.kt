package io.peekandpoke.karango.e2e.crud

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.peekandpoke.karango.aql.EQ
import io.peekandpoke.karango.aql.FOR
import io.peekandpoke.karango.aql.GT
import io.peekandpoke.karango.aql.RETURN
import io.peekandpoke.karango.e2e.database
import io.peekandpoke.karango.testdomain.TestRealmId
import io.peekandpoke.karango.testdomain.TestScore
import io.peekandpoke.karango.testdomain.TestVcRecord
import io.peekandpoke.karango.testdomain.realm
import io.peekandpoke.karango.testdomain.score
import io.peekandpoke.karango.testdomain.testVcRecords
import io.peekandpoke.ultra.vault.value

/**
 * Live round trip proving a `@JvmInline value class` id filters correctly through ArangoDB with NO
 * KSP change: the value-class field stores as its underlying scalar (slumber) and the `EQ`/`GT`
 * comparison value is slumbered into the bind var, so `FILTER(doc.realm EQ RealmId(...))` matches.
 */
@Suppress("ClassName")
class `E2E-Crud-ValueClassId-Filter-Spec` : StringSpec({

    "filter by a String-backed value-class id round-trips through ArangoDB" {
        val coll = database.testVcRecords.apply { removeAll() }

        coll.insert(TestVcRecord(realm = TestRealmId("b2b"), score = TestScore(10), label = "first"))
        coll.insert(TestVcRecord(realm = TestRealmId("ops"), score = TestScore(20), label = "second"))

        val hits = coll.findList {
            FOR(database.testVcRecords) { doc ->
                FILTER(doc.realm EQ TestRealmId("b2b"))
                RETURN(doc)
            }
        }

        hits.size shouldBe 1
        val hit = hits.single().value
        // the value-class fields survive the round trip as value classes, not raw scalars
        hit.realm shouldBe TestRealmId("b2b")
        hit.score shouldBe TestScore(10)
        hit.label shouldBe "first"
    }

    "filtering by a non-existent value-class id returns nothing" {
        val coll = database.testVcRecords.apply { removeAll() }

        coll.insert(TestVcRecord(realm = TestRealmId("b2b"), score = TestScore(10), label = "first"))

        val hits = coll.findList {
            FOR(database.testVcRecords) { doc ->
                FILTER(doc.realm EQ TestRealmId("does-not-exist"))
                RETURN(doc)
            }
        }

        hits.shouldContainExactlyInAnyOrder(emptyList())
    }

    "ordered filter (GT) on an Int-backed value class compares against the underlying number" {
        val coll = database.testVcRecords.apply { removeAll() }

        coll.insert(TestVcRecord(realm = TestRealmId("b2b"), score = TestScore(10), label = "low"))
        coll.insert(TestVcRecord(realm = TestRealmId("b2b"), score = TestScore(20), label = "mid"))
        coll.insert(TestVcRecord(realm = TestRealmId("b2b"), score = TestScore(30), label = "high"))

        val hits = coll.findList {
            FOR(database.testVcRecords) { doc ->
                FILTER(doc.score GT TestScore(15))
                RETURN(doc)
            }
        }

        hits.map { it.value.label } shouldContainExactlyInAnyOrder listOf("mid", "high")
    }
})
