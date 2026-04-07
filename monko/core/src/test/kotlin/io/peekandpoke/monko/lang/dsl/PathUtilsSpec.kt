package io.peekandpoke.monko.lang.dsl

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.monko.lang.MongoExpression
import io.peekandpoke.monko.lang.MongoIterableExpr
import io.peekandpoke.monko.lang.MongoPrinter
import io.peekandpoke.monko.lang.MongoPropertyPath
import io.peekandpoke.ultra.reflection.TypeRef
import io.peekandpoke.ultra.reflection.kType

class PathUtilsSpec : FreeSpec() {

    data class Address(
        val city: String,
        val zip: String,
    )

    data class Person(
        val name: String,
        val address: Address,
    )

    private val root = object : MongoExpression<List<Person>> {
        override fun getType(): TypeRef<List<Person>> = kType()
        override fun print(p: MongoPrinter) {
            p.name("root")
        }
    }
    private val r = MongoIterableExpr<Person>("r", root)

    init {
        "toFieldPath" - {

            "should return simple field name" {
                val path = MongoPropertyPath.start(r).append<String, String>("name")

                path.toFieldPath() shouldBe "name"
            }

            "should return dot-separated nested field path" {
                val path = MongoPropertyPath.start(r)
                    .append<Address, Address>("address")
                    .property<String>("city")

                path.toFieldPath() shouldBe "address.city"
            }

            "should handle deeply nested paths" {
                val path = MongoPropertyPath.start(r)
                    .append<Address, Address>("address")
                    .property<String>("zip")

                path.toFieldPath() shouldBe "address.zip"
            }

            "should return empty string for root-only path" {
                val path = MongoPropertyPath.start(r)

                path.toFieldPath() shouldBe ""
            }
        }
    }
}
