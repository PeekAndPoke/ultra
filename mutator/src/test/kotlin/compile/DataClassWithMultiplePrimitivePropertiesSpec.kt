package de.peekandpoke.ultra.mutator.compile

import de.peekandpoke.ultra.meta.compileTest
import de.peekandpoke.ultra.meta.expectFileToMatch
import de.peekandpoke.ultra.mutator.Mutable
import de.peekandpoke.ultra.mutator.meta.MutatorAnnotationProcessor
import io.kotest.core.spec.style.StringSpec
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import java.time.LocalDateTime

@Suppress("UnnecessaryOptInAnnotation")
@OptIn(ExperimentalCompilerApi::class)
class DataClassWithMultiplePrimitivePropertiesSpec : StringSpec({

    "Compiling a data class with multiple primitive properties" {

        compileTest {

            processor(MutatorAnnotationProcessor())

            kotlin(
                "KTest.kt",
                """
                    package mutator.compile
                    
                    import ${Mutable::class.qualifiedName}
                    import ${LocalDateTime::class.qualifiedName}
                    
                    @Mutable
                    data class KTest(
                        val text: String, 
                        val num: Int, 
                        val date: LocalDateTime
                    )
                
                """.trimIndent()
            )

            expectFileToMatch(
                "KTest${"$$"}mutator.kt",
                """
                    @file:Suppress("UNUSED_ANONYMOUS_PARAMETER")
                    
                    package mutator.compile
                    
                    import de.peekandpoke.ultra.mutator.*
                    import java.time.LocalDateTime
                    
                    
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
                    
                        /**
                         * Mutator for field [KTest.num]
                         *
                         * Info:
                         *   - type:         [Int]
                         *   - reflected by: [com.squareup.kotlinpoet.ClassName]
                         */ 
                        var num
                            get() = getResult().num
                            set(v) = modify(getResult()::num, getResult().num, v)
                    
                        /**
                         * Mutator for field [KTest.date]
                         *
                         * Info:
                         *   - type:         [LocalDateTime]
                         *   - reflected by: [com.squareup.kotlinpoet.ClassName]
                         */ 
                        var date
                            get() = getResult().date
                            set(v) = modify(getResult()::date, getResult().date, v)
                    
                    }
                    
                """.trimIndent()
            )
        }
    }
})
