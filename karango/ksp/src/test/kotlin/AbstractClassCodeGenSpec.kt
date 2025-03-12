package de.kotlincook.karango.ksp

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.symbolProcessorProviders
import de.peekandpoke.karango.Karango
import io.kotest.common.ExperimentalKotest
import io.kotest.core.spec.style.StringSpec
import io.kotest.engine.test.logging.warn
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi

@OptIn(ExperimentalCompilerApi::class, ExperimentalKotest::class)
class AbstractClassCodeGenSpec : StringSpec() {
    init {

        "Generating Code for an abstract class" {

            val source = SourceFile.kotlin(
                name = "AbstractClass.kt",
                contents = """
                    package karango.compile
                    
                    import ${Karango::class.qualifiedName}
                    
                    @Karango
                    abstract class AbstractClass {
                        @Karango.Field
                        abstract val x: Int
                        
                        @Karango.Field
                        abstract val data: SomeClass
                        
                        abstract val noCodeGeneratedForMe: String
                    }

                    data class SomeClass(val num: Int)

                """.trimIndent()
            )

            val compilation = KotlinCompilation().apply {
                sources = listOf(source)
                symbolProcessorProviders = listOf(KarangoKspProcessorProvider())
                inheritClassPath = false
            }

            val result = compilation.compile()

            println()
            println("Generated sources: ${result.exitCode}")
            println()

            compilation.workingDir.walkTopDown().forEach {
                println(it)
            }

            warn { "Fix this as soon as CompileTesting supports Kotlin 2.1.10" }

//                expectFileToMatch(
//                    "AbstractClass${"$$"}karango.kt",
//                    """
//                        package karango.compile
//
//                        import de.peekandpoke.karango.*
//                        import de.peekandpoke.karango.aql.*
//                        import de.peekandpoke.ultra.vault.lang.*
//
//                        //// generic property
//                        inline fun <reified T> IterableExpr<AbstractClass>.property(name: String) = PropertyPath.start(this).append<T, T>(name)
//
//                        //// x /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//                        //// annotations: []
//
//                        inline val IterableExpr<AbstractClass>.x inline get() = PropertyPath.start(this).append<kotlin.Int, kotlin.Int>("x")
//                        inline val Expression<AbstractClass>.x inline get() = PropertyPath.start(this).append<kotlin.Int, kotlin.Int>("x")
//
//                        inline val PropertyPath<AbstractClass, AbstractClass>.x inline @JvmName("x_0") get() = append<kotlin.Int, kotlin.Int>("x")
//                        inline val PropertyPath<AbstractClass, L1<AbstractClass>>.x inline @JvmName("x_1") get() = append<kotlin.Int, L1<kotlin.Int>>("x")
//                        inline val PropertyPath<AbstractClass, L2<AbstractClass>>.x inline @JvmName("x_2") get() = append<kotlin.Int, L2<kotlin.Int>>("x")
//                        inline val PropertyPath<AbstractClass, L3<AbstractClass>>.x inline @JvmName("x_3") get() = append<kotlin.Int, L3<kotlin.Int>>("x")
//                        inline val PropertyPath<AbstractClass, L4<AbstractClass>>.x inline @JvmName("x_4") get() = append<kotlin.Int, L4<kotlin.Int>>("x")
//                        inline val PropertyPath<AbstractClass, L5<AbstractClass>>.x inline @JvmName("x_5") get() = append<kotlin.Int, L5<kotlin.Int>>("x")
//
//                        //// data //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//                        //// annotations: [@org.jetbrains.annotations.NotNull]
//
//                        inline val IterableExpr<AbstractClass>.data inline get() = PropertyPath.start(this).append<karango.compile.SomeClass, karango.compile.SomeClass>("data")
//                        inline val Expression<AbstractClass>.data inline get() = PropertyPath.start(this).append<karango.compile.SomeClass, karango.compile.SomeClass>("data")
//
//                        inline val PropertyPath<AbstractClass, AbstractClass>.data inline @JvmName("data_0") get() = append<karango.compile.SomeClass, karango.compile.SomeClass>("data")
//                        inline val PropertyPath<AbstractClass, L1<AbstractClass>>.data inline @JvmName("data_1") get() = append<karango.compile.SomeClass, L1<karango.compile.SomeClass>>("data")
//                        inline val PropertyPath<AbstractClass, L2<AbstractClass>>.data inline @JvmName("data_2") get() = append<karango.compile.SomeClass, L2<karango.compile.SomeClass>>("data")
//                        inline val PropertyPath<AbstractClass, L3<AbstractClass>>.data inline @JvmName("data_3") get() = append<karango.compile.SomeClass, L3<karango.compile.SomeClass>>("data")
//                        inline val PropertyPath<AbstractClass, L4<AbstractClass>>.data inline @JvmName("data_4") get() = append<karango.compile.SomeClass, L4<karango.compile.SomeClass>>("data")
//                        inline val PropertyPath<AbstractClass, L5<AbstractClass>>.data inline @JvmName("data_5") get() = append<karango.compile.SomeClass, L5<karango.compile.SomeClass>>("data")
//                    """.trimIndent()
//                )
        }
    }
}
