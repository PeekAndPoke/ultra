package io.peekandpoke.funktor.rest.auth

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.http.HttpMethod
import io.peekandpoke.funktor.core.broker.TypedRoute
import io.peekandpoke.funktor.core.broker.UriPattern
import io.peekandpoke.funktor.rest.ApiRoute
import io.peekandpoke.ultra.reflection.kType
import io.peekandpoke.ultra.remote.ApiAccessLevel
import io.peekandpoke.ultra.security.user.User
import io.peekandpoke.ultra.security.user.UserPermissions
import io.peekandpoke.ultra.security.user.UserRecord

/**
 * Locks the accumulator semantics of the authorize {} DSL — see
 * `.claude/tasks/20260722-authorize-rule-builder.md`, ambiguity inventory.
 */
class AuthRuleBuilderSpec : StringSpec({

    fun route(): ApiRoute.Plain<Unit> = ApiRoute.Plain(
        method = HttpMethod.Get,
        route = TypedRoute.Plain(pattern = UriPattern("/test")),
        responseType = kType<Unit>(),
    )

    fun estimateCtx(
        isSuperUser: Boolean = false,
        roles: Set<String> = emptySet(),
        userType: String? = null,
    ) = AuthRule.EstimateCtx(
        user = User(
            record = UserRecord.LoggedIn(userId = "test-user", type = userType),
            permissions = UserPermissions(isSuperUser = isSuperUser, roles = roles),
        )
    )

    //  Inventory #1 — the original footgun: every statement is enforced  ///////////////////////

    "two statements in authorize {} enforce BOTH rules (the last-expression-wins footgun is dead)" {
        val authorized = route().authorize {
            isSuperUser()
            forUserType("OperatorUser")
        }

        authorized.authRules shouldHaveSize 2

        // super-user WITHOUT the user type must be denied — the first rule alone must not win
        authorized.estimateAccess(estimateCtx(isSuperUser = true, userType = "AppUser")) shouldBe
                ApiAccessLevel.Denied
        // both satisfied => granted
        authorized.estimateAccess(estimateCtx(isSuperUser = true, userType = "OperatorUser")) shouldBe
                ApiAccessLevel.Granted
    }

    //  Inventory #2 — block-style composition builds the literal tree  /////////////////////////

    "forAny {} statements are disjuncts" {
        val authorized = route().authorize {
            forAny {
                isSuperUser()
                forRole("support")
            }
        }

        authorized.authRules shouldHaveSize 1
        authorized.authRules.first().shouldBeInstanceOf<OrAuthRule<*, *>>()

        authorized.estimateAccess(estimateCtx(isSuperUser = true)) shouldBe ApiAccessLevel.Granted
        authorized.estimateAccess(estimateCtx(roles = setOf("support"))) shouldBe ApiAccessLevel.Granted
        authorized.estimateAccess(estimateCtx()) shouldBe ApiAccessLevel.Denied
    }

    "forAll {} nested inside forAny {} builds the written tree: OR(super, AND(type, role))" {
        val authorized = route().authorize {
            forAny {
                isSuperUser()
                forAll {
                    forUserType("B2bUser")
                    forRole("org-admin")
                }
            }
        }

        // super-user alone
        authorized.estimateAccess(estimateCtx(isSuperUser = true)) shouldBe ApiAccessLevel.Granted
        // the full conjunction
        authorized.estimateAccess(estimateCtx(userType = "B2bUser", roles = setOf("org-admin"))) shouldBe
                ApiAccessLevel.Granted
        // only half the conjunction — must NOT pass (an accumulator distortion would AND everything)
        authorized.estimateAccess(estimateCtx(userType = "B2bUser")) shouldBe ApiAccessLevel.Denied
        authorized.estimateAccess(estimateCtx(roles = setOf("org-admin"))) shouldBe ApiAccessLevel.Denied
    }

    //  Inventory #5 — constant rules only as the sole rule  ////////////////////////////////////

    "public() as the sole rule is legal" {
        val authorized = route().authorize { public() }

        authorized.authRules shouldHaveSize 1
        authorized.estimateAccess(estimateCtx()) shouldBe ApiAccessLevel.Granted
    }

    "forbidden() as the sole rule is legal and denies everyone" {
        val authorized = route().authorize { forbidden() }

        authorized.authRules shouldHaveSize 1
        authorized.authRules.first().shouldBeInstanceOf<ForbiddenRule<*, *>>()
        authorized.estimateAccess(estimateCtx(isSuperUser = true)) shouldBe ApiAccessLevel.Denied
    }

    "forbidden() combined with another rule fails at build time" {
        shouldThrow<IllegalStateException> {
            route().authorize {
                forbidden()
                isSuperUser()
            }
        }.message shouldContain "SOLE rule"
    }

    "a leading deny is enforced even when the last statement grants (old last-wins would grant)" {
        val authorized = route().authorize {
            forRole("required-role")
            authenticated()
        }

        // Authenticated user WITHOUT the role: under the old last-expression-wins semantics only
        // authenticated() would survive and GRANT — the accumulator must deny.
        authorized.estimateAccess(estimateCtx()) shouldBe ApiAccessLevel.Denied
        authorized.estimateAccess(estimateCtx(roles = setOf("required-role"))) shouldBe ApiAccessLevel.Granted
    }

    "public() combined with another rule fails at build time" {
        shouldThrow<IllegalStateException> {
            route().authorize {
                public()
                isSuperUser()
            }
        }.message shouldContain "SOLE rule"
    }

    "a composite that merely CONTAINS a constant is rejected even as the sole rule" {
        // Reads role-gated, but the PublicRule disjunct would grant everyone — must be a boot error.
        shouldThrow<IllegalStateException> {
            route().authorize {
                appendRule(OrAuthRule(listOf(AuthRule.forRole("admin"), PublicRule())))
            }
        }.message shouldContain "SOLE rule"
    }

    "an empty AND composite appended programmatically is rejected (folds to allow-all)" {
        shouldThrow<IllegalStateException> {
            route().authorize {
                appendRule(AndAuthRule(emptyList()))
            }
        }.message shouldContain "empty"
    }

    "an empty composite inside a sub-block is rejected" {
        shouldThrow<IllegalStateException> {
            route().authorize {
                forAny {
                    isSuperUser()
                    appendRule(OrAuthRule(emptyList()))
                }
            }
        }.message shouldContain "empty"
    }

    "a constant smuggled into a sub-block via appendRule fails at build time" {
        shouldThrow<IllegalStateException> {
            route().authorize {
                forAny {
                    appendRule(PublicRule())
                    isSuperUser()
                }
            }
        }.message shouldContain "constant rules"
    }

    //  Inventory #6 — empty chains and empty sub-blocks  ///////////////////////////////////////

    "an empty authorize {} block fails at build time" {
        shouldThrow<IllegalStateException> {
            route().authorize { }
        }.message shouldContain "declared no rules"
    }

    "an empty forAny {} fails at build time" {
        shouldThrow<IllegalStateException> {
            route().authorize {
                forAny { }
            }
        }.message shouldContain "forAny"
    }

    "an empty forAll {} fails at build time" {
        shouldThrow<IllegalStateException> {
            route().authorize {
                isSuperUser()
                forAll { }
            }
        }.message shouldContain "forAll"
    }

    //  Inventory #7 — one authorize block per route  ///////////////////////////////////////////

    "a second authorize {} on the same route fails at build time" {
        val once = route().authorize { isSuperUser() }

        shouldThrow<IllegalStateException> {
            once.authorize { forRole("x") }
        }.message shouldContain "once per route"
    }

    //  Whole-chain validator — catches what per-block build() cannot see  ///////////////////////

    "validateChain rejects a public constant coexisting with a restrictive rule in a combined chain" {
        // Simulates a part-2 floor seed of [public()] plus a route's own authorize { isSuperUser() }:
        // each block validates fine alone, but the combined chain is a silent no-op'd public route.
        val combined = listOf<AuthRule<*, *>>(PublicRule<Unit, Unit>(), AuthRule.isSuperUser<Unit, Unit>())

        shouldThrow<IllegalStateException> {
            AuthRuleBuilder.validateChain("test chain", combined)
        }.message shouldContain "SOLE rule"
    }

    "validateChain accepts a healthy multi-rule chain and a sole bare constant" {
        // Neither of these should throw.
        AuthRuleBuilder.validateChain(
            "test chain",
            listOf<AuthRule<*, *>>(
                AuthRule.isSuperUser<Unit, Unit>(),
                AuthRule.forUserType<Unit, Unit>("OperatorUser"),
            ),
        )
        AuthRuleBuilder.validateChain("test chain", listOf<AuthRule<*, *>>(PublicRule<Unit, Unit>()))
    }

    "validateChain rejects a chain assembled by a direct copy that buries a constant" {
        // A direct copy(authRules = ...) bypasses build() entirely — the boot backstop must catch it.
        val smuggled = route().copy(
            authRules = listOf(OrAuthRule(listOf(AuthRule.forRole("admin"), PublicRule())))
        )

        shouldThrow<IllegalStateException> {
            AuthRuleBuilder.validateChain("test chain", smuggled.authRules)
        }.message shouldContain "SOLE rule"
    }

    //  Inventory #9 — the programmatic bridge composes safely  /////////////////////////////////

    "appendRule with companion-built composition does not double-register" {
        val authorized = route().authorize {
            appendRule(
                OrAuthRule(
                    listOf(
                        AuthRule.isSuperUser(),
                        AuthRule.forRole("support"),
                    )
                )
            )
        }

        authorized.authRules shouldHaveSize 1
        authorized.estimateAccess(estimateCtx(roles = setOf("support"))) shouldBe ApiAccessLevel.Granted
        authorized.estimateAccess(estimateCtx()) shouldBe ApiAccessLevel.Denied
    }
})
