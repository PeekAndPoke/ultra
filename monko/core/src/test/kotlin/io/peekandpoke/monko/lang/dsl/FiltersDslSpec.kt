package io.peekandpoke.monko.lang.dsl

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.string.shouldContain
import io.peekandpoke.monko.lang.MongoExpression
import io.peekandpoke.monko.lang.MongoIterableExpr
import io.peekandpoke.monko.lang.MongoPrinter
import io.peekandpoke.monko.lang.MongoPropertyPath
import io.peekandpoke.ultra.reflection.TypeRef
import io.peekandpoke.ultra.reflection.kType

class FiltersDslSpec : FreeSpec() {

    data class Person(
        val name: String,
        val age: Int,
        val email: String,
        val tags: List<String>,
        val score: Double,
    )

    private val root = object : MongoExpression<List<Person>> {
        override fun getType(): TypeRef<List<Person>> = kType()
        override fun print(p: MongoPrinter) {
            p.name("root")
        }
    }
    private val r = MongoIterableExpr<Person>("r", root)

    private val namePath: MongoPropertyPath<String, String>
        get() = MongoPropertyPath.start(r).append<String, String>("name")

    private val agePath: MongoPropertyPath<Int, Int>
        get() = MongoPropertyPath.start(r).append<Int, Int>("age")

    private val emailPath: MongoPropertyPath<String, String>
        get() = MongoPropertyPath.start(r).append<String, String>("email")

    private val scorePath: MongoPropertyPath<Double, Double>
        get() = MongoPropertyPath.start(r).append<Double, Double>("score")

    private val tagsPath: MongoPropertyPath<List<String>, List<String>>
        get() = MongoPropertyPath.start(r).append<List<String>, List<String>>("tags")

    init {
        "Filter DSL" - {

            "eq" - {

                "should create an equality filter" {
                    val bson = namePath eq "Alice"
                    val json = bson.toBsonDocument().toJson()

                    json shouldContain "name"
                    json shouldContain "Alice"
                }

                "should handle integer values" {
                    val bson = agePath eq 30
                    val json = bson.toBsonDocument().toJson()

                    json shouldContain "age"
                    json shouldContain "30"
                }
            }

            "ne" - {

                "should create a not-equal filter" {
                    val bson = namePath ne "Bob"
                    val json = bson.toBsonDocument().toJson()

                    json shouldContain "name"
                    json shouldContain "\$ne"
                    json shouldContain "Bob"
                }
            }

            "gt" - {

                "should create a greater-than filter" {
                    val bson = agePath gt 18
                    val json = bson.toBsonDocument().toJson()

                    json shouldContain "age"
                    json shouldContain "\$gt"
                    json shouldContain "18"
                }
            }

            "gte" - {

                "should create a greater-than-or-equal filter" {
                    val bson = agePath gte 21
                    val json = bson.toBsonDocument().toJson()

                    json shouldContain "age"
                    json shouldContain "\$gte"
                    json shouldContain "21"
                }
            }

            "lt" - {

                "should create a less-than filter" {
                    val bson = agePath lt 65
                    val json = bson.toBsonDocument().toJson()

                    json shouldContain "age"
                    json shouldContain "\$lt"
                    json shouldContain "65"
                }
            }

            "lte" - {

                "should create a less-than-or-equal filter" {
                    val bson = agePath lte 100
                    val json = bson.toBsonDocument().toJson()

                    json shouldContain "age"
                    json shouldContain "\$lte"
                    json shouldContain "100"
                }
            }

            "isIn" - {

                "should create an in-filter" {
                    val bson = namePath isIn listOf("Alice", "Bob", "Charlie")
                    val json = bson.toBsonDocument().toJson()

                    json shouldContain "name"
                    json shouldContain "\$in"
                    json shouldContain "Alice"
                    json shouldContain "Bob"
                    json shouldContain "Charlie"
                }

                "should handle empty collection" {
                    val bson = namePath isIn emptyList()
                    val json = bson.toBsonDocument().toJson()

                    json shouldContain "\$in"
                }
            }

            "nin" - {

                "should create a not-in filter" {
                    val bson = namePath nin listOf("Excluded")
                    val json = bson.toBsonDocument().toJson()

                    json shouldContain "name"
                    json shouldContain "\$nin"
                    json shouldContain "Excluded"
                }
            }

            "regex" - {

                "should create a regex filter from string pattern" {
                    val bson = namePath regex "^Al"
                    val json = bson.toBsonDocument().toJson()

                    json shouldContain "name"
                    json shouldContain "\$regularExpression"
                    json shouldContain "^Al"
                }

                "should create a regex filter from Regex" {
                    val bson = namePath regex Regex("^Al.*", RegexOption.IGNORE_CASE)
                    val json = bson.toBsonDocument().toJson()

                    json shouldContain "name"
                    json shouldContain "\$regularExpression"
                }
            }

            "exists" - {

                "should create an exists-true filter" {
                    val bson = namePath.exists()
                    val json = bson.toBsonDocument().toJson()

                    json shouldContain "name"
                    json shouldContain "\$exists"
                    json shouldContain "true"
                }

                "should create an exists-false filter" {
                    val bson = namePath.exists(false)
                    val json = bson.toBsonDocument().toJson()

                    json shouldContain "name"
                    json shouldContain "\$exists"
                    json shouldContain "false"
                }
            }

            "size" - {

                "should create a size filter" {
                    val bson = tagsPath.size(3)
                    val json = bson.toBsonDocument().toJson()

                    json shouldContain "tags"
                    json shouldContain "\$size"
                    json shouldContain "3"
                }
            }

            "and" - {

                "should combine filters with AND" {
                    val bson = and(
                        namePath eq "Alice",
                        agePath gt 18,
                    )
                    val json = bson.toBsonDocument().toJson()

                    json shouldContain "\$and"
                    json shouldContain "name"
                    json shouldContain "age"
                }

                "should combine filters from list" {
                    val filters = listOf(
                        namePath eq "Alice",
                        agePath gt 18,
                    )
                    val bson = and(filters)
                    val json = bson.toBsonDocument().toJson()

                    json shouldContain "\$and"
                }
            }

            "or" - {

                "should combine filters with OR" {
                    val bson = or(
                        namePath eq "Alice",
                        namePath eq "Bob",
                    )
                    val json = bson.toBsonDocument().toJson()

                    json shouldContain "\$or"
                    json shouldContain "Alice"
                    json shouldContain "Bob"
                }

                "should combine filters from list" {
                    val filters = listOf(
                        namePath eq "Alice",
                        namePath eq "Bob",
                    )
                    val bson = or(filters)
                    val json = bson.toBsonDocument().toJson()

                    json shouldContain "\$or"
                }
            }

            "not" - {

                "should negate a filter" {
                    val bson = not(namePath eq "Alice")
                    val json = bson.toBsonDocument().toJson()

                    json shouldContain "\$not"
                }
            }

            "nor" - {

                "should combine filters with NOR" {
                    val bson = nor(
                        namePath eq "Alice",
                        agePath lt 18,
                    )
                    val json = bson.toBsonDocument().toJson()

                    json shouldContain "\$nor"
                }
            }
        }
    }
}
