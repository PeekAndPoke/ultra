package io.peekandpoke.funktor.rest

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
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
 * [ValidateRoutesOnAppStarting] is a thin runner: it feeds every route to every injected
 * [RouteBootCheck] and aggregates the errors. This spec exercises the runner plus the two REST
 * checks it ships ([ConverterCompatBootCheck], [AuthChainBootCheck]). `ApiRoutes.addRoute` still
 * rejects a fail-open chain at CONSTRUCTION, so the auth-chain check is a boot backstop.
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

    fun validatorFor(
        group: ApiRoutes,
        converter: OutgoingConverter = OutgoingConverter(emptyList()),
    ): ValidateRoutesOnAppStarting {
        val feature = object : ApiFeature {
            override val name = "test-feature"
            override val description = "test"
            override fun getRouteGroups() = listOf(group)
        }
        return ValidateRoutesOnAppStarting(
            checks = lazy { listOf(ConverterCompatBootCheck(converter), AuthChainBootCheck()) },
            features = lazy { listOf(feature) },
        )
    }

    //  addRoute construction gate (unchanged)  ////////////////////////////////////////////////////

    "a route whose own rules bury a constant is rejected at construction (addRoute)" {
        shouldThrow<IllegalStateException> {
            groupWith(
                plainRoute("/bad", listOf(OrAuthRule(listOf(AuthRule.forRole("admin"), PublicRule()))))
            )
        }.message shouldContain "SOLE rule"
    }

    "a route added with no rules of its own still gets the floor (non-empty) and passes" {
        validatorFor(groupWith(plainRoute("/ok", emptyList()))).validateOrThrow()
    }

    "a healthy group with a strengthening route passes the boot validator" {
        val group = groupWith(
            plainRoute("/a", emptyList()),
            plainRoute("/b", listOf(AuthRule.forUserType("OperatorUser"))),
        )
        validatorFor(group).validateOrThrow()
    }

    //  ConverterCompatBootCheck via the runner  ///////////////////////////////////////////////////

    "the runner aggregates and throws AppStartException on a converter failure" {
        data class UnhandledParam(val x: String)

        val paramRoute = ApiRoute.WithParams<UnhandledParam, Unit>(
            method = HttpMethod.Get,
            route = TypedRoute.WithParams(kType<UnhandledParam>(), UriPattern("/p/{x}")),
            responseType = kType<Unit>(),
            authRules = listOf(AuthRule.isSuperUser()),
        )

        // OutgoingConverter(emptyList()) can't handle the param → aggregated → AppStartException.
        shouldThrow<AppStartException> {
            validatorFor(groupWith(paramRoute)).validateOrThrow()
        }.message shouldContain "Route validation failed"
    }

    //  The checks in isolation  ///////////////////////////////////////////////////////////////////

    "AuthChainBootCheck flags a chain that buries a constant (backstop for non-DSL assembly)" {
        // Bypass addRoute: hand-build a route with a fail-open chain the DSL would have rejected.
        val bad = plainRoute("/x", listOf(OrAuthRule(listOf(AuthRule.forRole("admin"), PublicRule()))))
        AuthChainBootCheck().validate(bad).let { errors ->
            errors.size shouldBe 1
            errors.first() shouldContain "SOLE rule"
        }
    }

    "AuthChainBootCheck passes a healthy chain" {
        AuthChainBootCheck().validate(plainRoute("/x", listOf(AuthRule.isSuperUser()))) shouldBe emptyList()
    }
})
