package io.peekandpoke.funktor.demo.server

import io.kotest.matchers.shouldBe
import io.ktor.http.*
import io.peekandpoke.funktor.auth.api.AuthApiFeature
import io.peekandpoke.funktor.auth.api.AuthApiFeature.RealmParam
import io.peekandpoke.funktor.testing.AppSpec

class DemoTest : AppSpec<FunktorDemoConfig>(testApp) {

    private val api get() = kontainer.get(AuthApiFeature::class)

    init {
        api.auth.getRealm { route ->
            "Getting an existing realm must work" {
                apiApp {
                    anonymous {
                        val param = RealmParam(realm = "admin-user")

                        route(param) {
                            status shouldBe HttpStatusCode.OK
                        }
                    }
                }
            }
        }
    }
}
