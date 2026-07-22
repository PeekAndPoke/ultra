package io.peekandpoke.funktor.rest

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.string.shouldContain
import io.ktor.http.HttpMethod
import io.peekandpoke.funktor.core.broker.ConsistentParam
import io.peekandpoke.funktor.core.broker.OutgoingConverter
import io.peekandpoke.funktor.core.broker.TypedRoute
import io.peekandpoke.funktor.core.broker.UriPattern
import io.peekandpoke.funktor.core.broker.vault.OutgoingVaultConverter
import io.peekandpoke.funktor.core.lifecycle.AppStartException
import io.peekandpoke.funktor.rest.auth.AuthRule
import io.peekandpoke.funktor.rest.auth.OrAuthRule
import io.peekandpoke.funktor.rest.auth.PublicRule
import io.peekandpoke.ultra.reflection.kType
import io.peekandpoke.ultra.vault.Stored

private data class Ent1(val name: String)
private data class Ent2(val name: String)

/** Two resolved entity refs, no consistency declaration — the boot-forced shape. */
private data class TwoEntities(val a: Stored<Ent1>, val b: Stored<Ent2>)

/** Two resolved entity refs WITH a consistency declaration — satisfies the boot check. */
private data class TwoEntitiesConsistent(val a: Stored<Ent1>, val b: Stored<Ent2>) : ConsistentParam {
    override fun isConsistent(): Boolean = true
}

/** A single entity ref — inconsistency is impossible, so no declaration is required. */
private data class OneEntity(val a: Stored<Ent1>)

/**
 * `ApiRoutes.addRoute` is the single floor-applying choke point: it prepends the group floor and
 * validates the whole chain (non-empty + constant-soleness) at CONSTRUCTION, so a fail-open route
 * can never be registered. [ValidateRoutesOnAppStarting] re-runs the same checks at boot as
 * defense-in-depth alongside the converter checks.
 */
