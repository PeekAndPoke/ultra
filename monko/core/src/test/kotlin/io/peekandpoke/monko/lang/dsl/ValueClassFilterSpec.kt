package io.peekandpoke.monko.lang.dsl

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.peekandpoke.monko.lang.MongoExpression
import io.peekandpoke.monko.lang.MongoIterableExpr
import io.peekandpoke.monko.lang.MongoPrinter
import io.peekandpoke.monko.lang.MongoPropertyPath
import io.peekandpoke.ultra.reflection.TypeRef
import io.peekandpoke.ultra.reflection.kType

/**
 * Regression: comparison/membership filters must accept a `@JvmInline value class` value and serialize
 * it to the UNDERLYING scalar in the BSON — matching how the value class is stored (slumber unwraps it).
 *
 * The generated KSP accessor for a value-class field is `append<VcId, VcId>("field")`, so `path eq
 * VcId(...)` is what user code writes. Without unwrapping, `Filters.*` hands the boxed value class to
 * the driver's codec registry, which has no codec for it → `CodecConfigurationException` at
 * `.toBsonDocument()`. See value_class.kt.
 */
class ValueClassFilterSpec : FreeSpec() {

    @JvmInline
    value class VcRealmId(val value: String)

    @JvmInline
    value class VcCount(val value: Int) : Comparable<VcCount> {
        override fun compareTo(other: VcCount): Int = value.compareTo(other.value)
    }

    /** value class over a value class — must unwrap all the way down to the scalar. */
    @JvmInline
    value class VcWrapped(val value: VcRealmId)

    data class Rec(
        val realm: VcRealmId,
        val count: VcCount,
        val wrapped: VcWrapped,
    )

    private val root = object : MongoExpression<List<Rec>> {
        override fun getType(): TypeRef<List<Rec>> = kType()
        override fun print(p: MongoPrinter) {
            p.name("root")
        }
    }
    private val r = MongoIterableExpr<Rec>("r", root)

    private val realmPath: MongoPropertyPath<VcRealmId, VcRealmId>
        get() = MongoPropertyPath.start(r).append<VcRealmId, VcRealmId>("realm")

    private val countPath: MongoPropertyPath<VcCount, VcCount>
        get() = MongoPropertyPath.start(r).append<VcCount, VcCount>("count")

    private val wrappedPath: MongoPropertyPath<VcWrapped, VcWrapped>
        get() = MongoPropertyPath.start(r).append<VcWrapped, VcWrapped>("wrapped")

    init {
        "eq on a String-backed value class serializes to the underlying string" {
            val json = (realmPath eq VcRealmId("b2b")).toBsonDocument().toJson()

            json shouldContain "realm"
            json shouldContain "b2b"
            // must NOT wrap it as an object { "value": ... }
            json shouldNotContain "value"
        }

        "eq on an Int-backed value class serializes to the underlying number" {
            val json = (countPath eq VcCount(42)).toBsonDocument().toJson()

            json shouldContain "count"
            json shouldContain "42"
        }

        "eq on a value class over a value class unwraps recursively to the scalar" {
            val json = (wrappedPath eq VcWrapped(VcRealmId("nested"))).toBsonDocument().toJson()

            json shouldContain "wrapped"
            json shouldContain "nested"
            json shouldNotContain "value"
        }

        "ne on a value class serializes to the underlying scalar" {
            val json = (realmPath ne VcRealmId("b2b")).toBsonDocument().toJson()

            json shouldContain "\$ne"
            json shouldContain "b2b"
        }

        "ordered comparison (gt) on an Int-backed value class serializes to the number" {
            val json = (countPath gt VcCount(10)).toBsonDocument().toJson()

            json shouldContain "\$gt"
            json shouldContain "10"
        }

        "isIn on value classes serializes each element to its underlying scalar" {
            val json = (realmPath isIn listOf(VcRealmId("b2b"), VcRealmId("ops"))).toBsonDocument().toJson()

            json shouldContain "\$in"
            json shouldContain "b2b"
            json shouldContain "ops"
        }

        "nin on value classes serializes each element to its underlying scalar" {
            val json = (realmPath nin listOf(VcRealmId("b2b"))).toBsonDocument().toJson()

            json shouldContain "\$nin"
            json shouldContain "b2b"
        }

        "plain (non value-class) values are unaffected" {
            val namePath = MongoPropertyPath.start(r).append<String, String>("name")
            val json = (namePath eq "Alice").toBsonDocument().toJson()

            json shouldContain "name"
            json shouldContain "Alice"
        }
    }
}
