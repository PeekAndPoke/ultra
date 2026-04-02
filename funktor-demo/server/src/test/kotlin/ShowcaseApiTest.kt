package io.peekandpoke.funktor.demo.server

import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotBeBlank
import io.ktor.http.*
import io.peekandpoke.funktor.demo.common.showcase.AuthRuleCheckResult
import io.peekandpoke.funktor.demo.common.showcase.EchoResponse
import io.peekandpoke.funktor.demo.common.showcase.ItemResponse
import io.peekandpoke.funktor.demo.common.showcase.RetryDemoRequest
import io.peekandpoke.funktor.demo.common.showcase.RetryDemoResponse
import io.peekandpoke.funktor.demo.common.showcase.ServerTimeResponse
import io.peekandpoke.funktor.demo.common.showcase.TransformRequest
import io.peekandpoke.funktor.demo.common.showcase.TransformResponse
import io.peekandpoke.funktor.demo.common.showcase.UpdateItemRequest
import io.peekandpoke.funktor.demo.server.api.showcase.RestShowcaseApi
import io.peekandpoke.funktor.demo.server.api.showcase.ShowcaseApiFeature
import io.peekandpoke.funktor.testing.AppSpec

class ShowcaseApiTest : AppSpec<FunktorDemoConfig>(testApp) {

    private val api by service(ShowcaseApiFeature::class)

    init {
        // REST showcase

        api.rest.getPlain { route ->
            "Get server time must return timestamp" {
                apiApp {
                    anonymous {
                        request(route) {
                            status shouldBe HttpStatusCode.OK
                            val data = apiResponseData<ServerTimeResponse>()
                            data.shouldNotBeNull()
                            data.timestamp.shouldNotBeBlank()
                            (data.epochMs > 0) shouldBe true
                        }
                    }
                }
            }
        }

        api.rest.getEcho { route ->
            "Echo must return the same message" {
                apiApp {
                    anonymous {
                        route(RestShowcaseApi.EchoParams(message = "hello-funktor")) {
                            status shouldBe HttpStatusCode.OK
                            val data = apiResponseData<EchoResponse>()
                            data.shouldNotBeNull()
                            data.message shouldBe "hello-funktor"
                            data.echoedAt.shouldNotBeBlank()
                        }
                    }
                }
            }
        }

        api.rest.postTransform { route ->
            "Transform to uppercase must work" {
                apiApp {
                    anonymous {
                        request(route, body = TransformRequest(text = "hello", operation = "uppercase")) {
                            status shouldBe HttpStatusCode.OK
                            val data = apiResponseData<TransformResponse>()
                            data.shouldNotBeNull()
                            data.original shouldBe "hello"
                            data.transformed shouldBe "HELLO"
                            data.operation shouldBe "uppercase"
                        }
                    }
                }
            }

            "Transform to reverse must work" {
                apiApp {
                    anonymous {
                        request(route, body = TransformRequest(text = "funktor", operation = "reverse")) {
                            status shouldBe HttpStatusCode.OK
                            val data = apiResponseData<TransformResponse>()
                            data.shouldNotBeNull()
                            data.transformed shouldBe "rotknuf"
                        }
                    }
                }
            }
        }

        api.rest.putItem { route ->
            "Update item must return updated item" {
                apiApp {
                    anonymous {
                        route(
                            RestShowcaseApi.ItemParams(id = "item-42"),
                            body = UpdateItemRequest(name = "Updated Name"),
                        ) {
                            status shouldBe HttpStatusCode.OK
                            val data = apiResponseData<ItemResponse>()
                            data.shouldNotBeNull()
                            data.id shouldBe "item-42"
                            data.name shouldBe "Updated Name"
                            data.updatedAt.shouldNotBeBlank()
                        }
                    }
                }
            }
        }

        // Core showcase

        api.core.postRetryDemo { route ->
            "Retry demo must succeed after specified attempts" {
                apiApp {
                    anonymous {
                        request(
                            route,
                            body = RetryDemoRequest(maxAttempts = 3, failUntilAttempt = 2, delayMs = 10),
                        ) {
                            status shouldBe HttpStatusCode.OK
                            val data = apiResponseData<RetryDemoResponse>()
                            data.shouldNotBeNull()
                            data.finalSuccess shouldBe true
                            data.attempts.size shouldBe 2
                        }
                    }
                }
            }
        }

        // Auth showcase

        api.auth.getAuthRuleChecks { route ->
            "Auth rule checks for anonymous user must return rules" {
                apiApp {
                    anonymous {
                        request(route) {
                            status shouldBe HttpStatusCode.OK
                            val data = apiResponseData<List<AuthRuleCheckResult>>()
                            data.shouldNotBeNull()
                            data.shouldNotBeEmpty()
                            data.any { it.ruleName == "public()" && it.accessGranted } shouldBe true
                            data.any { it.ruleName == "isSuperUser()" && !it.accessGranted } shouldBe true
                        }
                    }
                }
            }
        }
    }
}
