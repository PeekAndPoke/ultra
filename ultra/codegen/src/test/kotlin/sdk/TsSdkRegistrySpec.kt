package io.peekandpoke.ultra.codegen.sdk

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.collections.shouldContain as shouldContainElement
import io.kotest.matchers.string.shouldContain

class TsSdkRegistrySpec : FreeSpec() {

    private fun registry() = TsSdkRegistry()

    init {
        "ordering does not depend on contributor order" {
            // THE property the whole design turns on. Contributors arrive from a DI container, which
            // defines no order at all, so an aggregate assembled in arrival order would produce a
            // different file on a different run — and `--check` would report drift that is not real.
            val forwards = registry().apply {
                scopeFor("a").route("/zebra", "pages/Z.vue", requiresAuth = true)
                scopeFor("b").route("/apple", "pages/A.vue", requiresAuth = false)
            }

            val backwards = registry().apply {
                scopeFor("b").route("/apple", "pages/A.vue", requiresAuth = false)
                scopeFor("a").route("/zebra", "pages/Z.vue", requiresAuth = true)
            }

            forwards.allRoutes().map { it.path } shouldBe listOf("/apple", "/zebra")
            forwards.allRoutes() shouldBe backwards.allRoutes()
        }

        "two contributors claiming one path is a hard error naming both" {
            val reg = registry()
            reg.scopeFor("funktor:insights").route("/insights", "pages/I.vue", requiresAuth = true)

            val thrown = shouldThrow<IllegalStateException> {
                reg.scopeFor("acme:app").route("/insights", "pages/Other.vue", requiresAuth = true)
            }

            withClue("both contributors and both components must be in the message") {
                thrown.message!! shouldContain "funktor:insights"
                thrown.message!! shouldContain "acme:app"
                thrown.message!! shouldContain "pages/I.vue"
                thrown.message!! shouldContain "pages/Other.vue"
            }
        }

        "nav order is total and stable" {
            // Ties break on path, so two entries at the same order still have ONE correct rendering.
            val reg = registry()
            val scope = reg.scopeFor("a")

            scope.route("/b", "B.vue", requiresAuth = false, nav = TsSdkRegistry.Nav("B", order = 5))
            scope.route("/a", "A.vue", requiresAuth = false, nav = TsSdkRegistry.Nav("A", order = 5))
            scope.route("/first", "F.vue", requiresAuth = false, nav = TsSdkRegistry.Nav("F", order = 1))
            scope.route("/hidden", "H.vue", requiresAuth = false)

            reg.navRoutes().map { it.path } shouldBe listOf("/first", "/a", "/b")

            withClue("a route with no nav entry is still a route") {
                reg.allRoutes().map { it.path } shouldContainElement "/hidden"
            }
        }

        "a relative path is refused" {
            // It would resolve against whatever the app happens to be showing.
            val thrown = shouldThrow<IllegalArgumentException> {
                registry().scopeFor("a").route("insights", "I.vue", requiresAuth = true)
            }

            thrown.message!! shouldContain "must start with '/'"
        }

        "a component escaping the SDK root is refused" - {
            // The generator owns its output directory outright and writes nothing outside it. A
            // component reference that climbs out would be an import the app cannot satisfy — or,
            // worse, one it can.
            listOf("../outside/X.vue", "/abs/X.vue", "pages/../../X.vue").forEach { component ->
                "rejects '$component'" {
                    val thrown = shouldThrow<IllegalArgumentException> {
                        registry().scopeFor("a").route("/x", component, requiresAuth = true)
                    }

                    thrown.message!! shouldContain "relative to the SDK root"
                }
            }
        }

        //  Nav access requirements  ///////////////////////////////////////////////////////////////

        "nav requirements are carried through registration" {
            val ref = TsSdkRegistry.ApiRouteRef("GET", "/api/records", isPublic = false)

            val nav = registry()
                .apply {
                    scopeFor("a").route(
                        path = "/records",
                        component = "pages/R.vue",
                        requiresAuth = true,
                        nav = TsSdkRegistry.Nav(label = "Records", requires = listOf(ref)),
                    )
                }
                .navRoutes().single().nav!!

            nav.requires shouldBe listOf(ref)
        }

        "a route with no nav requirements declares none, rather than null" {
            // `every` over an empty list is true, which is what makes an ungated page show. A null
            // would be a runtime error in the consuming app instead.
            val nav = registry()
                .apply {
                    scopeFor("a").route("/plain", "pages/P.vue", requiresAuth = false, nav = TsSdkRegistry.Nav("Plain"))
                }
                .navRoutes().single().nav!!

            nav.requires shouldBe emptyList()
        }

        "a requirement using a method the emitted union does not list is refused" - {
            // It lands in a CLOSED union literal in `mount.ts`, so this would be TypeScript that
            // fails to compile inside output the consuming app cannot edit.
            listOf("get", "TRACE", "", "GET ").forEach { method ->
                "rejects '$method'" {
                    val thrown = shouldThrow<IllegalArgumentException> {
                        registry().scopeFor("a").route(
                            path = "/x",
                            component = "pages/X.vue",
                            requiresAuth = true,
                            nav = TsSdkRegistry.Nav(
                                label = "X",
                                requires = listOf(TsSdkRegistry.ApiRouteRef(method, "/api/x", isPublic = false)),
                            ),
                        )
                    }

                    thrown.message!! shouldContain "runtime/route.ts"
                    // "route '/x'", not "/x": the ref's own uri is '/api/x', which CONTAINS "/x",
                    // so the bare form passed even with the route path dropped from the message —
                    // and naming the offending nav entry is the message's whole job.
                    thrown.message!! shouldContain "route '/x'"
                }
            }
        }

        "a requirement whose uri is not a route pattern is refused" {
            // The matrix is keyed on the pattern as the SERVER writes it. Anything else matches no
            // row, `ApiAcl` answers Denied — absence is how denial is transmitted — and the entry is
            // hidden from every user with nothing naming the cause.
            val thrown = shouldThrow<IllegalArgumentException> {
                registry().scopeFor("a").route(
                    path = "/x",
                    component = "pages/X.vue",
                    requiresAuth = true,
                    nav = TsSdkRegistry.Nav(
                        label = "X",
                        requires = listOf(TsSdkRegistry.ApiRouteRef("GET", "api/x", isPublic = false)),
                    ),
                )
            }

            thrown.message!! shouldContain "does not start with '/'"
        }

        //  Stylesheets — the SAME aggregate problem with the OPPOSITE collision rule  //////////////

        "stylesheets are ordered by cascade position, not by contributor order" {
            val forwards = registry().apply {
                scopeFor("feature").style("feature/f.css", order = 100)
                scopeFor("ui").style("ui/theme.css", order = 0)
            }

            val backwards = registry().apply {
                scopeFor("ui").style("ui/theme.css", order = 0)
                scopeFor("feature").style("feature/f.css", order = 100)
            }

            forwards.allStyles().map { it.path } shouldBe listOf("ui/theme.css", "feature/f.css")
            forwards.allStyles() shouldBe backwards.allStyles()
        }

        "equal orders break on path, so the emitted order is total" {
            // Otherwise two sheets at the same layer would emit in arrival order and `--check` would
            // report drift that is not real.
            val reg = registry().apply {
                scopeFor("b").style("z/b.css", order = 50)
                scopeFor("a").style("a/a.css", order = 50)
            }

            reg.allStyles().map { it.path } shouldBe listOf("a/a.css", "z/b.css")
        }

        "the SAME stylesheet registered twice at the same order deduplicates" {
            // DELIBERATELY not the route rule. `ui/theme.css` is a shared dependency: every module
            // shipping components that need it should be free to say so, or the app loads the theme
            // only when the one contributor blessed to register it happens to be in the profile.
            val reg = registry().apply {
                scopeFor("funktor:ui").style("ui/theme.css", order = 0)
                scopeFor("funktor:insights").style("ui/theme.css", order = 0)
            }

            reg.allStyles().map { it.path } shouldBe listOf("ui/theme.css")

            withClue("the FIRST registrant is recorded, so the message on a later conflict names it") {
                reg.allStyles().single().declaredBy shouldBe "funktor:ui"
            }
        }

        "the same stylesheet at CONFLICTING orders is a hard error naming both" {
            // There is no answer here that is not arbitrary, and the wrong one is silent — the
            // overrides simply stop applying.
            val reg = registry()
            reg.scopeFor("funktor:ui").style("ui/theme.css", order = 0)

            val thrown = shouldThrow<IllegalStateException> {
                reg.scopeFor("acme:app").style("ui/theme.css", order = 500)
            }

            thrown.message!! shouldContain "ui/theme.css"
            thrown.message!! shouldContain "funktor:ui"
            thrown.message!! shouldContain "acme:app"
            thrown.message!! shouldContain "0"
            thrown.message!! shouldContain "500"
        }

        "a stylesheet escaping the SDK root is refused" - {
            listOf("../outside/x.css", "/abs/x.css", "ui/../../x.css").forEach { path ->
                "rejects '$path'" {
                    val thrown = shouldThrow<IllegalArgumentException> {
                        registry().scopeFor("a").style(path, order = 0)
                    }

                    thrown.message!! shouldContain "relative to the SDK root"
                }
            }
        }
    }
}
