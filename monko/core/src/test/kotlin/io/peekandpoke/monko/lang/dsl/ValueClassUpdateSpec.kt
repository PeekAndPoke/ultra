package io.peekandpoke.monko.lang.dsl

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.string.shouldContain
import io.peekandpoke.monko.lang.MongoExpression
import io.peekandpoke.monko.lang.MongoIterableExpr
import io.peekandpoke.monko.lang.MongoPrinter
import io.peekandpoke.monko.lang.MongoPropertyPath
import io.peekandpoke.ultra.reflection.TypeRef
import io.peekandpoke.ultra.reflection.kType

/**
 * Regression: update operators must reduce a `@JvmInline value class` value to its underlying scalar,
 * exactly like the comparison filters — update values also bypass slumber (`MonkoDriver.updateMany`
 * applies the raw `Bson`), so without unwrapping the driver's codec registry throws. See value_class.kt.
 */
class ValueClassUpdateSpec : FreeSpec() {

    @JvmInline
    value class VcRealmId(val value: String)

    /** A value class over a NON-scalar backing — unsupported, must be rejected loudly. */
    @JvmInline
    value class VcNested(val value: Payload)

    data class Payload(val a: String)

    data class Rec(
        val realm: VcRealmId,
        val realms: List<VcRealmId>,
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

    private val realmsPath: MongoPropertyPath<List<VcRealmId>, List<VcRealmId>>
        get() = MongoPropertyPath.start(r).append<List<VcRealmId>, List<VcRealmId>>("realms")

    init {
        "setTo on a value-class field serializes to the underlying scalar" {
            val json = (realmPath setTo VcRealmId("b2b")).toBsonDocument().toJson()

            json shouldContain "\$set"
            json shouldContain "realm"
            json shouldContain "b2b"
        }

        "addToSet on an array of value classes serializes the element to its scalar" {
            val json = (realmsPath addToSet VcRealmId("ops")).toBsonDocument().toJson()

            json shouldContain "\$addToSet"
            json shouldContain "ops"
        }

        "pull on an array of value classes serializes the element to its scalar" {
            val json = (realmsPath pull VcRealmId("b2b")).toBsonDocument().toJson()

            json shouldContain "\$pull"
            json shouldContain "b2b"
        }

        "a non-scalar-backed value class is rejected loudly (not silently mis-encoded)" {
            val nestedPath = MongoPropertyPath.start(r).append<VcNested, VcNested>("nested")

            val ex = shouldThrow<IllegalArgumentException> {
                nestedPath setTo VcNested(Payload("x"))
            }
            ex.message shouldContain "scalar-backed"
        }
    }
}
