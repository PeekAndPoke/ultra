package io.peekandpoke.funktor

import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.ktor.client.request.header
import io.ktor.client.request.setBody
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.peekandpoke.funktor.auth.api.AuthApiFeature
import io.peekandpoke.funktor.auth.provider.EmailAndPasswordAuth
import io.peekandpoke.funktor.core.broker.typedRouteRenderer

/**
 * A body that does not fit its type is the CLIENT's error: a 400, on any body-bearing route (ULTRA-00292).
 *
 * It was a 500, logged as an internal error with the whole input, and the answer quoted it even in production:
 * a sign-in missing its email put the cleartext password into the log. What a production answer may say is
 * pinned by `ApiStatusPagesSpec` and `SlumberRestCodecSpec`; this pins the status through a real endpoint.
 */
class RequestBodyErrorsSpec : FunktorApiSpec() {

    private val api by service(AuthApiFeature::class)

    private val realmParam = AuthApiFeature.RealmParam(realm = TestUserRealm.REALM)

    init {
        val bodies = mapOf(
            "a missing required field" to """{"_type":"email_and_password","provider":"${EmailAndPasswordAuth.ID}","password":"x"}""",
            "a wrong-typed field" to
                """{"_type":"email_and_password","provider":"${EmailAndPasswordAuth.ID}","email":{"a":1},"password":"x"}""",
            "text that is not JSON" to """{"_type":"email_and_password","password":"x" oops""",
        )

        for ((what, body) in bodies) {
            "A sign-in body with $what is a 400, not a 500" {
                apiApp {
                    anonymous {
                        request(
                            HttpMethod.Post,
                            kontainer.typedRouteRenderer.render(api.authLogin.signIn.route(realmParam)),
                            setup = {
                                setBody(body)
                                header(HttpHeaders.ContentType, "application/json")
                            },
                        ) {
                            withClue(what) { status shouldBe HttpStatusCode.BadRequest }
                        }
                    }
                }
            }
        }
    }
}
