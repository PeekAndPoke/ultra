package de.kotlincook.karango.ksp

import de.peekandpoke.karango.ksp.KarangoKspProcessorProvider
import de.peekandpoke.ultra.meta.testing.expectFileToMatch
import de.peekandpoke.ultra.meta.testing.kspCompileTest
import de.peekandpoke.ultra.vault.Vault
import io.kotest.common.ExperimentalKotest
import io.kotest.core.spec.style.StringSpec
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi

@OptIn(ExperimentalCompilerApi::class, ExperimentalKotest::class)
class AbstractClassCodeGenSpec : StringSpec() {
    init {
        "Generating Code for an abstract class" {
            kspCompileTest {
                inheritClassPath(true)

                processor(KarangoKspProcessorProvider())

                kotlin(
                    file = "AbstractClass.kt",
                    contents = """
                        package karango.compile
                        
                        import ${Vault::class.qualifiedName}
                        
                        @Vault
                        abstract class AbstractClass {
                            @Vault.Field
                            abstract val x: Int
                            
                            @Vault.Field
                            abstract val data: SomeClass
                            
                            abstract val noCodeGeneratedForMe: String
                        }
    
                        data class SomeClass(val num: Int)
    
                    """.trimIndent()
                )

                expectFileToMatch(
                    file = "AbstractClass${"$$"}karango.kt",
                    contents = """
                        package karango.compile
                        
                        import de.peekandpoke.karango.*
                        import de.peekandpoke.karango.aql.*
                        import de.peekandpoke.ultra.vault.lang.*
                        
                        //// generic property
                        inline fun <reified T> AqlIterableExpr<AbstractClass>.property(name: String) = AqlPropertyPath.start(this).append<T, T>(name)
                        
                        // x ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                        // annotations: de.peekandpoke.ultra.vault.Vault.Field
                        // defined as:   Property
                        // defined by:   Class karango.compile.AbstractClass
                        // defined at:   Line 8
                        // defined type: kotlin.Int
                        // cleaned type: kotlin.Int
                        
                        inline val AqlIterableExpr<AbstractClass>.x inline get() = AqlPropertyPath.start(this).append<kotlin.Int, kotlin.Int>("x")
                        inline val AqlExpression<AbstractClass>.x inline get() = AqlPropertyPath.start(this).append<kotlin.Int, kotlin.Int>("x")
                        
                        inline val AqlPropertyPath<AbstractClass, AbstractClass>.x @JvmName("x_0") inline get() = append<kotlin.Int, kotlin.Int>("x")
                        inline val AqlPropertyPath<AbstractClass, L1<AbstractClass>>.x @JvmName("x_1") inline get() = append<kotlin.Int, L1<kotlin.Int>>("x")
                        inline val AqlPropertyPath<AbstractClass, L2<AbstractClass>>.x @JvmName("x_2") inline get() = append<kotlin.Int, L2<kotlin.Int>>("x")
                        inline val AqlPropertyPath<AbstractClass, L3<AbstractClass>>.x @JvmName("x_3") inline get() = append<kotlin.Int, L3<kotlin.Int>>("x")
                        inline val AqlPropertyPath<AbstractClass, L4<AbstractClass>>.x @JvmName("x_4") inline get() = append<kotlin.Int, L4<kotlin.Int>>("x")
                        inline val AqlPropertyPath<AbstractClass, L5<AbstractClass>>.x @JvmName("x_5") inline get() = append<kotlin.Int, L5<kotlin.Int>>("x")
                        
                        // data ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                        // annotations: de.peekandpoke.ultra.vault.Vault.Field
                        // defined as:   Property
                        // defined by:   Class karango.compile.AbstractClass
                        // defined at:   Line 11
                        // defined type: karango.compile.SomeClass
                        // cleaned type: karango.compile.SomeClass
                        
                        inline val AqlIterableExpr<AbstractClass>.data inline get() = AqlPropertyPath.start(this).append<karango.compile.SomeClass, karango.compile.SomeClass>("data")
                        inline val AqlExpression<AbstractClass>.data inline get() = AqlPropertyPath.start(this).append<karango.compile.SomeClass, karango.compile.SomeClass>("data")
                        
                        inline val AqlPropertyPath<AbstractClass, AbstractClass>.data @JvmName("data_0") inline get() = append<karango.compile.SomeClass, karango.compile.SomeClass>("data")
                        inline val AqlPropertyPath<AbstractClass, L1<AbstractClass>>.data @JvmName("data_1") inline get() = append<karango.compile.SomeClass, L1<karango.compile.SomeClass>>("data")
                        inline val AqlPropertyPath<AbstractClass, L2<AbstractClass>>.data @JvmName("data_2") inline get() = append<karango.compile.SomeClass, L2<karango.compile.SomeClass>>("data")
                        inline val AqlPropertyPath<AbstractClass, L3<AbstractClass>>.data @JvmName("data_3") inline get() = append<karango.compile.SomeClass, L3<karango.compile.SomeClass>>("data")
                        inline val AqlPropertyPath<AbstractClass, L4<AbstractClass>>.data @JvmName("data_4") inline get() = append<karango.compile.SomeClass, L4<karango.compile.SomeClass>>("data")
                        inline val AqlPropertyPath<AbstractClass, L5<AbstractClass>>.data @JvmName("data_5") inline get() = append<karango.compile.SomeClass, L5<karango.compile.SomeClass>>("data")                        
                    """.trimIndent()
                )

                expectFileToMatch(
                    file = "SomeClass${"$$"}karango.kt",
                    contents = """
                        package karango.compile
                        
                        import de.peekandpoke.karango.*
                        import de.peekandpoke.karango.aql.*
                        import de.peekandpoke.ultra.vault.lang.*
                        
                        //// generic property
                        inline fun <reified T> AqlIterableExpr<SomeClass>.property(name: String) = AqlPropertyPath.start(this).append<T, T>(name)
                        
                        // num /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                        // annotations: 
                        // defined as:   Primary Constructor Param
                        // defined by:   Class karango.compile.SomeClass
                        // defined at:   Line 16
                        // defined type: kotlin.Int
                        // cleaned type: kotlin.Int
                        
                        inline val AqlIterableExpr<SomeClass>.num inline get() = AqlPropertyPath.start(this).append<kotlin.Int, kotlin.Int>("num")
                        inline val AqlExpression<SomeClass>.num inline get() = AqlPropertyPath.start(this).append<kotlin.Int, kotlin.Int>("num")
                        
                        inline val AqlPropertyPath<SomeClass, SomeClass>.num @JvmName("num_0") inline get() = append<kotlin.Int, kotlin.Int>("num")
                        inline val AqlPropertyPath<SomeClass, L1<SomeClass>>.num @JvmName("num_1") inline get() = append<kotlin.Int, L1<kotlin.Int>>("num")
                        inline val AqlPropertyPath<SomeClass, L2<SomeClass>>.num @JvmName("num_2") inline get() = append<kotlin.Int, L2<kotlin.Int>>("num")
                        inline val AqlPropertyPath<SomeClass, L3<SomeClass>>.num @JvmName("num_3") inline get() = append<kotlin.Int, L3<kotlin.Int>>("num")
                        inline val AqlPropertyPath<SomeClass, L4<SomeClass>>.num @JvmName("num_4") inline get() = append<kotlin.Int, L4<kotlin.Int>>("num")
                        inline val AqlPropertyPath<SomeClass, L5<SomeClass>>.num @JvmName("num_5") inline get() = append<kotlin.Int, L5<kotlin.Int>>("num")                        
                    """.trimIndent()
                )
            }
        }
    }
}
