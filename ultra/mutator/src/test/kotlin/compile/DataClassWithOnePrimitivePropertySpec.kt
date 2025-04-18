package de.peekandpoke.ultra.mutator.compile

import de.peekandpoke.ultra.meta.testing.expectFileToMatch
import de.peekandpoke.ultra.meta.testing.kaptCompileTest
import de.peekandpoke.ultra.mutator.Mutable
import de.peekandpoke.ultra.mutator.meta.MutatorAnnotationProcessor
import io.kotest.core.spec.style.StringSpec
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi

@OptIn(ExperimentalCompilerApi::class)
class DataClassWithOnePrimitivePropertySpec : StringSpec({

    "Compiling a data class with one primitive property" {

        kaptCompileTest {

            processor(MutatorAnnotationProcessor())

            kotlin(
                "KTest.kt",
                """
                    package mutator.compile
                    
                    import ${Mutable::class.qualifiedName}
                    
                    @Mutable
                    data class KTest(val text: String)
                
                """.trimIndent()
            )

            expectFileToMatch(
                "KTest${"$$"}mutator.kt",
                """
                    @file:Suppress("UNUSED_ANONYMOUS_PARAMETER")
                    
                    package mutator.compile
                    
                    import de.peekandpoke.ultra.mutator.*
                    
                    
                    @JvmName("mutateKTestMutator")
                    fun KTest.mutate(mutation: KTestMutator.() -> Unit) = 
                        mutator({ x: KTest -> Unit }).apply(mutation).getResult()
                    
                    @JvmName("mutatorKTestMutator")
                    fun KTest.mutator(onModify: OnModify<KTest> = {}) = 
                        KTestMutator(this, onModify)
                    
                    class KTestMutator(
                        target: KTest, 
                        onModify: OnModify<KTest> = {}
                    ) : DataClassMutator<KTest>(target, onModify) {
                    
                        /**
                         * Mutator for field [KTest.text]
                         *
                         * Info:
                         *   - type:         [String]
                         *   - reflected by: [com.squareup.kotlinpoet.ClassName]
                         */ 
                        var text
                            get() = getResult().text
                            set(v) = modify(getResult()::text, getResult().text, v)
                    
                    }
                    
                """.trimIndent()
            )
        }
    }
})
