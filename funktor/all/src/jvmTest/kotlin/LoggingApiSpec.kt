package io.peekandpoke.funktor

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.ktor.http.*
import io.peekandpoke.funktor.inspect.logging.api.LogEntryModel
import io.peekandpoke.funktor.inspect.logging.api.LogsRequest
import io.peekandpoke.funktor.logging.api.LoggingApi
import io.peekandpoke.funktor.logging.api.LoggingApiFeature
import io.peekandpoke.ultra.model.Paged

class LoggingApiSpec : FunktorApiSpec() {

    private val api by service(LoggingApiFeature::class)

    init {
        api.logging.list { route ->
            "Anonymous request must be unauthorized" {
                apiApp {
                    anonymous {
                        route(LoggingApi.ListParam()) { status shouldBe HttpStatusCode.Unauthorized }
                    }
                }
            }

            "Regular user request must be unauthorized" {
                apiApp {
                    authenticate(regularUserToken) {
                        route(LoggingApi.ListParam()) { status shouldBe HttpStatusCode.Unauthorized }
                    }
                }
            }

            "Super user request must return paged result" {
                apiApp {
                    authenticate(superUserToken) {
                        route(LoggingApi.ListParam()) {
                            status shouldBe HttpStatusCode.OK
                            val paged = apiResponseData<Paged<LogEntryModel>>()
                            paged.shouldNotBeNull()
                        }
                    }
                }
            }
        }

        api.logging.get { route ->
            "Anonymous request must be unauthorized" {
                apiApp {
                    anonymous {
                        route(LoggingApi.GetParam(id = "non-existent")) { status shouldBe HttpStatusCode.Unauthorized }
                    }
                }
            }

            "Regular user request must be unauthorized" {
                apiApp {
                    authenticate(regularUserToken) {
                        route(LoggingApi.GetParam(id = "non-existent")) { status shouldBe HttpStatusCode.Unauthorized }
                    }
                }
            }

            "Super user request for non-existent id must return not found" {
                apiApp {
                    authenticate(superUserToken) {
                        route(LoggingApi.GetParam(id = "non-existent")) { status shouldBe HttpStatusCode.NotFound }
                    }
                }
            }
        }

        api.logging.execBulkAction { route ->
            "Anonymous request must be unauthorized" {
                apiApp {
                    anonymous {
                        val body = LogsRequest.BulkAction(
                            filter = LogsRequest.BulkAction.Filter(from = null, to = null),
                            action = LogsRequest.Action.SetState(state = LogEntryModel.State.Ack),
                        )
                        request(route, body = body) { status shouldBe HttpStatusCode.Unauthorized }
                    }
                }
            }

            "Regular user request must be unauthorized" {
                apiApp {
                    authenticate(regularUserToken) {
                        val body = LogsRequest.BulkAction(
                            filter = LogsRequest.BulkAction.Filter(from = null, to = null),
                            action = LogsRequest.Action.SetState(state = LogEntryModel.State.Ack),
                        )
                        request(route, body = body) { status shouldBe HttpStatusCode.Unauthorized }
                    }
                }
            }

            "Super user request must return bulk response" {
                apiApp {
                    authenticate(superUserToken) {
                        val body = LogsRequest.BulkAction(
                            filter = LogsRequest.BulkAction.Filter(from = null, to = null),
                            action = LogsRequest.Action.SetState(state = LogEntryModel.State.Ack),
                        )
                        request(route, body = body) {
                            status shouldBe HttpStatusCode.OK
                            val result = apiResponseData<LogsRequest.BulkResponse>()
                            result.shouldNotBeNull()
                        }
                    }
                }
            }
        }

        api.logging.execAction { route ->
            "Anonymous request must be unauthorized" {
                apiApp {
                    anonymous {
                        route(
                            LoggingApi.GetParam(id = "non-existent"),
                            body = LogsRequest.Action.SetState(state = LogEntryModel.State.Ack),
                        ) { status shouldBe HttpStatusCode.Unauthorized }
                    }
                }
            }

            "Regular user request must be unauthorized" {
                apiApp {
                    authenticate(regularUserToken) {
                        route(
                            LoggingApi.GetParam(id = "non-existent"),
                            body = LogsRequest.Action.SetState(state = LogEntryModel.State.Ack),
                        ) { status shouldBe HttpStatusCode.Unauthorized }
                    }
                }
            }

            "Super user request for non-existent id must return not found" {
                apiApp {
                    authenticate(superUserToken) {
                        route(
                            LoggingApi.GetParam(id = "non-existent"),
                            body = LogsRequest.Action.SetState(state = LogEntryModel.State.Ack),
                        ) { status shouldBe HttpStatusCode.NotFound }
                    }
                }
            }
        }
    }
}
