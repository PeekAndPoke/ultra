package io.peekandpoke.funktor.demo.server

import io.kotest.matchers.shouldBe
import io.ktor.http.HttpStatusCode
import io.peekandpoke.funktor.demo.server.operator.OperatorApiFeature
import io.peekandpoke.funktor.testing.AppSpec

class OperatorApiTest : AppSpec<FunktorDemoConfig>(testApp) {

    private val api by service(OperatorApiFeature::class)

    init {
        // Security boundary: the operator console is super-user only. This also proves the feature
        // is registered and the route is mounted (an unmounted route would 404, not 401).
        // The happy-path stats assertion is deferred until the demo test harness gains a super-user
        // sign-in helper (shared with 20260719-cross-realm-authz-and-tests).
        api.operator.dashboardStats { route ->
            "operator dashboard stats rejects an anonymous caller (super-user guard)" {
                apiApp {
                    anonymous {
                        request(route) {
                            status shouldBe HttpStatusCode.Unauthorized
                        }
                    }
                }
            }
        }
    }
}
