package io.peekandpoke.funktor

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.peekandpoke.funktor.insights.api.InsightsApiFeature
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** The running server's git version and environment for the insights list header (ULTRA-00190), over real HTTP. */
class InsightsServerInfoApiSpec : FunktorApiSpec() {

    private val api by service(InsightsApiFeature::class)

    init {
        "a superuser gets the running server's git version, application id and environment" {
            insightsApp {
                authenticate(superUserToken) {
                    request(api.insights.getServerInfo) {
                        status shouldBe HttpStatusCode.OK

                        // opt-in, like the other insights specs: the Vue list page's header reads this
                        insightsWireSample("server-info.json", bodyAsText())

                        val data = Json.parseToJsonElement(bodyAsText()).jsonObject["data"].shouldNotBeNull().jsonObject

                        data.keys shouldBe setOf("git", "gitDesc", "applicationId", "environment")
                        // From funktor/all's test config, not from the code under test
                        data["applicationId"]!!.jsonPrimitive.content shouldBe "Funktor-All | Test"
                        data["environment"]!!.jsonPrimitive.content shouldBe "test"
                        // The test build carries no git info: AppVersion's defaults
                        data["git"]!!.jsonPrimitive.content shouldBe ""
                        data["gitDesc"]!!.jsonPrimitive.content shouldBe "n/a"
                    }
                }
            }
        }

        "below superuser: the floor's 401, and no server info" {
            insightsApp {
                anonymous {
                    request(api.insights.getServerInfo) {
                        status shouldBe HttpStatusCode.Unauthorized
                        Json.parseToJsonElement(bodyAsText()).jsonObject["data"] shouldBe JsonNull
                    }
                }
                authenticate(regularUserToken) {
                    request(api.insights.getServerInfo) {
                        status shouldBe HttpStatusCode.Unauthorized
                        Json.parseToJsonElement(bodyAsText()).jsonObject["data"] shouldBe JsonNull
                    }
                }
            }
        }
    }
}
