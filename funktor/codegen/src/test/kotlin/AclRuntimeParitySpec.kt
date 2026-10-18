package io.peekandpoke.funktor.codegen

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.peekandpoke.funktor.rest.acl.UserApiAccessMatrix
import io.peekandpoke.ultra.codegen.sdk.TsSdkBuilder
import io.peekandpoke.ultra.remote.ApiAccessLevel
import io.peekandpoke.ultra.security.user.User
import io.peekandpoke.ultra.slumber.Codec
import kotlin.reflect.typeOf

/**
 * Guards the one property that makes the generated access lookup work: a client member and the access
 * matrix must be keyed by the SAME two strings.
 *
 * `ApiAccessDescriptor` builds its entries from `route.method.value` / `route.pattern.pattern`; the
 * emitter must pass the same two through `route(...)`. If they diverge, every lookup misses and —
 * because absence means denied — the symptom is "every button disappeared", with no error anywhere.
 *
 * The two closed unions in `acl.ts` and `route.ts` are guarded by `AclRuntimeSpec` in `ultra:codegen`,
 * next to the resources themselves.
 */
class AclRuntimeParitySpec : FreeSpec() {

    init {
        "a generated member carries exactly the strings the access matrix is keyed by" {
            val feature = FxDemoApiFeature(
                listOf(
                    FxTalksApiRoutes(),      // public()
                    FxParamApiRoutes(),      // public()
                    FxSseApiRoutes(),        // public()
                    FxAuthedApiRoutes(),     // authenticated()
                    FxRoleApiRoutes(),       // forRole("ops-admin")
                    FxComposedApiRoutes(),   // authenticated() and forRole("ops")
                )
            )

            val out = TsSdkBuilder.forTesting(listOf(RestApiTsContributor(lazyOf(listOf(feature)))))
                .build()
                .output.entries()
                .single { it.path.endsWith("fxDemoClient.ts") }
                .content

            val routes = feature.getRouteGroups().flatMap { it.all }

            withClue("the fixture must actually contain routes, or this passes vacuously") {
                routes.isEmpty() shouldBe false
            }

            routes.forEach { route ->
                // The WRAPPER is derived the same way the emitter derives it — by evaluating the
                // route's real rule chain — so this covers publicness and route identity in one
                // assertion. Hardcoding either would let the two drift together unnoticed.
                val isPublic = !route.estimateAccess(User.anonymous).isDenied()
                val wrapper = if (isPublic) "publicRoute" else "route"

                // Exactly the expressions ApiAccessDescriptor uses, read off the same route object.
                val expected = "$wrapper('${route.method.value}', '${route.pattern.pattern}',"

                withClue("emitted client must carry `$expected`") {
                    out shouldContain expected
                }
            }

            withClue("the fixture set must contain BOTH kinds, or the wrapper choice is untested") {
                routes.any { !it.estimateAccess(User.anonymous).isDenied() } shouldBe true
                routes.any { it.estimateAccess(User.anonymous).isDenied() } shouldBe true
            }
        }

        "the matrix entry serializes to the wire names acl.ts reads" {
            // `acl.ts` consumes WIRE field names, not Kotlin property names, and builds its key from
            // `e.method` / `e.uri`. Asserting the Kotlin getters would pin nothing — adding
            // `@SerialName("http_method")` to Entry.method would keep that green while every lookup
            // in every generated SDK silently missed. So round-trip through the real codec.
            val slumbered = Codec.default.slumber(
                typeOf<UserApiAccessMatrix>(),
                UserApiAccessMatrix(
                    entries = listOf(
                        UserApiAccessMatrix.Entry(
                            method = "GET",
                            uri = "/x/{id}",
                            level = ApiAccessLevel.Granted,
                        )
                    )
                )
            )

            @Suppress("UNCHECKED_CAST")
            val entry = ((slumbered as Map<String, Any?>)["entries"] as List<Map<String, Any?>>).single()

            withClue("acl.ts keys on `method` and `uri`, and compares `level` to a string") {
                entry.keys shouldBe setOf("method", "uri", "level")
                entry["method"] shouldBe "GET"
                entry["uri"] shouldBe "/x/{id}"
                entry["level"] shouldBe "Granted"
            }
        }
    }
}
