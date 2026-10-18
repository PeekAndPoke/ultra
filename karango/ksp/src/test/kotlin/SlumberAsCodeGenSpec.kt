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
                        import io.peekandpoke.ultra.common.slumber.Slumber
                        import io.peekandpoke.ultra.datetime.MpInstant
                        import io.peekandpoke.ultra.datetime.MpLocalTime
                        import io.peekandpoke.ultra.datetime.MpTimezone

                        data class LocalShape(val alpha: kotlin.Long, val beta: kotlin.String)

                        @Slumber.As(LocalShape::class)
                        data class LocalCustom(val ignoredKotlinProp: kotlin.Int)

                        @Vault
                        data class Probe(
                            val createdAt: MpInstant,
                            val time: MpLocalTime,
                            val zone: MpTimezone,
                            val local: LocalCustom,
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

            withClue("a type annotated IN THE COMPILED SOURCE, with a positional argument") {
                // The classpath cases above read the annotation from a descriptor, where argument names
                // are always present. A user annotating their own type does not go through that path,
                // and `getSlumberAsShape` looks the argument up BY NAME — so if KSP does not back-fill
                // the name for a positional argument in source, it silently falls back to the type's own
                // Kotlin properties, which is the exact defect this feature exists to remove.
                val code = generated.getValue("LocalCustom${"$$"}karango.kt")

                code shouldContain "//// wire shape declared by @Slumber.As: karango.compile.LocalShape"
                code shouldContain """inline val AqlExpression<LocalCustom>.alpha inline get() = AqlPropertyPath.start(this).append<kotlin.Long, kotlin.Long>("alpha")"""
                code shouldContain """inline val AqlExpression<LocalCustom>.beta inline get()"""
                code shouldNotContain "ignoredKotlinProp"
            }

            withClue("MpTimezone declares a bare String, so it has no sub-paths either") {
                // NOTE: this does not independently exercise the scalar guard, despite covering a
                // different scalar. KSP reports no declared properties for `kotlin.*` builtins at all --
                // not even `String.length` -- so removing the guard changes nothing here. Both mutants
                // survived; see the matching comment in KarangoKspProcessor. What this pins is the
                // OUTCOME, which is the real tripwire.
                val code = generated.getValue("MpTimezone${"$$"}karango.kt")

                code shouldContain "//// wire shape declared by @Slumber.As: kotlin.String"
                code shouldNotContain "AqlExpression<MpTimezone>.id"
                code shouldNotContain "length"
            }
        }
    
        "a shape that cannot describe a wire shape fails the build instead of generating sub-paths" {

            // Before the review this fell through to the shape's own properties: `@Slumber.As(Money::class)`
            // where Money is an enum yielded Money's ctor property as a query path -- a sub-path into what
            // is a bare string on the wire, i.e. the very defect this feature removes, reintroduced
            // through it. Only the nine primitives were rejected.
            val result = kspCompileTest {
                inheritClassPath(true)

                processor(KarangoKspProcessorProvider())

                kotlin(
                    file = "BadShape.kt",
                    contents = """
                        package karango.compile

                        import ${Vault::class.qualifiedName}
                        import io.peekandpoke.ultra.common.slumber.Slumber

                        enum class Currency(val code: kotlin.String) { EUR("EUR") }

                        @Slumber.As(Currency::class)
                        data class Price(val cents: kotlin.Long)

                        @Vault
                        data class Basket(val price: Price)

                    """.trimIndent()
                )
            }

            result.messages shouldContain "which cannot describe a wire shape"

            val generated = result.getGeneratedSources().associate { it.name to it.readText() }

            withClue("no accessor may be generated for the enum's own property") {
                generated["Price${"$$"}karango.kt"]?.let { it shouldNotContain "code" }
            }
        }
    }
}
