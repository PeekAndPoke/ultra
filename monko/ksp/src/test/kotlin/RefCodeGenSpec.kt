@file:Suppress("detekt:all")

package io.peekandpoke.monko.ksp

import io.kotest.common.ExperimentalKotest
import io.kotest.core.spec.style.StringSpec
import io.peekandpoke.ultra.meta.testing.expectFileToMatch
import io.peekandpoke.ultra.meta.testing.kspCompileTest
import io.peekandpoke.ultra.vault.Vault
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi

@OptIn(ExperimentalCompilerApi::class, ExperimentalKotest::class)
class RefCodeGenSpec : StringSpec() {
    init {
        "Ref properties are treated as kotlin.String" {
            kspCompileTest {
                inheritClassPath(true)

                processor(MonkoKspProcessorProvider())

                kotlin(
                    file = "RefDataClass.kt",
                    contents = """
                        package monko.compile

                        import ${Vault::class.qualifiedName}
                        import io.peekandpoke.ultra.vault.Ref

                        data class OtherClass(val value: String)

                        @Vault
                        data class RefDataClass(
                            val ref: Ref<OtherClass>,
                        )

                    """.trimIndent()
                )

                expectFileToMatch(
                    file = "RefDataClass${"\$\$"}monko.kt",
                    contents = """
                        package monko.compile

                        import io.peekandpoke.monko.*
                        import io.peekandpoke.monko.lang.*

                        //// generic property
                        inline fun <reified T> MongoIterableExpr<RefDataClass>.property(name: String) = MongoPropertyPath.start(this).append<T, T>(name)

                        // ref /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                        // annotations:
                        // defined as:   Primary Constructor Param
                        // defined by:   Class monko.compile.RefDataClass
                        // defined type: kotlin.String
                        // cleaned type: kotlin.String
                        // defined at:   Line 10

                        inline val MongoIterableExpr<RefDataClass>.ref inline get() = MongoPropertyPath.start(this).append<kotlin.String, kotlin.String>("ref")
                        inline val MongoExpression<RefDataClass>.ref inline get() = MongoPropertyPath.start(this).append<kotlin.String, kotlin.String>("ref")

                        inline val MongoPropertyPath<RefDataClass, RefDataClass>.ref @JvmName("ref_0") inline get() = append<kotlin.String, kotlin.String>("ref")
                    """.trimIndent()
                )
            }
        }
    }
}
