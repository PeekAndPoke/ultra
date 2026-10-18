package io.peekandpoke.funktor.codegen

import io.peekandpoke.funktor.codegen.cli.TsSdkGenerateCliCommand
import io.peekandpoke.funktor.rest.ApiFeature
import io.peekandpoke.funktor.rest.ApiRoute
import io.peekandpoke.funktor.rest.docs.codeGen
import io.peekandpoke.ultra.codegen.contributors.KotlinxJsonTsContributor
import io.peekandpoke.ultra.codegen.contributors.MpDateTimeTsContributor
import io.peekandpoke.ultra.codegen.sdk.TsSdkBuilder
import io.peekandpoke.ultra.codegen.sdk.TsSdkContributor
import io.peekandpoke.ultra.kontainer.KontainerBuilder
import io.peekandpoke.ultra.kontainer.module
import io.peekandpoke.ultra.slumber.SlumberConfig

/**
 * Registers the TypeScript SDK generator.
 *
 * **Opt-in, and deliberately NOT part of the all-in-one `Funktor` module.** Code generation is a
 * dev-time concern; a production server should not carry the generator, its reflection walk, or its
 * jar of TypeScript resources.
 *
 * ```kotlin
 * kontainer {
 *     funktorCodegen()
 *     singleton(MyTypesTsContributor::class)   // user contributors register alongside
 * }
 * ```
 */
fun KontainerBuilder.funktorCodegen(
    builder: FunktorCodegenBuilder.() -> Unit = {},
) = module(Funktor_Codegen, builder)

/** Extension point for configuring the generator; mirrors the other funktor module builders. */
class FunktorCodegenBuilder(private val kontainer: KontainerBuilder) {

    /** Registers an additional contributor. Equivalent to `singleton(T::class)` next to the module. */
    inline fun <reified T : TsSdkContributor> contributor() {
        builder().singleton(T::class)
    }

    /**
     * Narrows which routes reach the SDK — a PROFILE.
     *
     * ```kotlin
     * funktorCodegen {
     *     profile { route -> route.codeGen.tags.contains("public") }
     * }
     * ```
     *
     * **A profile selects ROOTS; there is no tree-shaking stage anywhere.** Narrowing the route set
     * narrows the walk, and every consequence follows for free: a type nothing reaches is never
     * declared, a claim nothing reaches leaves `usedClaims` so nothing imports it, and a runtime
     * module nobody asked for is not emitted. That is why this is a predicate over routes rather
     * than a filter over output.
     *
     * **Profiles are not access control.** A route excluded here is still served, still reachable,
     * and still governed by its auth floor. This decides what a given frontend is given a client
     * for, not what the server will answer.
     *
     * Replaces the default registration, which admits every route.
     */
    fun profile(include: (ApiRoute<*>) -> Boolean) {
        with(kontainer) {
            singleton(RestApiTsContributor::class) { features: Lazy<List<ApiFeature>> ->
                RestApiTsContributor(features = features, include = include)
            }
        }
    }

    /**
     * A profile admitting routes carrying ANY of [tags], via `codeGen { tag("...") }`.
     *
     * ANY rather than ALL: a tag marks a route as belonging to an audience, and a route serving two
     * audiences carries both. Requiring all of them would make the second tag *narrow* the route's
     * reach, which is the opposite of what adding one reads like.
     *
     * An untagged route is excluded — that is the point of asking for a profile.
     */
    fun profileTagged(vararg tags: String) {
        require(tags.isNotEmpty()) {
            "profileTagged() needs at least one tag. With none it would exclude every route and " +
                    "generate an empty SDK, which the builder rejects anyway — so this is a mistake " +
                    "worth catching where it is made."
        }

        val wanted = tags.toSet()

        profile { route -> route.codeGen.tags.any { it in wanted } }
    }

    @PublishedApi
    internal fun builder(): KontainerBuilder = kontainer
}

val Funktor_Codegen = module { builder: FunktorCodegenBuilder.() -> Unit ->

    // DYNAMIC deliberately. The builder reaches SlumberConfig, which `Funktor_Rest` registers next to
    // a `dynamic` RestCodec; per this repo's scoping rule a singleton that transitively injects a
    // dynamic silently becomes SemiDynamic anyway, so declaring it dynamic makes that explicit rather
    // than surprising. It costs nothing for a one-shot CLI run.
    dynamic(TsSdkBuilder::class) { contributors: List<TsSdkContributor>, config: SlumberConfig ->
        TsSdkBuilder(contributors = contributors, slumberConfig = config)
    }

    // The REST contributor takes a root predicate that a kontainer cannot supply, so it is built
    // here with the default "all routes". A profile replaces this registration with its own.
    singleton(RestApiTsContributor::class) { features: Lazy<List<ApiFeature>> ->
        RestApiTsContributor(features = features)
    }

    // Built-in contributors. Each is opt-out by not registering it — see the claims registry, which
    // rejects a second claim for a type rather than letting one silently shadow another.
    singleton(AuthTsContributor::class)

    // `ui/sdkContext.ts`, unconditionally. The app's `provideSdkConfig(app, config)` is hand-written,
    // so the module it imports must not come and go with whichever feature happens to be installed.
    singleton(SdkContextTsContributor::class)

    // Ships the insights pages, but ONLY into an SDK that actually has the insights feature — it
    // checks the feature list itself, so registering it here is safe for an app without insights.
    singleton(InsightsTsContributor::class) { features: Lazy<List<ApiFeature>> ->
        InsightsTsContributor(features = features)
    }
    singleton(FunktorUrlParamsTsContributor::class)
    singleton(MpDateTimeTsContributor::class)
    singleton(KotlinxJsonTsContributor::class)

    // CLI. `CliRunner` collects every CliktCommand in the kontainer, so registering it is enough.
    singleton(TsSdkGenerateCliCommand::class)

    FunktorCodegenBuilder(this).apply(builder)
}
