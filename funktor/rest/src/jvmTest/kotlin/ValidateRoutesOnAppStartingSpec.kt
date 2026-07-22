package io.peekandpoke.funktor.rest

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.string.shouldContain
import io.ktor.http.HttpMethod
import io.peekandpoke.funktor.core.broker.OutgoingConverter
import io.peekandpoke.funktor.core.broker.TypedRoute
import io.peekandpoke.funktor.core.broker.UriPattern
import io.peekandpoke.funktor.core.lifecycle.AppStartException
import io.peekandpoke.funktor.rest.auth.AuthRule
import io.peekandpoke.funktor.rest.auth.OrAuthRule
import io.peekandpoke.funktor.rest.auth.PublicRule
import io.peekandpoke.ultra.reflection.kType

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

    fun validatorFor(group: ApiRoutes): ValidateRoutesOnAppStarting {
        val feature = object : ApiFeature {
            override val name = "test-feature"
            override val description = "test"
            override fun getRouteGroups() = listOf(group)
        }
        return ValidateRoutesOnAppStarting(
            converter = OutgoingConverter(emptyList()),
            features = lazy { listOf(feature) },
        )
    }

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
})
