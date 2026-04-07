package io.peekandpoke.monko.vault

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.maps.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.peekandpoke.monko.vault.vault.MongoTypedQuery
import io.peekandpoke.ultra.reflection.kType

class MongoTypedQuerySpec : FreeSpec() {

    init {
        "MongoTypedQuery" - {

            "of with defaults" {
                val query = MongoTypedQuery.of(type = kType<String>())

                query.query shouldBe ""
                query.vars.shouldBeEmpty()
            }

            "of with custom query and vars" {
                val query = MongoTypedQuery.of(
                    type = kType<String>(),
                    query = "some query",
                    vars = mapOf("key" to "value"),
                )

                query.query shouldBe "some query"
                query.vars shouldBe mapOf("key" to "value")
            }

            "of creates valid root expression" {
                val query = MongoTypedQuery.of(type = kType<String>())

                // root should be an EmptyExpression that returns a list type
                query.root.getType() shouldBe kType<List<String>>()
            }

            "structural equality on query and vars" {
                val query1 = MongoTypedQuery.of(
                    type = kType<String>(),
                    query = "test",
                    vars = mapOf("a" to 1),
                )
                val query2 = MongoTypedQuery.of(
                    type = kType<String>(),
                    query = "test",
                    vars = mapOf("a" to 1),
                )

                query1.query shouldBe query2.query
                query1.vars shouldBe query2.vars
            }

            "data class inequality on query" {
                val query1 = MongoTypedQuery.of(
                    type = kType<String>(),
                    query = "test1",
                )
                val query2 = MongoTypedQuery.of(
                    type = kType<String>(),
                    query = "test2",
                )

                (query1 == query2) shouldBe false
            }

            "data class inequality on vars" {
                val query1 = MongoTypedQuery.of(
                    type = kType<String>(),
                    vars = mapOf("a" to 1),
                )
                val query2 = MongoTypedQuery.of(
                    type = kType<String>(),
                    vars = mapOf("a" to 2),
                )

                (query1 == query2) shouldBe false
            }
        }
    }
}
