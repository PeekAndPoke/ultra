package io.peekandpoke.funktor.codegen

import io.peekandpoke.funktor.codegen.cli.TsSdkGenerateCliCommand
import io.peekandpoke.funktor.rest.ApiFeature
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
    singleton(FunktorUrlParamsTsContributor::class)
    singleton(MpDateTimeTsContributor::class)
    singleton(KotlinxJsonTsContributor::class)

    // CLI. `CliRunner` collects every CliktCommand in the kontainer, so registering it is enough.
    singleton(TsSdkGenerateCliCommand::class)

    FunktorCodegenBuilder(this).apply(builder)
}
