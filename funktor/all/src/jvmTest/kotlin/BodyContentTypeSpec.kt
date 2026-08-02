package io.peekandpoke.funktor

import io.kotest.matchers.shouldBe
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.withCharset
import io.peekandpoke.funktor.auth.api.AuthApiFeature
import io.peekandpoke.funktor.auth.model.AuthSignInRequest
import io.peekandpoke.funktor.auth.provider.EmailAndPasswordAuth

/**
 * A body-bearing route must declare `application/json`.
 *
 * ### Why this is a CSRF test, not a tidiness test
 *
 * A POST with `text/plain`, `application/x-www-form-urlencoded` or `multipart` is a **simple request**:
 * the browser sends it cross-origin with no CORS preflight, and CORS then only governs whether the
 * *response* is readable. The side effect has already happened. A JSON API is normally immune by
 * accident, because `application/json` always preflights — this one was not, because the body was read
 * as bytes and parsed as JSON whatever the header said.
 *
 * Found while designing (and then dropping) the cookie transport; it is a live hole independent of that.
 * See `.claude/tasks/20260802-rest-content-type-enforcement.md`.
 *
 * **Both directions are asserted deliberately.** A test that only shows JSON still works would pass with
 * the gate deleted.
 */
class BodyContentTypeSpec : FunktorApiSpec() {

    private val api by service(AuthApiFeature::class)

    private val realmParam = AuthApiFeature.RealmParam(realm = TestUserRealm.REALM)

    private val body = AuthSignInRequest.EmailAndPassword(
        provider = EmailAndPasswordAuth.ID,
        email = "content-type@test.com",
        password = "password",
    )

    init {

        api.authLogin.signIn { route ->

            "A body declaring text/plain is refused with 415 — this is the simple-request shape" {
                apiApp {
                    anonymous {
                        route(realmParam, body = body, setup = { contentType(ContentType.Text.Plain) }) {
                            status shouldBe HttpStatusCode.UnsupportedMediaType
                        }
                    }
                }
            }

            "A body declaring form-urlencoded is refused too — the other simple-request shape" {
                apiApp {
                    anonymous {
                        route(
                            realmParam,
                            body = body,
                            setup = { contentType(ContentType.Application.FormUrlEncoded) },
                        ) {
                            status shouldBe HttpStatusCode.UnsupportedMediaType
                        }
                    }
                }
            }

            "A body declaring application/json is accepted — the gate must not reject real clients" {
                apiApp {
                    anonymous {
                        route(realmParam, body = body) {
                            // Whatever the credentials do, it must NOT be a media-type rejection.
                            status shouldBe HttpStatusCode.Forbidden
                        }
                    }
                }
            }

            "application/json with a charset parameter is still accepted" {
                apiApp {
                    anonymous {
                        route(
                            realmParam,
                            body = body,
                            setup = { contentType(ContentType.Application.Json.withCharset(Charsets.UTF_8)) },
                        ) {
                            status shouldBe HttpStatusCode.Forbidden
                        }
                    }
                }
            }
        }
    }
}
