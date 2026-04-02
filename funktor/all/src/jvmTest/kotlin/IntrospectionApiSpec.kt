package io.peekandpoke.funktor

import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotBeBlank
import io.ktor.http.*
import io.peekandpoke.funktor.inspect.introspection.api.ApiAccessMatrixModel
import io.peekandpoke.funktor.inspect.introspection.api.AppLifecycleInfo
import io.peekandpoke.funktor.inspect.introspection.api.AuthRealmInfo
import io.peekandpoke.funktor.inspect.introspection.api.ConfigInfoEntry
import io.peekandpoke.funktor.inspect.introspection.api.EndpointInfo
import io.peekandpoke.funktor.inspect.introspection.api.IntrospectionApiFeature
import io.peekandpoke.funktor.inspect.introspection.api.PasswordValidationRequest
import io.peekandpoke.funktor.inspect.introspection.api.PasswordValidationResponse

class IntrospectionApiSpec : FunktorApiSpec() {

    private val api by service(IntrospectionApiFeature::class)

    init {
        api.introspection.getLifecycleHooks { route ->
            "Anonymous request must be unauthorized" {
                apiApp {
                    anonymous {
                        request(route) { status shouldBe HttpStatusCode.Unauthorized }
                    }
                }
            }

            "Regular user request must be unauthorized" {
                apiApp {
                    authenticate(regularUserToken) {
                        request(route) { status shouldBe HttpStatusCode.Unauthorized }
                    }
                }
            }

            "Super user request must succeed" {
                apiApp {
                    authenticate(superUserToken) {
                        request(route) { status shouldBe HttpStatusCode.OK }
                    }
                }
            }
        }

        api.introspection.getConfigInfo { route ->
            "Anonymous request must be unauthorized" {
                apiApp {
                    anonymous {
                        request(route) { status shouldBe HttpStatusCode.Unauthorized }
                    }
                }
            }

            "Regular user request must be unauthorized" {
                apiApp {
                    authenticate(regularUserToken) {
                        request(route) { status shouldBe HttpStatusCode.Unauthorized }
                    }
                }
            }

            "Super user request must return config entries" {
                apiApp {
                    authenticate(superUserToken) {
                        request(route) {
                            status shouldBe HttpStatusCode.OK
                            val entries = apiResponseData<List<ConfigInfoEntry>>()
                            entries.shouldNotBeNull()
                            entries.shouldNotBeEmpty()
                        }
                    }
                }
            }
        }

        api.introspection.getCliCommands { route ->
            "Anonymous request must be unauthorized" {
                apiApp {
                    anonymous {
                        request(route) { status shouldBe HttpStatusCode.Unauthorized }
                    }
                }
            }

            "Regular user request must be unauthorized" {
                apiApp {
                    authenticate(regularUserToken) {
                        request(route) { status shouldBe HttpStatusCode.Unauthorized }
                    }
                }
            }

            "Super user request must succeed" {
                apiApp {
                    authenticate(superUserToken) {
                        request(route) { status shouldBe HttpStatusCode.OK }
                    }
                }
            }
        }

        api.introspection.getFixtures { route ->
            "Anonymous request must be unauthorized" {
                apiApp {
                    anonymous {
                        request(route) { status shouldBe HttpStatusCode.Unauthorized }
                    }
                }
            }

            "Regular user request must be unauthorized" {
                apiApp {
                    authenticate(regularUserToken) {
                        request(route) { status shouldBe HttpStatusCode.Unauthorized }
                    }
                }
            }

            "Super user request must succeed" {
                apiApp {
                    authenticate(superUserToken) {
                        request(route) { status shouldBe HttpStatusCode.OK }
                    }
                }
            }
        }

        api.introspection.getRepairs { route ->
            "Anonymous request must be unauthorized" {
                apiApp {
                    anonymous {
                        request(route) { status shouldBe HttpStatusCode.Unauthorized }
                    }
                }
            }

            "Regular user request must be unauthorized" {
                apiApp {
                    authenticate(regularUserToken) {
                        request(route) { status shouldBe HttpStatusCode.Unauthorized }
                    }
                }
            }

            "Super user request must succeed" {
                apiApp {
                    authenticate(superUserToken) {
                        request(route) { status shouldBe HttpStatusCode.OK }
                    }
                }
            }
        }

        api.introspection.getAllEndpoints { route ->
            "Anonymous request must be unauthorized" {
                apiApp {
                    anonymous {
                        request(route) { status shouldBe HttpStatusCode.Unauthorized }
                    }
                }
            }

            "Regular user request must be unauthorized" {
                apiApp {
                    authenticate(regularUserToken) {
                        request(route) { status shouldBe HttpStatusCode.Unauthorized }
                    }
                }
            }

            "Super user request must return all endpoints" {
                apiApp {
                    authenticate(superUserToken) {
                        request(route) {
                            status shouldBe HttpStatusCode.OK
                            val endpoints = apiResponseData<List<EndpointInfo>>()
                            endpoints.shouldNotBeNull()
                            endpoints.shouldNotBeEmpty()
                        }
                    }
                }
            }
        }

        api.introspection.getAuthRealms { route ->
            "Anonymous request must be unauthorized" {
                apiApp {
                    anonymous {
                        request(route) { status shouldBe HttpStatusCode.Unauthorized }
                    }
                }
            }

            "Regular user request must be unauthorized" {
                apiApp {
                    authenticate(regularUserToken) {
                        request(route) { status shouldBe HttpStatusCode.Unauthorized }
                    }
                }
            }

            "Super user request must return realms including admin-user" {
                apiApp {
                    authenticate(superUserToken) {
                        request(route) {
                            status shouldBe HttpStatusCode.OK
                            val realms = apiResponseData<List<AuthRealmInfo>>()
                            realms.shouldNotBeNull()
                            realms.shouldNotBeEmpty()
                            realms.any { it.id == TestUserRealm.REALM } shouldBe true
                        }
                    }
                }
            }
        }

        api.introspection.validatePassword { route ->
            "Anonymous request must be unauthorized" {
                apiApp {
                    anonymous {
                        request(route, body = PasswordValidationRequest(password = "test")) {
                            status shouldBe HttpStatusCode.Unauthorized
                        }
                    }
                }
            }

            "Regular user request must be unauthorized" {
                apiApp {
                    authenticate(regularUserToken) {
                        request(route, body = PasswordValidationRequest(password = "test")) {
                            status shouldBe HttpStatusCode.Unauthorized
                        }
                    }
                }
            }

            "Super user request must return validation result" {
                apiApp {
                    authenticate(superUserToken) {
                        request(route, body = PasswordValidationRequest(password = "Test1234!")) {
                            status shouldBe HttpStatusCode.OK
                            val result = apiResponseData<PasswordValidationResponse>()
                            result.shouldNotBeNull()
                            result.policyDescription.shouldNotBeBlank()
                        }
                    }
                }
            }
        }

        api.introspection.getAppLifecycle { route ->
            "Anonymous request must be unauthorized" {
                apiApp {
                    anonymous {
                        request(route) { status shouldBe HttpStatusCode.Unauthorized }
                    }
                }
            }

            "Regular user request must be unauthorized" {
                apiApp {
                    authenticate(regularUserToken) {
                        request(route) { status shouldBe HttpStatusCode.Unauthorized }
                    }
                }
            }

            "Super user request must return lifecycle info" {
                apiApp {
                    authenticate(superUserToken) {
                        request(route) {
                            status shouldBe HttpStatusCode.OK
                            val info = apiResponseData<AppLifecycleInfo>()
                            info.shouldNotBeNull()
                            info.jvmVersion.shouldNotBeBlank()
                            info.kotlinVersion.shouldNotBeBlank()
                        }
                    }
                }
            }
        }

        api.introspection.getApiAccessMatrix { route ->
            "Anonymous request must be unauthorized" {
                apiApp {
                    anonymous {
                        request(route) { status shouldBe HttpStatusCode.Unauthorized }
                    }
                }
            }

            "Regular user request must be unauthorized" {
                apiApp {
                    authenticate(regularUserToken) {
                        request(route) { status shouldBe HttpStatusCode.Unauthorized }
                    }
                }
            }

            "Super user request must return access matrix" {
                apiApp {
                    authenticate(superUserToken) {
                        request(route) {
                            status shouldBe HttpStatusCode.OK
                            val matrix = apiResponseData<ApiAccessMatrixModel>()
                            matrix.shouldNotBeNull()
                            matrix.features.shouldNotBeEmpty()
                        }
                    }
                }
            }
        }
    }
}
