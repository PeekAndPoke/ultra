package io.peekandpoke.funktor.saas.isolation

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.http.HttpMethod
import io.peekandpoke.funktor.core.broker.TypedRoute
import io.peekandpoke.funktor.core.broker.UriPattern
import io.peekandpoke.funktor.core.lifecycle.AppStartException
import io.peekandpoke.funktor.rest.ApiFeature
import io.peekandpoke.funktor.rest.ApiRoute
import io.peekandpoke.funktor.rest.ApiRoutes
import io.peekandpoke.funktor.rest.GuardVerdict
import io.peekandpoke.funktor.rest.ValidateRoutesOnAppStarting
import io.peekandpoke.funktor.saas.domain.Organisation
import io.peekandpoke.ultra.reflection.TypeRef
import io.peekandpoke.ultra.reflection.kType
import io.peekandpoke.ultra.remote.ApiResponse
import io.peekandpoke.ultra.security.user.UserPermissions
import io.peekandpoke.ultra.vault.Ref
import io.peekandpoke.ultra.vault.Stored

// An org-owned entity + params over it.
private data class OwnedThing(override val org: Ref<Organisation>) : OrgAware
private data class OwnedParams(override val org: Stored<Organisation>, val thing: Stored<OwnedThing>) : OrgAwareParam

// Same entity resolved, but the params FORGET to declare OrgAwareParam — the boot-forced hole.
private data class UnboundParams(val orgRef: Stored<Organisation>, val thing: Stored<OwnedThing>)

// No org-owned entity at all.
private data class PlainParams(val id: String)

// A hand-written (non-data) params where the loaded entity `w` is a NON-`val` ctor param aliased to
// a differently-named readable property. The guard must still check it — regression guard for the
// round-3→4 narrowing (a ctor-param-NAME filter would have dropped `thing`).
private class AliasedParams(override val org: Stored<Organisation>, w: Stored<OwnedThing>) : OrgAwareParam {
    @Suppress("unused")
    val thing: Stored<OwnedThing> = w
}

class OrgIsolationSpec : StringSpec({

    fun <P : Any> paramRoute(paramsType: TypeRef<P>, pattern: String) =
        ApiRoute.WithParams<P, Unit>(
            method = HttpMethod.Get,
            route = TypedRoute.WithParams(paramsType, UriPattern(pattern)),
            responseType = kType<Unit>(),
            authRules = emptyList(),
        )

    //  OrgIsolationBootCheck  /////////////////////////////////////////////////////////////////////

    "boot: params resolving an OrgAware entity without OrgAwareParam fail with the fix" {
        val route = paramRoute(kType<UnboundParams>(), "/o/{orgRef}/{thing}")

        OrgIsolationBootCheck().validate(route).let { errors ->
            errors.size shouldBe 1
            errors.first() shouldContain "does not implement OrgAwareParam"
            errors.first() shouldContain "thing"
        }
    }

    "boot: params resolving an OrgAware entity WITH OrgAwareParam pass" {
        OrgIsolationBootCheck().validate(paramRoute(kType<OwnedParams>(), "/o/{org}/{thing}")) shouldBe emptyList()
    }

    "boot: params with no org-owned entity are not forced" {
        OrgIsolationBootCheck().validate(paramRoute(kType<PlainParams>(), "/p/{id}")) shouldBe emptyList()
    }

    "boot: the runner + OrgIsolationBootCheck ABORTS app start on an unbound OrgAware route" {
        // End-to-end composition: a real ValidateRoutesOnAppStarting runner fed the boot check plus a
        // feature whose route resolves an OrgAware entity without OrgAwareParam → aggregated AppStartException.
        val group = object : ApiRoutes("bad", authFloor = { authenticated() }) {
            val r = route {
                get<UnboundParams, ApiResponse<String>>("/o/{orgRef}/{thing}").handle { ApiResponse.ok("x") }
            }
        }
        val feature = object : ApiFeature {
            override val name = "bad-feature"
            override val description = "d"
            override fun getRouteGroups() = listOf(group)
        }
        val runner = ValidateRoutesOnAppStarting(
            checks = lazy { listOf(OrgIsolationBootCheck()) },
            features = lazy { listOf(feature) },
        )

        shouldThrow<AppStartException> { runner.validateOrThrow() }.message shouldContain "OrgAwareParam"
    }

    //  OrgIsolationGuard  /////////////////////////////////////////////////////////////////////////

    val guard = OrgIsolationGuard()

    fun org(key: String) = Stored(value = Organisation(slug = key, name = key), _id = "organisation/$key", _key = key)
    fun thingIn(orgKey: String) =
        Stored(value = OwnedThing(org = Ref("organisation/$orgKey") { error("not resolved in test") }), _id = "owned/x", _key = "x")

    "guard: matching selected org + consistent entity → Pass" {
        val p = OwnedParams(org = org("acme"), thing = thingIn("acme"))
        guard.guard(p, UserPermissions(org = "acme")) shouldBe GuardVerdict.Pass
    }

    "guard: caller selected a DIFFERENT org → DenyAsNotFound (caller-binding)" {
        val p = OwnedParams(org = org("acme"), thing = thingIn("acme"))
        // Even a multi-org member: accessibleOrgs lists acme, but the SELECTED org is globex.
        guard.guard(p, UserPermissions(org = "globex", accessibleOrgs = setOf("acme", "globex"))) shouldBe
                GuardVerdict.DenyAsNotFound
    }

    "guard: entity belongs to a foreign org → DenyAsNotFound (org-consistency)" {
        val p = OwnedParams(org = org("acme"), thing = thingIn("globex"))
        guard.guard(p, UserPermissions(org = "acme")) shouldBe GuardVerdict.DenyAsNotFound
    }

    "guard: super-user passes caller-binding for any org" {
        val p = OwnedParams(org = org("acme"), thing = thingIn("acme"))
        guard.guard(p, UserPermissions(isSuperUser = true)) shouldBe GuardVerdict.Pass
    }

    "guard: non-OrgAwareParam params abstain (Pass)" {
        guard.guard(PlainParams("x"), UserPermissions(org = "acme")) shouldBe GuardVerdict.Pass
    }

    "guard: an aliased (non-val ctor) OrgAware entity is still checked — foreign org denied" {
        guard.guard(AliasedParams(org = org("acme"), w = thingIn("globex")), UserPermissions(org = "acme")) shouldBe
                GuardVerdict.DenyAsNotFound
    }

    "guard: an aliased consistent entity passes" {
        guard.guard(AliasedParams(org = org("acme"), w = thingIn("acme")), UserPermissions(org = "acme")) shouldBe
                GuardVerdict.Pass
    }
})
