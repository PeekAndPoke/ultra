package io.peekandpoke.funktor.rest

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldNotBe
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
 * Locks the BOOT BACKSTOP: `ValidateRoutesOnAppStarting` is the only whole-chain check for auth
 * rules assembled outside the `authorize {}` DSL (a part-2 floor seed, a direct `copy(authRules)`).
 * A future change that widened the catch or dropped the aggregation would let a fail-open route
 * boot silently — these tests would catch that.
 */
class ValidateRoutesOnAppStartingSpec : StringSpec({

    fun validatorFor(vararg routes: ApiRoute<*>): ValidateRoutesOnAppStarting {
        val group = object : ApiRoutes("test") {
            init {
                routes.forEach { addRoute(it) }
            }
        }
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

    fun plainRoute(path: String, authRules: List<AuthRule<Unit, Unit>>) = ApiRoute.Plain(
        method = HttpMethod.Get,
        route = TypedRoute.Plain(pattern = UriPattern(path)),
        responseType = kType<Unit>(),
        authRules = authRules,
    )

    "a route whose chain buries a constant (built via copy, bypassing build()) aborts startup" {
        val bad = plainRoute(
            "/bad",
            listOf(OrAuthRule(listOf(AuthRule.forRole("admin"), PublicRule()))),
        )

        shouldThrow<AppStartException> {
            validatorFor(bad).validateOrThrow()
        }.message.let { msg ->
            msg shouldContain "Route validation failed"
            msg shouldContain "/bad"
            msg shouldContain "SOLE rule"
        }
    }

    "a healthy chain and an empty (public) chain both pass startup" {
        val healthy = plainRoute("/ok", listOf(AuthRule.isSuperUser()))
        val public = plainRoute("/open", emptyList())

        // Must not throw.
        validatorFor(healthy, public).validateOrThrow()
    }

    "aggregation: two bad routes both surface in the same AppStartException" {
        val bad1 = plainRoute("/bad-one", listOf(PublicRule<Unit, Unit>(), AuthRule.isSuperUser()))
        val bad2 = plainRoute("/bad-two", listOf(OrAuthRule(emptyList())))

        val ex = shouldThrow<AppStartException> {
            validatorFor(bad1, bad2).validateOrThrow()
        }
        ex.message shouldContain "/bad-one"
        ex.message shouldContain "/bad-two"
        ex.message shouldNotBe null
    }
})
