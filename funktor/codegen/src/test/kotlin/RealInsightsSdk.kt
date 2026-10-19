package io.peekandpoke.funktor.codegen

import io.peekandpoke.funktor.core.broker.funktorBroker
import io.peekandpoke.funktor.core.config.AppConfig
import io.peekandpoke.funktor.core.funktorCore
import io.peekandpoke.funktor.core.model.AppInfo
import io.peekandpoke.funktor.core.model.default
import io.peekandpoke.funktor.insights.api.InsightsApiFeature
import io.peekandpoke.funktor.rest.funktorRest
import io.peekandpoke.ultra.codegen.sdk.TsSdkBuilder
import io.peekandpoke.ultra.codegen.sdk.TsSdkOutput
import io.peekandpoke.ultra.kontainer.kontainer
import java.io.File

/**
 * The SDK an app with the REAL `funktor:insights` feature gets, wired as `sdk:ts:generate` wires it.
 *
 * The fixtures elsewhere in this module stand in for insights by NAME only, so their `models.ts` has none
 * of the types the pages import. This is what the pages are compiled and tested against.
 */
object RealInsightsSdk {

    fun build(): TsSdkOutput = kontainer {
        val config = AppConfig.empty

        funktorCore(config, AppInfo.default())
        funktorBroker()
        funktorRest(config)
        funktorCodegen()

        singleton(InsightsApiFeature::class)
    }.create().get(TsSdkBuilder::class).build().output
}

/** Writes [RealInsightsSdk] into `args[0]` for the `vue-verify` harness; Gradle's `generateVueVerifySdk`. */
fun main(args: Array<String>) {
    val target = File(args.single())

    target.deleteRecursively()
    RealInsightsSdk.build().writeTo(target)

    println("vue-verify: wrote the insights SDK to $target")
}
