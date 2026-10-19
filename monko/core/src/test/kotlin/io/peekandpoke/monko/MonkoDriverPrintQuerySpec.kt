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
    }

    private fun printed(block: MonkoDriver.FindQueryBuilder.() -> Unit): MonkoDriver.FindQueryBuilder.Printed =
        MonkoDriver.FindQueryBuilder().apply(block).printQuery()

    init {
        "a token lookup records the field and a placeholder, and the token only in vars" {
            // The shape of MonkoAuthRecordsRepo.findByToken
            val result = printed {
                filter(Filters.and(Filters.eq("_type", "session"), Filters.eq("realm", "app"), Filters.eq("token", TOKEN)))
                limit(1)
            }

            withClue(result.query) {
                result.query shouldNotContain TOKEN
                result.query shouldContain "\"token\": \"@p3\""
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

            withClue("operators and field names are structure, so they stay readable") {
                doc.toJson() shouldBe BsonDocument.parse(
                    """{"filter": {"${'$'}or": [{"a": "@p1"}, {"b": {"${'$'}gt": "@p2"}}]},
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
            result.query shouldContain "\"${'$'}in\": \"@p1\""
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
                    BsonDocument.parse("""{"filter": null, "sort": null, "limit": 5, "skip": 10}""").toJson()
        }

        "print() still shows the values -- it is a debugging aid and must not be what gets recorded" {
            val builder = MonkoDriver.FindQueryBuilder().apply { filter(Filters.eq("token", TOKEN)) }

            builder.print() shouldContain TOKEN
            builder.printQuery().query shouldNotContain TOKEN
        }
    }
}
