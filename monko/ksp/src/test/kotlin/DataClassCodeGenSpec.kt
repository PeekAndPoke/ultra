@file:Suppress("detekt:all")

package io.peekandpoke.monko.ksp

import io.kotest.common.ExperimentalKotest
import io.kotest.core.spec.style.StringSpec
import io.peekandpoke.ultra.meta.testing.expectFileToMatch
import io.peekandpoke.ultra.meta.testing.kspCompileTest
import io.peekandpoke.ultra.vault.Vault
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi

@OptIn(ExperimentalCompilerApi::class, ExperimentalKotest::class)
class DataClassCodeGenSpec : StringSpec() {
    init {
        "Generating code for a simple data class with constructor params" {
            kspCompileTest {
                inheritClassPath(true)

                processor(MonkoKspProcessorProvider())

                kotlin(
                    file = "SimpleDataClass.kt",
                    contents = """
                        package monko.compile

                        import ${Vault::class.qualifiedName}

                        @Vault
                        data class SimpleDataClass(
                            val name: String,
                            val age: Int,
                            val score: Double,
                        )

                    """.trimIndent()
                )

                expectFileToMatch(
                    file = "SimpleDataClass${"\$\$"}monko.kt",
                    contents = """
                        package monko.compile

                        import io.peekandpoke.monko.*
                        import io.peekandpoke.monko.lang.*

                        //// generic property
                        inline fun <reified T> MongoIterableExpr<SimpleDataClass>.property(name: String) = MongoPropertyPath.start(this).append<T, T>(name)

                        // name ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                        // annotations:
                        // defined as:   Primary Constructor Param
                        // defined by:   Class monko.compile.SimpleDataClass
                        // defined type: kotlin.String
                        // cleaned type: kotlin.String
                        // defined at:   Line 7

                        inline val MongoIterableExpr<SimpleDataClass>.name inline get() = MongoPropertyPath.start(this).append<kotlin.String, kotlin.String>("name")
                        inline val MongoExpression<SimpleDataClass>.name inline get() = MongoPropertyPath.start(this).append<kotlin.String, kotlin.String>("name")

                        inline val MongoPropertyPath<SimpleDataClass, SimpleDataClass>.name @JvmName("name_0") inline get() = append<kotlin.String, kotlin.String>("name")

                        // age /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                        // annotations:
                        // defined as:   Primary Constructor Param
                        // defined by:   Class monko.compile.SimpleDataClass
                        // defined type: kotlin.Int
                        // cleaned type: kotlin.Int
                        // defined at:   Line 8

                        inline val MongoIterableExpr<SimpleDataClass>.age inline get() = MongoPropertyPath.start(this).append<kotlin.Int, kotlin.Int>("age")
                        inline val MongoExpression<SimpleDataClass>.age inline get() = MongoPropertyPath.start(this).append<kotlin.Int, kotlin.Int>("age")

                        inline val MongoPropertyPath<SimpleDataClass, SimpleDataClass>.age @JvmName("age_0") inline get() = append<kotlin.Int, kotlin.Int>("age")

                        // score ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                        // annotations:
                        // defined as:   Primary Constructor Param
                        // defined by:   Class monko.compile.SimpleDataClass
                        // defined type: kotlin.Double
                        // cleaned type: kotlin.Double
                        // defined at:   Line 9

                        inline val MongoIterableExpr<SimpleDataClass>.score inline get() = MongoPropertyPath.start(this).append<kotlin.Double, kotlin.Double>("score")
                        inline val MongoExpression<SimpleDataClass>.score inline get() = MongoPropertyPath.start(this).append<kotlin.Double, kotlin.Double>("score")

                        inline val MongoPropertyPath<SimpleDataClass, SimpleDataClass>.score @JvmName("score_0") inline get() = append<kotlin.Double, kotlin.Double>("score")
                    """.trimIndent()
                )
            }
        }
    }
}
