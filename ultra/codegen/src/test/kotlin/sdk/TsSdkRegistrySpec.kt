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
    }
}
