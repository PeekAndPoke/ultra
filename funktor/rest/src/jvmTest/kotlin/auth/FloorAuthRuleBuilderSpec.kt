package io.peekandpoke.funktor.rest.auth

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.peekandpoke.funktor.core.broker.OutgoingConverter
import io.peekandpoke.funktor.rest.ApiFeature
import io.peekandpoke.funktor.rest.ApiRoutes
import io.peekandpoke.funktor.rest.AuthChainBootCheck
import io.peekandpoke.funktor.rest.ConverterCompatBootCheck
import io.peekandpoke.funktor.rest.ValidateRoutesOnAppStarting
import io.peekandpoke.ultra.remote.ApiAccessLevel
import io.peekandpoke.ultra.remote.ApiResponse
import io.peekandpoke.ultra.remote.TypedApiEndpoint
import io.peekandpoke.ultra.remote.api
import io.peekandpoke.ultra.security.user.User
import io.peekandpoke.ultra.security.user.UserPermissions
import io.peekandpoke.ultra.security.user.UserRecord
import kotlinx.serialization.builtins.serializer

/**
 * Locks the ApiRoutes floor (`authFloor = { ... }`): every route inherits the floor as its
 * initial chain, a per-route `authorize {}` can only strengthen it, and a missing/empty floor
 * aborts group construction. See `.claude/tasks/20260722-apiroutes-auth-floor.md`.
 */
class FloorAuthRuleBuilderSpec : StringSpec({

    val endpoint = TypedApiEndpoint.Get(uri = "/thing", response = Unit.serializer().api())

    fun estimateCtx(isSuperUser: Boolean = false, userType: String? = null) = AuthRule.EstimateCtx(
        user = User(
            record = UserRecord.LoggedIn(userId = "u", type = userType),
            permissions = UserPermissions(isSuperUser = isSuperUser),
        )
    )

    "every route in a group inherits the floor with no per-route authorize" {
        val group = object : ApiRoutes("g", authFloor = { isSuperUser() }) {
            val route = endpoint.mount { handle { ApiResponse.ok(Unit) } }
        }

        val route = group.all.single()
        route.authRules shouldHaveSize 1
        route.estimateAccess(estimateCtx(isSuperUser = true)) shouldBe ApiAccessLevel.Granted
        route.estimateAccess(estimateCtx(isSuperUser = false)) shouldBe ApiAccessLevel.Denied
    }

    "a per-route authorize STRENGTHENS the floor (both floor and route rule enforced)" {
        val group = object : ApiRoutes("g", authFloor = { isSuperUser() }) {
            val route = endpoint.mount {
                authorize { forUserType("OperatorUser") }
                    .handle { ApiResponse.ok(Unit) }
            }
        }

        val route = group.all.single()
        // floor (superuser) + route rule (user-type) = 2 rules, both must pass
        route.authRules shouldHaveSize 2
        route.estimateAccess(estimateCtx(isSuperUser = true, userType = "OperatorUser")) shouldBe ApiAccessLevel.Granted
        // superuser but wrong type → the strengthening rule denies
        route.estimateAccess(estimateCtx(isSuperUser = true, userType = "AppUser")) shouldBe ApiAccessLevel.Denied
        // right type but not superuser → the floor denies
        route.estimateAccess(estimateCtx(isSuperUser = false, userType = "OperatorUser")) shouldBe ApiAccessLevel.Denied
    }

    "the floor is PREPENDED (floor rule precedes the route rule in the chain)" {
        val group = object : ApiRoutes("g", authFloor = { isSuperUser() }) {
            val route = endpoint.mount {
                authorize { forUserType("X") }.handle { ApiResponse.ok(Unit) }
            }
        }
        val rules = group.all.single().authRules
        rules[0].description shouldContain "SuperUser"
        rules[1].description shouldContain "type"
    }

    "a public floor makes routes public without any per-route authorize" {
        val group = object : ApiRoutes("g", authFloor = { public() }) {
            val route = endpoint.mount { handle { ApiResponse.ok(Unit) } }
        }
        group.all.single().estimateAccess(estimateCtx()) shouldBe ApiAccessLevel.Granted
    }

    "an empty floor aborts group construction" {
        shouldThrow<IllegalStateException> {
            object : ApiRoutes("g", authFloor = { }) {}
        }.message shouldContain "declared no rules"
    }

    "a floor combining public() with a restrictive rule aborts group construction" {
        shouldThrow<IllegalStateException> {
            object : ApiRoutes("g", authFloor = { public(); isSuperUser() }) {}
        }.message shouldContain "SOLE rule"
    }

    "a public-floored group with a route that tries to add a restrictive rule fails at build" {
        // The route's authorize adds isSuperUser() on top of the public() floor → the whole chain
        // is [PublicRule, isSuperUser] → constant-not-sole → boot error at route construction.
        shouldThrow<IllegalStateException> {
            object : ApiRoutes("g", authFloor = { public() }) {
                val route = endpoint.mount {
                    authorize { isSuperUser() }.handle { ApiResponse.ok(Unit) }
                }
            }
        }.message shouldContain "SOLE rule"
    }

    "a healthy floored group passes the boot validator" {
        val group = object : ApiRoutes("g", authFloor = { isSuperUser() }) {
            val route = endpoint.mount { handle { ApiResponse.ok(Unit) } }
        }
        val feature = object : ApiFeature {
            override val name = "f"
            override val description = "d"
            override fun getRouteGroups() = listOf<ApiRoutes>(group)
        }
        // Every route already passed addRoute's whole-chain validation at construction; the boot
        // validator re-runs it (via the injected AuthChainBootCheck) as defense-in-depth and agrees.
        ValidateRoutesOnAppStarting(
            checks = lazy { listOf(ConverterCompatBootCheck(OutgoingConverter(emptyList())), AuthChainBootCheck()) },
            features = lazy { listOf(feature) },
        ).validateOrThrow()
    }

    "a constant inside a floor forAny{} aborts group construction" {
        // FloorAuthRuleBuilder's combinators reuse the floor builder (which exposes public()), so
        // this COMPILES — unlike authorize{}'s forAny{} where it's a compile error — but the floor's
        // build()->validateChain recurses and rejects the nested constant at construction.
        shouldThrow<IllegalStateException> {
            object : ApiRoutes("g", authFloor = { forAny { public(); isSuperUser() } }) {}
        }.message shouldContain "SOLE rule"
    }

    "an empty floor forAny{} aborts group construction" {
        shouldThrow<IllegalStateException> {
            object : ApiRoutes("g", authFloor = { forAny { } }) {}
        }.message shouldContain "empty"
    }

    "a route created via the low-level route{} path is also floored" {
        val group = object : ApiRoutes("g", authFloor = { isSuperUser() }) {
            init {
                route {
                    routeBuilder.get<Unit>("/x").handle { ApiResponse.ok(Unit) }
                }
            }
        }
        group.all.single().authRules shouldHaveSize 1
        group.all.single().estimateAccess(estimateCtx(isSuperUser = true)) shouldBe ApiAccessLevel.Granted
    }
})
