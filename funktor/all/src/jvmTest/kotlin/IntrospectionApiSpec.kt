package io.peekandpoke.funktor

import io.kotest.matchers.shouldBe
import io.ktor.http.*
import io.peekandpoke.funktor.inspect.introspection.api.IntrospectionApiFeature
import io.peekandpoke.funktor.inspect.introspection.api.PasswordValidationRequest
import io.peekandpoke.funktor.testing.AppSpec
import kotlinx.coroutines.runBlocking

class IntrospectionApiSpec : AppSpec<FunktorAllTestConfig>(testApp) {

    private val api by service(IntrospectionApiFeature::class)
    private val realm by service(TestUserRealm::class)
    private val usersRepo by service(TestUsersRepo::class)

    private val superUserToken: String by lazy {
        runBlocking {
            val user = usersRepo.insert(
                TestUser(name = "Super User", email = "super@test.com", isSuperUser = true)
            )
            realm.generateJwt(user).token
        }
    }

    init {
        api.introspection.getLifecycleHooks { route ->
            "Anonymous request must be forbidden" {
                apiApp {
                    anonymous {
                        request(route) {
                            status shouldBe HttpStatusCode.Unauthorized
                        }
                    }
                }
            }

            "Super user request must succeed" {
                apiApp {
                    authenticate(superUserToken) {
                        request(route) {
                            status shouldBe HttpStatusCode.OK
                        }
                    }
                }
            }
        }

        api.introspection.getConfigInfo { route ->
            "Anonymous request must be forbidden" {
                apiApp {
                    anonymous {
                        request(route) {
                            status shouldBe HttpStatusCode.Unauthorized
                        }
                    }
                }
            }

            "Super user request must succeed" {
                apiApp {
                    authenticate(superUserToken) {
                        request(route) {
                            status shouldBe HttpStatusCode.OK
                        }
                    }
                }
            }
        }

        api.introspection.getCliCommands { route ->
            "Anonymous request must be forbidden" {
                apiApp {
                    anonymous {
                        request(route) {
                            status shouldBe HttpStatusCode.Unauthorized
                        }
                    }
                }
            }

            "Super user request must succeed" {
                apiApp {
                    authenticate(superUserToken) {
                        request(route) {
                            status shouldBe HttpStatusCode.OK
                        }
                    }
                }
            }
        }

        api.introspection.getFixtures { route ->
            "Anonymous request must be forbidden" {
                apiApp {
                    anonymous {
                        request(route) {
                            status shouldBe HttpStatusCode.Unauthorized
                        }
                    }
                }
            }

            "Super user request must succeed" {
                apiApp {
                    authenticate(superUserToken) {
                        request(route) {
                            status shouldBe HttpStatusCode.OK
                        }
                    }
                }
            }
        }

        api.introspection.getRepairs { route ->
            "Anonymous request must be forbidden" {
                apiApp {
                    anonymous {
                        request(route) {
                            status shouldBe HttpStatusCode.Unauthorized
                        }
                    }
                }
            }

            "Super user request must succeed" {
                apiApp {
                    authenticate(superUserToken) {
                        request(route) {
                            status shouldBe HttpStatusCode.OK
                        }
                    }
                }
            }
        }

        api.introspection.getAllEndpoints { route ->
            "Anonymous request must be forbidden" {
                apiApp {
                    anonymous {
                        request(route) {
                            status shouldBe HttpStatusCode.Unauthorized
                        }
                    }
                }
            }

            "Super user request must succeed" {
                apiApp {
                    authenticate(superUserToken) {
                        request(route) {
                            status shouldBe HttpStatusCode.OK
                        }
                    }
                }
            }
        }

        api.introspection.getAuthRealms { route ->
            "Anonymous request must be forbidden" {
                apiApp {
                    anonymous {
                        request(route) {
                            status shouldBe HttpStatusCode.Unauthorized
                        }
                    }
                }
            }

            "Super user request must succeed" {
                apiApp {
                    authenticate(superUserToken) {
                        request(route) {
                            status shouldBe HttpStatusCode.OK
                        }
                    }
                }
            }
        }

        api.introspection.validatePassword { route ->
            "Anonymous request must be forbidden" {
                apiApp {
                    anonymous {
                        request(route, body = PasswordValidationRequest(password = "test")) {
                            status shouldBe HttpStatusCode.Unauthorized
                        }
                    }
                }
            }

            "Super user request must succeed" {
                apiApp {
                    authenticate(superUserToken) {
                        request(route, body = PasswordValidationRequest(password = "test")) {
                            status shouldBe HttpStatusCode.OK
                        }
                    }
                }
            }
        }

        api.introspection.getAppLifecycle { route ->
            "Anonymous request must be forbidden" {
                apiApp {
                    anonymous {
                        request(route) {
                            status shouldBe HttpStatusCode.Unauthorized
                        }
                    }
                }
            }

            "Super user request must succeed" {
                apiApp {
                    authenticate(superUserToken) {
                        request(route) {
                            status shouldBe HttpStatusCode.OK
                        }
                    }
                }
            }
        }

        api.introspection.getApiAccessMatrix { route ->
            "Anonymous request must be forbidden" {
                apiApp {
                    anonymous {
                        request(route) {
                            status shouldBe HttpStatusCode.Unauthorized
                        }
                    }
                }
            }

            "Super user request must succeed" {
                apiApp {
                    authenticate(superUserToken) {
                        request(route) {
                            status shouldBe HttpStatusCode.OK
                        }
                    }
                }
            }
        }
    }
}
