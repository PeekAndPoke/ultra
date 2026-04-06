package io.peekandpoke.monko.lang.dsl

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.string.shouldContain
import io.peekandpoke.monko.lang.MongoExpression
import io.peekandpoke.monko.lang.MongoIterableExpr
import io.peekandpoke.monko.lang.MongoPrinter
import io.peekandpoke.monko.lang.MongoPropertyPath
import io.peekandpoke.ultra.reflection.TypeRef
import io.peekandpoke.ultra.reflection.kType

class UpdatesDslSpec : FreeSpec() {

    data class Item(
        val name: String,
        val count: Int,
        val score: Double,
        val longVal: Long,
        val tags: List<String>,
    )

    private val root = object : MongoExpression<List<Item>> {
        override fun getType(): TypeRef<List<Item>> = kType()
        override fun print(p: MongoPrinter) {
            p.name("root")
        }
    }
    private val r = MongoIterableExpr<Item>("r", root)

    private val namePath: MongoPropertyPath<String, String>
        get() = MongoPropertyPath.start(r).append<String, String>("name")

    private val countPath: MongoPropertyPath<Int, Int>
        get() = MongoPropertyPath.start(r).append<Int, Int>("count")

    private val scorePath: MongoPropertyPath<Double, Double>
        get() = MongoPropertyPath.start(r).append<Double, Double>("score")

    private val longValPath: MongoPropertyPath<Long, Long>
        get() = MongoPropertyPath.start(r).append<Long, Long>("longVal")

    private val tagsPath: MongoPropertyPath<List<String>, List<String>>
        get() = MongoPropertyPath.start(r).append<List<String>, List<String>>("tags")

    init {
        "Update DSL" - {

            "setTo" - {

                "should create a set update for string" {
                    val bson = namePath setTo "NewName"
                    val json = bson.toBsonDocument().toJson()

                    json shouldContain "\$set"
                    json shouldContain "name"
                    json shouldContain "NewName"
                }

                "should create a set update for int" {
                    val bson = countPath setTo 42
                    val json = bson.toBsonDocument().toJson()

                    json shouldContain "\$set"
                    json shouldContain "count"
                    json shouldContain "42"
                }
            }

            "unset" - {

                "should create an unset update" {
                    val bson = namePath.unset()
                    val json = bson.toBsonDocument().toJson()

                    json shouldContain "\$unset"
                    json shouldContain "name"
                }
            }

            "inc (Int)" - {

                "should create an increment update" {
                    val bson = countPath inc 5
                    val json = bson.toBsonDocument().toJson()

                    json shouldContain "\$inc"
                    json shouldContain "count"
                    json shouldContain "5"
                }

                "should support negative increment (decrement)" {
                    val bson = countPath inc -3
                    val json = bson.toBsonDocument().toJson()

                    json shouldContain "\$inc"
                    json shouldContain "count"
                    json shouldContain "-3"
                }
            }

            "inc (Long)" - {

                "should create a long increment update" {
                    val bson = longValPath inc 100L
                    val json = bson.toBsonDocument().toJson()

                    json shouldContain "\$inc"
                    json shouldContain "longVal"
                }
            }

            "inc (Double)" - {

                "should create a double increment update" {
                    val bson = scorePath inc 1.5
                    val json = bson.toBsonDocument().toJson()

                    json shouldContain "\$inc"
                    json shouldContain "score"
                }
            }

            "push" - {

                "should create a push update" {
                    val bson = tagsPath push "newTag"
                    val json = bson.toBsonDocument().toJson()

                    json shouldContain "\$push"
                    json shouldContain "tags"
                    json shouldContain "newTag"
                }
            }

            "pull" - {

                "should create a pull update" {
                    val bson = tagsPath pull "removeMe"
                    val json = bson.toBsonDocument().toJson()

                    json shouldContain "\$pull"
                    json shouldContain "tags"
                    json shouldContain "removeMe"
                }
            }

            "addToSet" - {

                "should create an addToSet update" {
                    val bson = tagsPath addToSet "uniqueTag"
                    val json = bson.toBsonDocument().toJson()

                    json shouldContain "\$addToSet"
                    json shouldContain "tags"
                    json shouldContain "uniqueTag"
                }
            }

            "combine" - {

                "should combine multiple update operations" {
                    val bson = combine(
                        namePath setTo "Updated",
                        countPath inc 1,
                    )
                    val json = bson.toBsonDocument().toJson()

                    json shouldContain "\$set"
                    json shouldContain "\$inc"
                    json shouldContain "name"
                    json shouldContain "count"
                }

                "should combine set and unset" {
                    val bson = combine(
                        namePath setTo "New",
                        countPath.unset(),
                    )
                    val json = bson.toBsonDocument().toJson()

                    json shouldContain "\$set"
                    json shouldContain "\$unset"
                }
            }
        }
    }
}
