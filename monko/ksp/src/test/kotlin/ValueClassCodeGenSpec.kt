@file:Suppress("detekt:all")

package io.peekandpoke.monko.ksp

import io.kotest.common.ExperimentalKotest
import io.kotest.core.spec.style.StringSpec
import io.peekandpoke.ultra.meta.testing.expectFileToMatch
import io.peekandpoke.ultra.meta.testing.kspCompileTest
import io.peekandpoke.ultra.vault.Vault
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi

/**
 * Regression guard: a `@JvmInline value class` id property must render with the VALUE-CLASS type
 * (`append<RealmId, RealmId>`), NOT reduced to its underlying scalar (the way `Ref` is reduced to
 * `kotlin.String`). Keeping the value-class type is what makes `entity.realm eq RealmId(...)`
 * type-check and reject a bare `String`. The value class itself gets NO generated file (it is neither
 * data/abstract/sealed, so the processor's blacklist skips it).
 */
@OptIn(ExperimentalCompilerApi::class, ExperimentalKotest::class)
class ValueClassCodeGenSpec : StringSpec() {
    init {
        "Value class id properties render with the value-class type (not the underlying scalar)" {
            kspCompileTest {
                inheritClassPath(true)

                processor(MonkoKspProcessorProvider())

                kotlin(
                    file = "VcRecord.kt",
                    contents = """
                        package monko.compile

                        import ${Vault::class.qualifiedName}

                        @JvmInline
                        value class RealmId(val value: String)

                        @Vault
                        data class VcRecord(
                            val realm: RealmId,
                        )

                    """.trimIndent()
                )

                expectFileToMatch(
                    file = "VcRecord${"\$\$"}monko.kt",
                    contents = """
                        package monko.compile

                        import io.peekandpoke.monko.*
                        import io.peekandpoke.monko.lang.*

                        //// generic property
                        inline fun <reified T> MongoIterableExpr<VcRecord>.property(name: String) = MongoPropertyPath.start(this).append<T, T>(name)

                        // realm ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                        // annotations:
                        // defined as:   Primary Constructor Param
                        // defined by:   Class monko.compile.VcRecord
                        // defined type: monko.compile.RealmId
                        // cleaned type: monko.compile.RealmId
                        // defined at:   Line 10

                        inline val MongoIterableExpr<VcRecord>.realm inline get() = MongoPropertyPath.start(this).append<monko.compile.RealmId, monko.compile.RealmId>("realm")
                        inline val MongoExpression<VcRecord>.realm inline get() = MongoPropertyPath.start(this).append<monko.compile.RealmId, monko.compile.RealmId>("realm")

                        inline val MongoPropertyPath<VcRecord, VcRecord>.realm @JvmName("realm_0") inline get() = append<monko.compile.RealmId, monko.compile.RealmId>("realm")
                    """.trimIndent()
                )
            }
        }
    }
}
