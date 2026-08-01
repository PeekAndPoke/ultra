@file:Suppress("detekt:all")

package io.peekandpoke.monko.ksp

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
 * `MongoExpression<MpInstant>.value` — a field path `value` that does not exist in the stored document
 * (`{ts, timezone, human}`). It compiled, matched nothing, and warned about nothing. The hand-written
 * `.ts` helper in `lang/base_slumber.kt` was the manual patch beside it.
 *
 * Both types here are CLASSPATH symbols — only `Probe` is compiled — which is the case that matters:
 * every real consumer of monko sees `MpInstant` that way.
 */
@OptIn(ExperimentalCompilerApi::class, ExperimentalKotest::class)
class SlumberAsCodeGenSpec : StringSpec() {
    init {
        "@Slumber.As replaces a type's own properties as the source of paths" {

            val result = kspCompileTest {
                inheritClassPath(true)

                processor(MonkoKspProcessorProvider())

                kotlin(
                    file = "Probe.kt",
                    contents = """
                        package monko.compile

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
                val code = generated.getValue("MpInstant${"$$"}monko.kt")

                code shouldContain "//// wire shape declared by @Slumber.As: io.peekandpoke.ultra.datetime.MpDateTimeRawData"

                code shouldContain """inline val MongoExpression<MpInstant>.ts inline get() = MongoPropertyPath.start(this).append<kotlin.Long, kotlin.Long>("ts")"""
                code shouldContain """inline val MongoExpression<MpInstant>.timezone inline get() = MongoPropertyPath.start(this).append<kotlin.String, kotlin.String>("timezone")"""
                code shouldContain """inline val MongoExpression<MpInstant>.human inline get()"""

                // The typealias MongoPathExpr<P> = MongoPropertyPath<P, P>, so this is the receiver the
                // hand-written helper in lang/base_slumber.kt uses. It is what makes removing it
                // source-compatible.
                code shouldContain """inline val MongoPropertyPath<MpInstant, MpInstant>.ts @JvmName("ts_0")"""

                // The defect this fixes: MpInstant has no `value` key on the wire.
                code shouldNotContain "MongoExpression<MpInstant>.value"
            }

            withClue("MpLocalTime declares a bare Long, so it has no sub-paths at all") {
                val code = generated.getValue("MpLocalTime${"$$"}monko.kt")

                code shouldContain "//// wire shape declared by @Slumber.As: kotlin.Long"
                code shouldNotContain "MongoExpression<MpLocalTime>.value"
                code shouldNotContain "inline val MongoPropertyPath<MpLocalTime"
            }

            withClue("MpTimezone declares a bare String, so it has no sub-paths either") {
                val code = generated.getValue("MpTimezone${"$$"}monko.kt")

                code shouldContain "//// wire shape declared by @Slumber.As: kotlin.String"
                code shouldNotContain "MongoExpression<MpTimezone>.id"
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

                processor(MonkoKspProcessorProvider())

                kotlin(
                    file = "BadShape.kt",
                    contents = """
                        package monko.compile

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
                generated["Price${"$$"}monko.kt"]?.let { it shouldNotContain "code" }
            }
        }
    }
}
