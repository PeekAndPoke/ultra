@file:Suppress("detekt:all")

package io.peekandpoke.monko.ksp

import io.kotest.common.ExperimentalKotest
import io.kotest.core.spec.style.StringSpec
import io.peekandpoke.ultra.meta.testing.expectFileToMatch
import io.peekandpoke.ultra.meta.testing.kspCompileTest
import io.peekandpoke.ultra.vault.Vault
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi

@OptIn(ExperimentalCompilerApi::class, ExperimentalKotest::class)
class FieldSelectionCodeGenSpec : StringSpec() {
    init {
        "Ctor parameters are picked up, @Vault.Field non-ctor fields are picked up, @Vault.Ignore fields are excluded, unannotated non-ctor fields are excluded" {
            kspCompileTest {
                inheritClassPath(true)

                processor(MonkoKspProcessorProvider())

                kotlin(
                    file = "FieldSelection.kt",
                    contents = """
                        package monko.compile

                        import ${Vault::class.qualifiedName}
                        import io.peekandpoke.ultra.slumber.Slumber

                        @Vault
                        abstract class FieldSelection {
                            abstract val ctorLikeField: String

                            @Vault.Field
                            abstract val vaultField: Int

                            @Slumber.Field
                            abstract val slumberField: Double

                            @Vault.Ignore
                            abstract val ignoredField: String

                            abstract val unannotatedField: String
                        }

                    """.trimIndent()
                )

                expectFileToMatch(
                    file = "FieldSelection${"\$\$"}monko.kt",
                    contents = """
                        package monko.compile

                        import io.peekandpoke.monko.*
                        import io.peekandpoke.monko.lang.*

                        //// generic property
                        inline fun <reified T> MongoIterableExpr<FieldSelection>.property(name: String) = MongoPropertyPath.start(this).append<T, T>(name)

                        // vaultField //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                        // annotations: io.peekandpoke.ultra.vault.Vault.Field
                        // defined as:   Property
                        // defined by:   Class monko.compile.FieldSelection
                        // defined type: kotlin.Int
                        // cleaned type: kotlin.Int
                        // defined at:   Line 11

                        inline val MongoIterableExpr<FieldSelection>.vaultField inline get() = MongoPropertyPath.start(this).append<kotlin.Int, kotlin.Int>("vaultField")
                        inline val MongoExpression<FieldSelection>.vaultField inline get() = MongoPropertyPath.start(this).append<kotlin.Int, kotlin.Int>("vaultField")

                        inline val MongoPropertyPath<FieldSelection, FieldSelection>.vaultField @JvmName("vaultField_0") inline get() = append<kotlin.Int, kotlin.Int>("vaultField")

                        // slumberField ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                        // annotations: io.peekandpoke.ultra.slumber.Slumber.Field
                        // defined as:   Property
                        // defined by:   Class monko.compile.FieldSelection
                        // defined type: kotlin.Double
                        // cleaned type: kotlin.Double
                        // defined at:   Line 14

                        inline val MongoIterableExpr<FieldSelection>.slumberField inline get() = MongoPropertyPath.start(this).append<kotlin.Double, kotlin.Double>("slumberField")
                        inline val MongoExpression<FieldSelection>.slumberField inline get() = MongoPropertyPath.start(this).append<kotlin.Double, kotlin.Double>("slumberField")

                        inline val MongoPropertyPath<FieldSelection, FieldSelection>.slumberField @JvmName("slumberField_0") inline get() = append<kotlin.Double, kotlin.Double>("slumberField")
                    """.trimIndent()
                )
            }
        }
    }
}
