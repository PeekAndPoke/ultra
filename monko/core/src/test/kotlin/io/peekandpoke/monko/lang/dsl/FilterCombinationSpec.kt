package io.peekandpoke.monko.lang.dsl

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.string.shouldContain
import io.peekandpoke.monko.lang.MongoExpression
import io.peekandpoke.monko.lang.MongoIterableExpr
import io.peekandpoke.monko.lang.MongoPrinter
import io.peekandpoke.monko.lang.MongoPropertyPath
import io.peekandpoke.ultra.reflection.TypeRef
import io.peekandpoke.ultra.reflection.kType

class FilterCombinationSpec : FreeSpec() {

    data class Product(
        val name: String,
        val price: Double,
        val category: String,
        val inStock: Boolean,
        val tags: List<String>,
        val rating: Int,
    )

    private val root = object : MongoExpression<List<Product>> {
        override fun getType(): TypeRef<List<Product>> = kType()
        override fun print(p: MongoPrinter) {
            p.name("root")
        }
    }
    private val r = MongoIterableExpr<Product>("r", root)

    private val namePath: MongoPropertyPath<String, String>
        get() = MongoPropertyPath.start(r).append<String, String>("name")

    private val pricePath: MongoPropertyPath<Double, Double>
        get() = MongoPropertyPath.start(r).append<Double, Double>("price")

    private val categoryPath: MongoPropertyPath<String, String>
        get() = MongoPropertyPath.start(r).append<String, String>("category")

    private val ratingPath: MongoPropertyPath<Int, Int>
        get() = MongoPropertyPath.start(r).append<Int, Int>("rating")

    private val tagsPath: MongoPropertyPath<List<String>, List<String>>
        get() = MongoPropertyPath.start(r).append<List<String>, List<String>>("tags")

    init {
        "Complex filter combinations" - {

            "AND with equality and range" {
                val bson = and(
                    categoryPath eq "electronics",
                    pricePath gte 10.0,
                    pricePath lte 100.0,
                )
                val json = bson.toBsonDocument().toJson()

                json shouldContain "\$and"
                json shouldContain "category"
                json shouldContain "electronics"
                json shouldContain "\$gte"
                json shouldContain "\$lte"
            }

            "OR with multiple equalities" {
                val bson = or(
                    categoryPath eq "electronics",
                    categoryPath eq "books",
                    categoryPath eq "toys",
                )
                val json = bson.toBsonDocument().toJson()

                json shouldContain "\$or"
                json shouldContain "electronics"
                json shouldContain "books"
                json shouldContain "toys"
            }

            "nested AND inside OR" {
                val bson = or(
                    and(
                        categoryPath eq "electronics",
                        pricePath lt 50.0,
                    ),
                    and(
                        categoryPath eq "books",
                        ratingPath gte 4,
                    ),
                )
                val json = bson.toBsonDocument().toJson()

                json shouldContain "\$or"
                json shouldContain "\$and"
                json shouldContain "electronics"
                json shouldContain "books"
            }

            "nested OR inside AND" {
                val bson = and(
                    ratingPath gte 3,
                    or(
                        categoryPath eq "electronics",
                        categoryPath eq "books",
                    ),
                )
                val json = bson.toBsonDocument().toJson()

                json shouldContain "\$and"
                json shouldContain "\$or"
            }

            "NOT with equality" {
                val bson = not(categoryPath eq "deprecated")
                val json = bson.toBsonDocument().toJson()

                json shouldContain "\$not"
            }

            "NOR with multiple conditions" {
                val bson = nor(
                    pricePath lt 0.0,
                    ratingPath lt 0,
                )
                val json = bson.toBsonDocument().toJson()

                json shouldContain "\$nor"
            }

            "combination of isIn with range" {
                val bson = and(
                    categoryPath isIn listOf("electronics", "books"),
                    pricePath gt 0.0,
                    ratingPath gte 1,
                )
                val json = bson.toBsonDocument().toJson()

                json shouldContain "\$in"
                json shouldContain "\$gt"
                json shouldContain "\$gte"
            }

            "regex combined with other filters" {
                val bson = and(
                    namePath regex "^Pro.*",
                    pricePath lte 999.99,
                )
                val json = bson.toBsonDocument().toJson()

                json shouldContain "\$regularExpression"
                json shouldContain "^Pro"
                json shouldContain "\$lte"
            }

            "exists combined with equality" {
                val bson = and(
                    namePath.exists(),
                    categoryPath eq "special",
                )
                val json = bson.toBsonDocument().toJson()

                json shouldContain "\$exists"
                json shouldContain "special"
            }

            "deeply nested logical operators" {
                val bson = and(
                    or(
                        and(
                            categoryPath eq "A",
                            pricePath lt 10.0,
                        ),
                        and(
                            categoryPath eq "B",
                            pricePath lt 20.0,
                        ),
                    ),
                    ratingPath gte 3,
                )
                val json = bson.toBsonDocument().toJson()

                json shouldContain "\$and"
                json shouldContain "\$or"
                json shouldContain "\"A\""
                json shouldContain "\"B\""
            }
        }
    }
}
