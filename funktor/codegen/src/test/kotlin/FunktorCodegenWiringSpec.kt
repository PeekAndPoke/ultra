package io.peekandpoke.funktor.codegen

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.peekandpoke.funktor.codegen.cli.TsSdkGenerateCliCommand
import io.peekandpoke.funktor.core.model.AppInfo
import io.peekandpoke.funktor.core.model.default
import io.peekandpoke.funktor.core.config.AppConfig
import io.peekandpoke.funktor.core.funktorCore
import io.peekandpoke.funktor.core.broker.funktorBroker
import io.peekandpoke.funktor.rest.ApiFeature
import io.peekandpoke.funktor.rest.funktorRest
import io.peekandpoke.ultra.codegen.sdk.TsSdkBuilder
import io.peekandpoke.ultra.codegen.sdk.TsSdkContributor
import io.peekandpoke.ultra.kontainer.kontainer
import io.peekandpoke.ultra.slumber.SlumberConfig

/**
 * Proves the kontainer registrations RESOLVE, not merely that they compile.
 *
 * A module definition type-checks whether or not its dependencies can be satisfied: a missing
 * `SlumberConfig`, a constructor parameter no provider matches, or a service registered at the wrong
 * scope all surface only when something asks the container for the service. For a CLI command that is
 * at the moment an operator runs it, which is the worst place to find out.
 */
class FunktorCodegenWiringSpec : FreeSpec() {

    private fun container() = kontainer {
        val config = AppConfig.empty

        funktorCore(config, AppInfo.default())
        funktorBroker()
        funktorRest(config)
        funktorCodegen()

        // One feature, so the REST contributor has something to walk.
        singleton(FxWiringApiFeature::class)
    }.create()

    init {
        "the generate command resolves from the kontainer" {
            // CliRunner finds commands by asking for every CliktCommand, so this is also what makes
            // `--cli sdk:ts:generate` reachable at all.
            container().get(TsSdkGenerateCliCommand::class) shouldNotBe null
        }

        "SlumberConfig is injectable, which is the whole point of the Funktor_Rest change" {
            // Without `instance(codecConfig)` the builder cannot be constructed, and the only
            // alternative was downcasting the injected RestCodec at every call site.
            container().get(SlumberConfig::class) shouldNotBe null
        }

        "the builder resolves with every registered contributor injected" {
            val contributors = container().getAll(TsSdkContributor::class).map { it.name }

            withClue("a contributor that is registered but not injected is invisible, not loud") {
                contributors shouldContain RestApiTsContributor.NAME
                contributors shouldContain FunktorUrlParamsTsContributor.NAME
                contributors shouldContain "ultra:codegen:datetime"
            }

            container().get(TsSdkBuilder::class) shouldNotBe null
        }

        "generation runs end to end through the container" {
            // The strongest check here: not that the graph resolves, but that driving it produces the
            // SDK. A wiring bug that only shows up mid-generation would pass every check above.
            val result = container().get(TsSdkBuilder::class).build()

            val paths = result.output.entries().map { it.path }

            paths shouldContain "models.ts"
            paths shouldContain "fxWiringClient.ts"
            paths shouldContain "runtime/client.ts"
        }

        "the generator is NOT part of the module a production server gets" {
            // funktorCodegen() is opt-in; a server that never calls it must not carry the generator.
            val without = kontainer {
                val config = AppConfig.empty

                funktorCore(config, AppInfo.default())
                funktorBroker()
                funktorRest(config)
            }.create()

            without.getOrNull(TsSdkGenerateCliCommand::class) shouldBe null
        }
    }
}
