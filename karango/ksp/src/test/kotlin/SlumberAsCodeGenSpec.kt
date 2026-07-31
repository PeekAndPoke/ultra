@file:Suppress("detekt:all")

package io.peekandpoke.karango.ksp

import io.kotest.assertions.withClue
import io.kotest.common.ExperimentalKotest
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.peekandpoke.ultra.meta.testing.getGeneratedSources
import io.peekandpoke.ultra.meta.testing.kspCompileTest
import io.peekandpoke.ultra.vault.Vault
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi

/**
 * `@Slumber.As` REPLACES a type's own Kotlin properties as the source of query paths.
 *
 * Before this, `MpInstant` was walked like any data class and produced
 * `AqlExpression<MpInstant>.value` — an AQL path `value` that does not exist in the stored document
 * (`{ts, timezone, human}`). It compiled, matched nothing, and warned about nothing. The hand-written
 * `.ts` helper in `base_slumber.kt` was the manual patch beside it.
 *
 * Both types here are CLASSPATH symbols — only `Probe` is compiled — which is the case that matters:
 * every real consumer of karango sees `MpInstant` that way.
 */
@OptIn(ExperimentalCompilerApi::class, ExperimentalKotest::class)
class SlumberAsCodeGenSpec : StringSpec() {
    init {
        "@Slumber.As replaces a type's own properties as the source of paths" {

            val result = kspCompileTest {
                inheritClassPath(true)

                processor(KarangoKspProcessorProvider())

                kotlin(
                    file = "Probe.kt",
                    contents = """
                        package karango.compile

                        import ${Vault::class.qualifiedName}
                        import io.peekandpoke.ultra.datetime.MpInstant
                        import io.peekandpoke.ultra.datetime.MpLocalTime
                        import io.peekandpoke.ultra.datetime.MpTimezone

                        @Vault
                        data class Probe(
                            val createdAt: MpInstant,
                            val time: MpLocalTime,
                            val zone: MpTimezone,
                        )

                    """.trimIndent()
                )
            }

            val generated = result.getGeneratedSources().associate { it.name to it.readText() }

            withClue("MpInstant declares MpDateTimeRawData, so its paths are ts/timezone/human") {
                val code = generated.getValue("MpInstant${"$$"}karango.kt")

                code shouldContain "//// wire shape declared by @Slumber.As: io.peekandpoke.ultra.datetime.MpDateTimeRawData"

                code shouldContain """inline val AqlExpression<MpInstant>.ts inline get() = AqlPropertyPath.start(this).append<kotlin.Long, kotlin.Long>("ts")"""
                code shouldContain """inline val AqlExpression<MpInstant>.timezone inline get() = AqlPropertyPath.start(this).append<kotlin.String, kotlin.String>("timezone")"""
                code shouldContain """inline val AqlExpression<MpInstant>.human inline get()"""

                // The typealias AqlPathExpr<P> = AqlPropertyPath<P, P>, so this is the receiver the
                // hand-written helper in base_slumber.kt uses. It is what makes removing it source-compatible.
                code shouldContain """inline val AqlPropertyPath<MpInstant, MpInstant>.ts @JvmName("ts_0")"""

                // The defect this fixes: MpInstant has no `value` key on the wire.
                code shouldNotContain "AqlExpression<MpInstant>.value"
            }

            withClue("MpLocalTime declares a bare Long, so it has no sub-paths at all") {
                val code = generated.getValue("MpLocalTime${"$$"}karango.kt")

                code shouldContain "//// wire shape declared by @Slumber.As: kotlin.Long"
                code shouldNotContain "AqlExpression<MpLocalTime>.value"
                code shouldNotContain "inline val AqlPropertyPath<MpLocalTime"
            }

            withClue("MpTimezone declares a bare String — and String HAS properties, unlike Long") {
                // This is the case that actually exercises the scalar guard. Walking `kotlin.Long`
                // yields nothing either way, so `MpLocalTime` above cannot tell the guard from its
                // absence; `kotlin.String` has `length`, so this one can. Found by mutation testing.
                val code = generated.getValue("MpTimezone${"$$"}karango.kt")

                code shouldContain "//// wire shape declared by @Slumber.As: kotlin.String"
                code shouldNotContain "AqlExpression<MpTimezone>.id"
                code shouldNotContain "length"
            }
        }
    }
}
