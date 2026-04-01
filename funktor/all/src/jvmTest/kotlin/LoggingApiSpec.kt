package io.peekandpoke.funktor

import io.kotest.matchers.shouldBe
import io.ktor.http.*
import io.peekandpoke.funktor.inspect.logging.api.LogEntryModel
import io.peekandpoke.funktor.inspect.logging.api.LogsRequest
import io.peekandpoke.funktor.logging.api.LoggingApi
import io.peekandpoke.funktor.logging.api.LoggingApiFeature
import io.peekandpoke.funktor.testing.AppSpec
import kotlinx.coroutines.runBlocking

class LoggingApiSpec : AppSpec<FunktorAllTestConfig>(testApp) {

    private val api by service(LoggingApiFeature::class)
    private val realm by service(TestUserRealm::class)
    private val usersRepo by service(TestUsersRepo::class)

    private val superUserToken: String by lazy {
        runBlocking {
            val user = usersRepo.insert(
                TestUser(name = "Super User", email = "logging-super@test.com", isSuperUser = true)
            )
            realm.generateJwt(user).token
        }
    }

    init {
        api.logging.list { route ->
            "Anonymous request must be unauthorized" {
                apiApp {
                    anonymous {
                        route(LoggingApi.ListParam()) {
                            status shouldBe HttpStatusCode.Unauthorized
                        }
                    }
                }
            }

            "Super user request must succeed" {
                apiApp {
                    authenticate(superUserToken) {
                        route(LoggingApi.ListParam()) {
                            status shouldBe HttpStatusCode.OK
                        }
                    }
                }
            }
        }

        api.logging.get { route ->
            "Anonymous request must be unauthorized" {
                apiApp {
                    anonymous {
                        route(LoggingApi.GetParam(id = "non-existent")) {
                            status shouldBe HttpStatusCode.Unauthorized
                        }
                    }
                }
            }

            "Super user request for non-existent id must return not found" {
                apiApp {
                    authenticate(superUserToken) {
                        route(LoggingApi.GetParam(id = "non-existent")) {
                            status shouldBe HttpStatusCode.NotFound
                        }
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
                        request(route, body = body) {
                            status shouldBe HttpStatusCode.Unauthorized
                        }
                    }
                }
            }

            "Super user request must succeed" {
                apiApp {
                    authenticate(superUserToken) {
                        val body = LogsRequest.BulkAction(
                            filter = LogsRequest.BulkAction.Filter(from = null, to = null),
                            action = LogsRequest.Action.SetState(state = LogEntryModel.State.Ack),
                        )
                        request(route, body = body) {
                            status shouldBe HttpStatusCode.OK
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
                        ) {
                            status shouldBe HttpStatusCode.Unauthorized
                        }
                    }
                }
            }

            "Super user request for non-existent id must return not found" {
                apiApp {
                    authenticate(superUserToken) {
                        route(
                            LoggingApi.GetParam(id = "non-existent"),
                            body = LogsRequest.Action.SetState(state = LogEntryModel.State.Ack),
                        ) {
                            status shouldBe HttpStatusCode.NotFound
                        }
                    }
                }
            }
        }
    }
}
