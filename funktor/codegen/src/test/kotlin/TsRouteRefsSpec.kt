package io.peekandpoke.funktor.codegen

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.peekandpoke.funktor.rest.ApiRoutes
import io.peekandpoke.ultra.codegen.sdk.TsSdkRegistry

/**
 * Guards the lookup a nav entry's access gate is built on.
 *
 * Everything here exists because the alternative — a literal `ApiRouteRef("GET", "/…")` in a
 * contributor — fails SILENTLY. `ApiAcl` transmits denial by OMISSION, so a ref matching no matrix
 * row reads `Denied` for every user and the menu entry disappears for everyone, with nothing
 * anywhere naming the cause.
 */
class TsRouteRefsSpec : FreeSpec() {

    private fun feature(vararg groups: ApiRoutes) = FxDemoApiFeature(groups.toList())

    init {
        "it resolves a member to the method and PATTERN the access matrix is keyed on" {
            val ref = TsRouteRefs.of(feature(FxParamApiRoutes()), "getTalk")

            ref.method shouldBe "GET"

            withClue("the PATTERN, placeholders included — a filled-in URL matches no matrix row") {
                ref.uri shouldBe "/api/fx/talks/{id}"
            }
        }

        "it finds a route by its DERIVED member name, not only a declared funcName" {
            // The naming rule must be the SAME one `RestApiTsContributor` emits the client with. If
            // the two ever diverged, a contributor would declare a gate on a name no generated
            // member carries — and the failure would be a silently hidden menu entry.
            val ref = TsRouteRefs.of(feature(FxTalksApiRoutes()), "getApiFxTalksLatest")

            ref.uri shouldBe "/api/fx/talks/latest"
        }

        "isPublic is EVALUATED from the route's rules, not assumed" - {
            // The pair matters more than either half: `canAccess` short-circuits on isPublic, so a
            // hardcoded `true` would show every gated entry to anonymous visitors, and a hardcoded
            // `false` would hide the sign-in link from the only people who need it. One fixture
            // alone kills neither mutant.

            "a public() floor yields true" {
                TsRouteRefs.of(feature(FxTalksApiRoutes()), "listTalks").isPublic shouldBe true
            }

            "a forRole() floor yields false" {
                TsRouteRefs.of(feature(FxRoleApiRoutes()), "adminOnly").isPublic shouldBe false
            }

            "an authenticated() floor yields false" {
                TsRouteRefs.of(feature(FxAuthedApiRoutes()), "secret").isPublic shouldBe false
            }
        }

        "an unknown member is a hard error listing what IS available" {
            val thrown = shouldThrow<IllegalStateException> {
                TsRouteRefs.of(feature(FxParamApiRoutes()), "getTalks")
            }

            thrown.message!! shouldContain "getTalks"

            withClue("the message must name the feature and offer the real names") {
                thrown.message!! shouldContain "FxDemo"
                thrown.message!! shouldContain "fx-params.getTalk"
            }
        }

        "a member name shared by two groups is an ambiguity, not a silent pick" - {
            // Two groups may legitimately share a member name — the duplicate guard in
            // `RestApiTsContributor` only fires WITHIN a group. Picking one here would gate the
            // menu on whichever group happened to be listed first.
            val shared = feature(FxSharedNameTalksRoutes(), FxSharedNameSpeakersRoutes())

            "the unqualified lookup refuses, naming both" {
                val thrown = shouldThrow<IllegalStateException> { TsRouteRefs.of(shared, "list") }

                thrown.message!! shouldContain "fx-shared-talks"
                thrown.message!! shouldContain "fx-shared-speakers"

                withClue("and must say how to fix it") {
                    thrown.message!! shouldContain "group ="
                }
            }

            "naming the group resolves it" {
                TsRouteRefs.of(shared, "list", group = "fx-shared-speakers").uri shouldBe
                        "/api/fx/shared/speakers"

                TsRouteRefs.of(shared, "list", group = "fx-shared-talks").uri shouldBe
                        "/api/fx/shared/talks"
            }
        }

        "a resolved ref is accepted by the registry it is destined for" {
            // The two validations in `TsSdkRegistry` refuse a method outside the emitted union and a
            // uri that cannot be a route pattern. Anything this object produces must pass them, or
            // the helper would be handing contributors values the registry then rejects.
            val ref = TsRouteRefs.of(feature(FxRoleApiRoutes()), "adminOnly")

            TsSdkRegistry().scopeFor("fx").route(
                path = "/admin",
                component = "pages/Admin.vue",
                requiresAuth = true,
                nav = TsSdkRegistry.Nav(label = "Admin", requires = listOf(ref)),
            )
        }
    }
}
