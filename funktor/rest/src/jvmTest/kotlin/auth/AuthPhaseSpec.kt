package io.peekandpoke.funktor.rest.auth

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
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
 * Phase classification ([isCallerOnly]) and phase-1 evaluation ([ApiRoute.phase1Denials]) for the
 * two-phase auth split. Phase 1 (caller-only) runs before param conversion; phase 2 (param-
 * dependent) after. See `AuthPhase.kt`.
 */
class AuthPhaseSpec : StringSpec({

    val superUser = User(
        record = UserRecord.LoggedIn(userId = "root"),
        permissions = UserPermissions(isSuperUser = true),
    )
    val plainUser = User(
        record = UserRecord.LoggedIn(userId = "alice"),
        permissions = UserPermissions(),
    )

    fun route(authRules: List<AuthRule<Unit, Unit>>) = ApiRoute.Plain(
        method = HttpMethod.Get,
        route = TypedRoute.Plain(pattern = UriPattern("/x")),
        responseType = kType<Unit>(),
        authRules = authRules,
    )

    //  isCallerOnly classification  ///////////////////////////////////////////////////////////////

    "constant rules are caller-only (phase 1)" {
        AuthRule.public<Unit, Unit>().isCallerOnly().shouldBeTrue()
        AuthRule.forbidden<Unit, Unit>().isCallerOnly().shouldBeTrue()
    }

    "permission / access-level rules are caller-only (phase 1)" {
        AuthRule.isSuperUser<Unit, Unit>().isCallerOnly().shouldBeTrue()
        AuthRule.authenticated<Unit, Unit>().isCallerOnly().shouldBeTrue()
        AuthRule.forUserType<Unit, Unit>("Op").isCallerOnly().shouldBeTrue()
        AuthRule.forOrganisation<Unit, Unit>("acme").isCallerOnly().shouldBeTrue()
    }

    "forCall {} (CallCheck) is param-dependent (phase 2)" {
        AuthRule.forCall<Unit, Unit>("custom") { true }.isCallerOnly().shouldBeFalse()
    }

    "the framework param auto-rules are param-dependent (phase 2)" {
        ConsistentParamRule().isCallerOnly().shouldBeFalse()
        CallerScopedParamRule().isCallerOnly().shouldBeFalse()
    }

    "a composite of only caller-only rules is caller-only" {
        AndAuthRule(listOf(AuthRule.isSuperUser(), AuthRule.authenticated<Unit, Unit>())).isCallerOnly().shouldBeTrue()
        OrAuthRule(listOf(AuthRule.forRole("a"), AuthRule.forGroup<Unit, Unit>("b"))).isCallerOnly().shouldBeTrue()
    }

    "a composite that mixes in a param-dependent member is param-dependent as a whole" {
        AndAuthRule(listOf(AuthRule.isSuperUser<Unit, Unit>(), AuthRule.forCall("c") { true }))
            .isCallerOnly().shouldBeFalse()
        OrAuthRule(listOf(AuthRule.forRole<Unit, Unit>("a"), AuthRule.forCall("c") { true }))
            .isCallerOnly().shouldBeFalse()
    }

    //  Auto-rule contract  ////////////////////////////////////////////////////////////////////////

    "param auto-rules hide their failure as not-found" {
        ConsistentParamRule().shouldBeInstanceOf<HideFailureAsNotFound>()
        CallerScopedParamRule().shouldBeInstanceOf<HideFailureAsNotFound>()
    }

    "param auto-rules estimate Granted (they are request-shape guards, not permissions)" {
        ConsistentParamRule().estimate(AuthRule.EstimateCtx.of(plainUser)) shouldBe ApiAccessLevel.Granted
        CallerScopedParamRule().estimate(AuthRule.EstimateCtx.of(plainUser)) shouldBe ApiAccessLevel.Granted
    }

    //  phase1Denials  /////////////////////////////////////////////////////////////////////////////

    "phase1Denials returns the caller-only rules that deny, ignoring param-phase rules" {
        // isSuperUser denies for a plain user; the forCall is phase-2 → not evaluated in phase 1.
        val superRule = AuthRule.isSuperUser<Unit, Unit>()
        val r = route(listOf(superRule, AuthRule.forCall("c") { true }))

        r.phase1Denials(AuthRule.EstimateCtx.of(plainUser)) shouldBe listOf(superRule)
    }

    "phase1Denials is empty when all caller-only rules pass — even if a param rule would deny" {
        // The param-phase forCall would fail, but phase 1 must not see it (no params yet).
        val r = route(listOf(AuthRule.isSuperUser(), AuthRule.forCall("c") { false }))

        r.phase1Denials(AuthRule.EstimateCtx.of(superUser)) shouldBe emptyList()
    }

    //  flattenTopLevelAnds — "AND splits its members"  ////////////////////////////////////////////

    "flattenTopLevelAnds splits nested top-level ANDs but leaves ORs intact" {
        val a = AuthRule.isSuperUser<Unit, Unit>()
        val b = AuthRule.authenticated<Unit, Unit>()
        val or = OrAuthRule(listOf(AuthRule.forRole<Unit, Unit>("x"), AuthRule.forGroup<Unit, Unit>("y")))
        val nested = AndAuthRule(listOf(a, AndAuthRule(listOf(b, or))))

        flattenTopLevelAnds(listOf(nested)) shouldBe listOf(a, b, or)
    }

    "phase1Denials flattens a mixed top-level forAll so its caller-only conjunct still gates phase 1" {
        // forAll { isSuperUser(); forCall { … } } — the isSuperUser conjunct must deny in phase 1
        // (before conversion) for a non-super caller, not be demoted to phase 2 with the forCall.
        val superRule = AuthRule.isSuperUser<Unit, Unit>()
        val mixed = AndAuthRule(listOf(superRule, AuthRule.forCall<Unit, Unit>("c") { true }))
        val r = route(listOf(mixed))

        r.phase1Denials(AuthRule.EstimateCtx.of(plainUser)) shouldBe listOf(superRule)
    }
})
