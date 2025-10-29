@file:Suppress("detekt:all")

package de.kotlincook.karango.ksp

import de.peekandpoke.karango.ksp.KarangoKspProcessorProvider
import de.peekandpoke.ultra.meta.testing.expectFileToMatch
import de.peekandpoke.ultra.meta.testing.kspCompileTest
import de.peekandpoke.ultra.vault.Vault
import io.kotest.common.ExperimentalKotest
import io.kotest.core.spec.style.StringSpec
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi

@OptIn(ExperimentalCompilerApi::class, ExperimentalKotest::class)
class NestedClassCodeGenSpec : StringSpec() {
    init {
        "Generating code for a nested class" {

            kspCompileTest {
                inheritClassPath(true)

                processor(KarangoKspProcessorProvider())

                kotlin(
                    file = "nested.kt",
                    contents = """
                        package karango.compile
                        
                        import ${Vault::class.qualifiedName}
    
                        @Vault
                        data class Outer(val inner: Inner) {
                            data class Inner(val num: Int)
                        }
    
                    """.trimIndent()
                )

                expectFileToMatch(
                    file = "Outer${"$$"}karango.kt",
                    contents = """
                        package karango.compile
                        
                        import de.peekandpoke.karango.*
                        import de.peekandpoke.karango.aql.*
                        import de.peekandpoke.ultra.vault.lang.*
                        
                        //// generic property
                        inline fun <reified T> AqlIterableExpr<Outer>.property(name: String) = AqlPropertyPath.start(this).append<T, T>(name)
                        
                        // inner ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                        // annotations: 
                        // defined as:   Primary Constructor Param
                        // defined by:   Class karango.compile.Outer
                        // defined at:   Line 6
                        // defined type: karango.compile.Outer.Inner
                        // cleaned type: karango.compile.Outer.Inner
                        
                        inline val AqlIterableExpr<Outer>.inner inline get() = AqlPropertyPath.start(this).append<karango.compile.Outer.Inner, karango.compile.Outer.Inner>("inner")
                        inline val AqlExpression<Outer>.inner inline get() = AqlPropertyPath.start(this).append<karango.compile.Outer.Inner, karango.compile.Outer.Inner>("inner")
                        
                        inline val AqlPropertyPath<Outer, Outer>.inner @JvmName("inner_0") inline get() = append<karango.compile.Outer.Inner, karango.compile.Outer.Inner>("inner")
                        inline val AqlPropertyPath<Outer, L1<Outer>>.inner @JvmName("inner_1") inline get() = append<karango.compile.Outer.Inner, L1<karango.compile.Outer.Inner>>("inner")
                        inline val AqlPropertyPath<Outer, L2<Outer>>.inner @JvmName("inner_2") inline get() = append<karango.compile.Outer.Inner, L2<karango.compile.Outer.Inner>>("inner")
                        inline val AqlPropertyPath<Outer, L3<Outer>>.inner @JvmName("inner_3") inline get() = append<karango.compile.Outer.Inner, L3<karango.compile.Outer.Inner>>("inner")
                        inline val AqlPropertyPath<Outer, L4<Outer>>.inner @JvmName("inner_4") inline get() = append<karango.compile.Outer.Inner, L4<karango.compile.Outer.Inner>>("inner")
                        inline val AqlPropertyPath<Outer, L5<Outer>>.inner @JvmName("inner_5") inline get() = append<karango.compile.Outer.Inner, L5<karango.compile.Outer.Inner>>("inner")
                    """.trimIndent()
                )

                expectFileToMatch(
                    file = "Outer.Inner${"$$"}karango.kt",
                    contents = """
                        package karango.compile

                        import de.peekandpoke.karango.*
                        import de.peekandpoke.karango.aql.*
                        import de.peekandpoke.ultra.vault.lang.*

                        //// generic property
                        inline fun <reified T> AqlIterableExpr<Outer.Inner>.property(name: String) = AqlPropertyPath.start(this).append<T, T>(name)

                        // num /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                        // annotations: 
                        // defined as:   Primary Constructor Param
                        // defined by:   Class karango.compile.Outer.Inner
                        // defined at:   Line 7
                        // defined type: kotlin.Int
                        // cleaned type: kotlin.Int

                        inline val AqlIterableExpr<Outer.Inner>.num inline get() = AqlPropertyPath.start(this).append<kotlin.Int, kotlin.Int>("num")
                        inline val AqlExpression<Outer.Inner>.num inline get() = AqlPropertyPath.start(this).append<kotlin.Int, kotlin.Int>("num")

                        inline val AqlPropertyPath<Outer.Inner, Outer.Inner>.num @JvmName("num_0") inline get() = append<kotlin.Int, kotlin.Int>("num")
                        inline val AqlPropertyPath<Outer.Inner, L1<Outer.Inner>>.num @JvmName("num_1") inline get() = append<kotlin.Int, L1<kotlin.Int>>("num")
                        inline val AqlPropertyPath<Outer.Inner, L2<Outer.Inner>>.num @JvmName("num_2") inline get() = append<kotlin.Int, L2<kotlin.Int>>("num")
                        inline val AqlPropertyPath<Outer.Inner, L3<Outer.Inner>>.num @JvmName("num_3") inline get() = append<kotlin.Int, L3<kotlin.Int>>("num")
                        inline val AqlPropertyPath<Outer.Inner, L4<Outer.Inner>>.num @JvmName("num_4") inline get() = append<kotlin.Int, L4<kotlin.Int>>("num")
                        inline val AqlPropertyPath<Outer.Inner, L5<Outer.Inner>>.num @JvmName("num_5") inline get() = append<kotlin.Int, L5<kotlin.Int>>("num")
                    """.trimIndent()
                )
            }
        }
    }
}
