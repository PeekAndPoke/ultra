package io.peekandpoke.monko.lang.dsl

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.string.shouldContain
import io.peekandpoke.monko.lang.MongoExpression
import io.peekandpoke.monko.lang.MongoIterableExpr
import io.peekandpoke.monko.lang.MongoPrinter
import io.peekandpoke.monko.lang.MongoPropertyPath
import io.peekandpoke.ultra.reflection.TypeRef
import io.peekandpoke.ultra.reflection.kType

class SortsDslSpec : FreeSpec() {

    data class Person(
        val name: String,
        val age: Int,
        val createdAt: Long,
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

    private val createdAtPath: MongoPropertyPath<Long, Long>
        get() = MongoPropertyPath.start(r).append<Long, Long>("createdAt")

    init {
        "Sort DSL" - {

            "asc" - {

                "should create an ascending sort" {
                    val bson = namePath.asc
                    val json = bson.toBsonDocument().toJson()

                    json shouldContain "name"
                    json shouldContain "1"
                }
            }

            "desc" - {

                "should create a descending sort" {
                    val bson = namePath.desc
                    val json = bson.toBsonDocument().toJson()

                    json shouldContain "name"
                    json shouldContain "-1"
                }
            }

            "orderBy" - {

                "should combine multiple sorts" {
                    val bson = orderBy(namePath.asc, agePath.desc)
                    val json = bson.toBsonDocument().toJson()

                    json shouldContain "name"
                    json shouldContain "age"
                }

                "should combine ascending and descending" {
                    val bson = orderBy(
                        createdAtPath.desc,
                        namePath.asc,
                    )
                    val json = bson.toBsonDocument().toJson()

                    json shouldContain "createdAt"
                    json shouldContain "name"
                }

                "should handle single sort" {
                    val bson = orderBy(namePath.asc)
                    val json = bson.toBsonDocument().toJson()

                    json shouldContain "name"
                }
            }
        }
    }
}
