package io.peekandpoke.monko

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Sorts
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import org.bson.BsonDocument
import org.bson.types.ObjectId
import java.util.regex.Pattern

/**
 * What `MonkoDriver` hands the query profiler, and therefore what an insights record holds.
 *
 * The recorded text used to be [MonkoDriver.FindQueryBuilder.print] -- the filter WITH its values -- so a
 * session-token lookup put the live token into every insights record (review gate 2026-08-24, S1).
 */
class MonkoDriverPrintQuerySpec : FreeSpec() {

    companion object {
        private const val TOKEN = "a-live-session-token-4f9c2e"
        private const val COLLECTION = "system_auth_records"
    }

    private fun printed(block: MonkoDriver.FindQueryBuilder.() -> Unit): MonkoDriver.FindQueryBuilder.Printed =
        MonkoDriver.FindQueryBuilder().apply(block).printQuery(COLLECTION)

    init {
        "a token lookup records the field and a placeholder, and the token only in vars" {
            // The shape of MonkoAuthRecordsRepo.findByToken
            val result = printed {
                filter(Filters.and(Filters.eq("_type", "session"), Filters.eq("realm", "app"), Filters.eq("token", TOKEN)))
                limit(1)
            }

            withClue(result.query) {
                result.query shouldNotContain TOKEN
                result.query shouldContain "\"token\": \"@p3:string\""
            }

            result.vars shouldBe mapOf("p1" to "session", "p2" to "app", "p3" to TOKEN)
        }

        "the placeholder text is valid JSON with the query's structure intact" {
            val result = printed {
                filter(Filters.or(Filters.eq("a", 1), Filters.gt("b", 2)))
                sort(Sorts.descending("createdAt"))
                limit(10)
                skip(20)
            }

            val doc = BsonDocument.parse(result.query)

            withClue("the collection, operators and field names are structure, so they stay readable") {
                doc.toJson() shouldBe BsonDocument.parse(
                    """{"collection": "$COLLECTION", "filter": {"${'$'}or": [{"a": "@p1:int"}, {"b": {"${'$'}gt": "@p2:int"}}]},
                       "sort": {"createdAt": -1}, "limit": 10, "skip": 20}"""
                ).toJson()
            }

            result.vars shouldBe mapOf("p1" to 1, "p2" to 2)
        }

        "every kind of value is bound, not only strings -- a PIN is a number" {
            val id = ObjectId()

            val result = printed {
                filter(
                    Filters.and(
                        Filters.eq("pin", 734_912),
                        Filters.eq("_id", id),
                        Filters.eq("active", true),
                        Filters.regex("email", Pattern.compile("^secret-prefix")),
                        Filters.exists("deletedAt", false),
                    )
                )
            }

            withClue(result.query) {
                result.query shouldNotContain "734912"
                result.query shouldNotContain id.toHexString()
                result.query shouldNotContain "secret-prefix"
                result.query shouldNotContain "true"
                result.query shouldNotContain "false"
            }

            result.vars["p1"] shouldBe 734_912
            result.vars["p2"] shouldBe id
            result.vars.size shouldBe 5
        }

        "a list value is ONE placeholder holding the whole list" {
            val result = printed { filter(Filters.`in`("token", listOf(TOKEN, "other"))) }

            result.query shouldNotContain TOKEN
            withClue("the length is structure, the elements are values") {
                result.query shouldContain "\"${'$'}in\": \"@p1:array[2]\""
            }
            result.vars shouldBe mapOf("p1" to listOf(TOKEN, "other"))
        }

        "a value nested inside an embedded document is still a value" {
            val result = printed { filter(Filters.elemMatch("sessions", Filters.eq("token", TOKEN))) }

            result.query shouldNotContain TOKEN
            result.vars.values.toList() shouldBe listOf(TOKEN)
        }

        "no filter records no vars and keeps the paging" {
            val result = printed { page(page = 3, epp = 5) }

            result.vars shouldBe emptyMap()
            BsonDocument.parse(result.query).toJson() shouldBe
                    BsonDocument.parse("""{"collection": "$COLLECTION", "filter": null, "sort": null, "limit": 5, "skip": 10}""").toJson()
        }

        "the placeholder carries the BSON type, so an ObjectId _id and a string _id read differently" {
            val id = ObjectId()

            printed { filter(Filters.eq("_id", id)) }.query shouldContain "\"_id\": \"@p1:objectId\""
            printed { filter(Filters.eq("_id", id.toHexString())) }.query shouldContain "\"_id\": \"@p1:string\""
        }

        "every placeholder is exactly @pN:<type> -- nothing of a value can travel in the suffix" {
            // Values chosen so that ANY leak is visible: each would have to show up verbatim.
            val result = printed {
                filter(
                    Filters.and(
                        Filters.eq("a", TOKEN),
                        Filters.eq("b", 734_912L),
                        Filters.`in`("c", listOf(TOKEN, TOKEN)),
                        Filters.regex("d", Pattern.compile(TOKEN)),
                        Filters.eq("e", 1.5),
                    )
                )
            }

            val placeholders = Regex("\"@[^\"]*\"").findAll(result.query).map { it.value }.toList()

            placeholders shouldBe listOf(
                "\"@p1:string\"", "\"@p2:long\"", "\"@p3:array[2]\"", "\"@p4:regex\"", "\"@p5:double\"",
            )
            result.query shouldNotContain TOKEN
            result.query shouldNotContain "734912"
            result.query shouldNotContain "1.5"
        }

        "the label Monko records under is its own, not the generic json the collector no longer trusts" {
            MonkoDriver.FindQueryBuilder.Printed.LANGUAGE shouldBe "mongo-bound"
        }

        "print() still shows the values -- it is a debugging aid and must not be what gets recorded" {
            val builder = MonkoDriver.FindQueryBuilder().apply { filter(Filters.eq("token", TOKEN)) }

            builder.print() shouldContain TOKEN
            builder.printQuery(COLLECTION).query shouldNotContain TOKEN
        }
    }
}