class ValidateRoutesOnAppStartingSpec : StringSpec({

    fun plainRoute(path: String, authRules: List<AuthRule<Unit, Unit>>) = ApiRoute.Plain(
        method = HttpMethod.Get,
        route = TypedRoute.Plain(pattern = UriPattern(path)),
        responseType = kType<Unit>(),
        authRules = authRules,
    )

    // Group floor is { isSuperUser() }; addRoute prepends it to every added route.
    fun groupWith(vararg routes: ApiRoute<*>): ApiRoutes =
        object : ApiRoutes("test", defaultAuth = { isSuperUser() }) {
            init {
                routes.forEach { addRoute(it) }
            }
        }

    // A public (sole-constant) group — routes here are exempt from the auto-rules + boot forcing.
    fun publicGroupWith(vararg routes: ApiRoute<*>): ApiRoutes =
        object : ApiRoutes("test-public", defaultAuth = { public() }) {
            init {
                routes.forEach { addRoute(it) }
            }
        }

    fun validatorFor(group: ApiRoutes, converter: OutgoingConverter = OutgoingConverter(emptyList())): ValidateRoutesOnAppStarting {
        val feature = object : ApiFeature {
            override val name = "test-feature"
            override val description = "test"
            override fun getRouteGroups() = listOf(group)
        }
        return ValidateRoutesOnAppStarting(
            converter = converter,
            features = lazy { listOf(feature) },
        )
    }

    // A converter that CAN handle Stored<*> params, so entity-param routes don't trip the converter
    // branch — isolating the ConsistentParam-forcing assertions.
    val vaultConverter = OutgoingConverter(listOf(OutgoingVaultConverter()))

    "a route whose own rules bury a constant is rejected at construction (addRoute)" {
        // The floor prepends isSuperUser(); the route's OrAuthRule buries a PublicRule that would
        // grant everyone — the combined chain has a non-sole constant → rejected.
        shouldThrow<IllegalStateException> {
            groupWith(
                plainRoute("/bad", listOf(OrAuthRule(listOf(AuthRule.forRole("admin"), PublicRule()))))
            )
        }.message shouldContain "SOLE rule"
    }

    "a route added with no rules of its own still gets the floor (non-empty) and passes" {
        val group = groupWith(plainRoute("/ok", emptyList()))

        // The floor made it non-empty; construction succeeded. The boot validator agrees.
        validatorFor(group).validateOrThrow()
    }

    "a healthy group with a strengthening route passes the boot validator" {
        val group = groupWith(
            plainRoute("/a", emptyList()),
            plainRoute("/b", listOf(AuthRule.forUserType("OperatorUser"))),
        )

        validatorFor(group).validateOrThrow()
    }

    // The auth-chain FAILURE path of validateOrThrow is unreachable via a normal group (addRoute
    // gates registration, so a bad chain never enters `all`). Its throw + aggregation MECHANISM
    // — retained as defense-in-depth for part-3's post-registration auto-rules — is still reachable
    // through the CONVERTER branch, which this exercises.
    "validateOrThrow aggregates and throws AppStartException on a converter failure" {
        data class UnhandledParam(val x: String)

        val paramRoute = ApiRoute.WithParams<UnhandledParam, Unit>(
            method = HttpMethod.Get,
            route = TypedRoute.WithParams(kType<UnhandledParam>(), UriPattern("/p/{x}")),
            responseType = kType<Unit>(),
            authRules = listOf(AuthRule.isSuperUser()),
        )
        val group = groupWith(paramRoute)

        // OutgoingConverter(emptyList()) can't handle the param → aggregated → AppStartException.
        shouldThrow<AppStartException> {
            validatorFor(group).validateOrThrow()
        }.message shouldContain "Route validation failed"
    }

    //  ConsistentParam boot forcing (part 3)  /////////////////////////////////////////////////////

    "a non-public route with 2+ entity params but no ConsistentParam fails boot with the fix" {
        val route = ApiRoute.WithParams<TwoEntities, Unit>(
            method = HttpMethod.Get,
            route = TypedRoute.WithParams(kType<TwoEntities>(), UriPattern("/x/{a}/{b}")),
            responseType = kType<Unit>(),
            authRules = emptyList(),
        )
        val group = groupWith(route)

        val message = shouldThrow<AppStartException> {
            validatorFor(group, vaultConverter).validateOrThrow()
        }.message

        message shouldContain "does not implement ConsistentParam"
        message shouldContain "isConsistent()"
    }

    "a non-public route with 2+ entity params that implements ConsistentParam passes boot" {
        val route = ApiRoute.WithParams<TwoEntitiesConsistent, Unit>(
            method = HttpMethod.Get,
            route = TypedRoute.WithParams(kType<TwoEntitiesConsistent>(), UriPattern("/x/{a}/{b}")),
            responseType = kType<Unit>(),
            authRules = emptyList(),
        )

        validatorFor(groupWith(route), vaultConverter).validateOrThrow()
    }

    "a route with a single entity param is not forced to implement ConsistentParam" {
        val route = ApiRoute.WithParams<OneEntity, Unit>(
            method = HttpMethod.Get,
            route = TypedRoute.WithParams(kType<OneEntity>(), UriPattern("/x/{a}")),
            responseType = kType<Unit>(),
            authRules = emptyList(),
        )

        validatorFor(groupWith(route), vaultConverter).validateOrThrow()
    }

    "a public (sole-constant) route with 2+ entity params is exempt from the forcing" {
        val route = ApiRoute.WithParams<TwoEntities, Unit>(
            method = HttpMethod.Get,
            route = TypedRoute.WithParams(kType<TwoEntities>(), UriPattern("/x/{a}/{b}")),
            responseType = kType<Unit>(),
            authRules = emptyList(),
        )

        // Public floor ⇒ sole-constant chain ⇒ no auto-rule, no forcing.
        validatorFor(publicGroupWith(route), vaultConverter).validateOrThrow()
    }
})
