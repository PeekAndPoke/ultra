package de.kotlincook.karango.ksp

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.symbolProcessorProviders
import de.peekandpoke.karango.ksp.KarangoKspProcessorProvider
import de.peekandpoke.ultra.vault.Vault
import io.kotest.common.ExperimentalKotest
import io.kotest.core.spec.style.StringSpec
import io.kotest.engine.test.logging.warn
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi

@OptIn(ExperimentalCompilerApi::class, ExperimentalKotest::class)
class NestedClassCodeGenSpec : StringSpec() {
    init {

        "Generating code for a nested class" {

            val source = SourceFile.kotlin(
                name = "nested.kt",
                contents = """
                    package karango.compile
                    
                    import ${Vault::class.qualifiedName}

                    @Vault
                    data class Outer(val inner: Inner) {
                        data class Inner(val num: Int)
                    }

                """.trimIndent()
            )

            val compilation = KotlinCompilation().apply {
                sources = listOf(source)
                symbolProcessorProviders.add(KarangoKspProcessorProvider())
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
//                    "Outer${"$$"}karango.kt",
//                    """
//                        package karango.compile
//
//                        import de.peekandpoke.karango.*
//                        import de.peekandpoke.karango.aql.*
//                        import de.peekandpoke.ultra.vault.lang.*
//
//                        //// generic property
//                        inline fun <reified T> IterableExpr<Outer>.property(name: String) = PropertyPath.start(this).append<T, T>(name)
//
//                        //// inner /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//                        //// annotations: [@org.jetbrains.annotations.NotNull]
//
//                        inline val IterableExpr<Outer>.inner inline get() = PropertyPath.start(this).append<karango.compile.Outer.Inner, karango.compile.Outer.Inner>("inner")
//                        inline val Expression<Outer>.inner inline get() = PropertyPath.start(this).append<karango.compile.Outer.Inner, karango.compile.Outer.Inner>("inner")
//
//                        inline val PropertyPath<Outer, Outer>.inner inline @JvmName("inner_0") get() = append<karango.compile.Outer.Inner, karango.compile.Outer.Inner>("inner")
//                        inline val PropertyPath<Outer, L1<Outer>>.inner inline @JvmName("inner_1") get() = append<karango.compile.Outer.Inner, L1<karango.compile.Outer.Inner>>("inner")
//                        inline val PropertyPath<Outer, L2<Outer>>.inner inline @JvmName("inner_2") get() = append<karango.compile.Outer.Inner, L2<karango.compile.Outer.Inner>>("inner")
//                        inline val PropertyPath<Outer, L3<Outer>>.inner inline @JvmName("inner_3") get() = append<karango.compile.Outer.Inner, L3<karango.compile.Outer.Inner>>("inner")
//                        inline val PropertyPath<Outer, L4<Outer>>.inner inline @JvmName("inner_4") get() = append<karango.compile.Outer.Inner, L4<karango.compile.Outer.Inner>>("inner")
//                        inline val PropertyPath<Outer, L5<Outer>>.inner inline @JvmName("inner_5") get() = append<karango.compile.Outer.Inner, L5<karango.compile.Outer.Inner>>("inner")
//                    """.trimIndent()
//                )
//
//                expectFileToMatch(
//                    "Outer.Inner${"$$"}karango.kt",
//                    """
//                        package karango.compile
//
//                        import de.peekandpoke.karango.*
//                        import de.peekandpoke.karango.aql.*
//                        import de.peekandpoke.ultra.vault.lang.*
//
//                        //// generic property
//                        inline fun <reified T> IterableExpr<Outer.Inner>.property(name: String) = PropertyPath.start(this).append<T, T>(name)
//
//                        //// num ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//                        //// annotations: []
//
//                        inline val IterableExpr<Outer.Inner>.num inline get() = PropertyPath.start(this).append<kotlin.Int, kotlin.Int>("num")
//                        inline val Expression<Outer.Inner>.num inline get() = PropertyPath.start(this).append<kotlin.Int, kotlin.Int>("num")
//
//                        inline val PropertyPath<Outer.Inner, Outer.Inner>.num inline @JvmName("num_0") get() = append<kotlin.Int, kotlin.Int>("num")
//                        inline val PropertyPath<Outer.Inner, L1<Outer.Inner>>.num inline @JvmName("num_1") get() = append<kotlin.Int, L1<kotlin.Int>>("num")
//                        inline val PropertyPath<Outer.Inner, L2<Outer.Inner>>.num inline @JvmName("num_2") get() = append<kotlin.Int, L2<kotlin.Int>>("num")
//                        inline val PropertyPath<Outer.Inner, L3<Outer.Inner>>.num inline @JvmName("num_3") get() = append<kotlin.Int, L3<kotlin.Int>>("num")
//                        inline val PropertyPath<Outer.Inner, L4<Outer.Inner>>.num inline @JvmName("num_4") get() = append<kotlin.Int, L4<kotlin.Int>>("num")
//                        inline val PropertyPath<Outer.Inner, L5<Outer.Inner>>.num inline @JvmName("num_5") get() = append<kotlin.Int, L5<kotlin.Int>>("num")
//                    """.trimIndent()
//                )
        }
    }
}
