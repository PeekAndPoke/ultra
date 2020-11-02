package de.peekandpoke.ultra.mutator.compile

import de.peekandpoke.ultra.meta.compileTest
import de.peekandpoke.ultra.meta.expectFileToMatch
import de.peekandpoke.ultra.mutator.Mutable
import de.peekandpoke.ultra.mutator.meta.MutatorAnnotationProcessor
import io.kotest.core.spec.style.StringSpec

class DataClassWithMapPropertySpec : StringSpec({

    "Compiling a data class with one list property" {

        compileTest {

            processor(MutatorAnnotationProcessor())

            kotlin(
                "KTest.kt",
                """
                    package mutator.compile
                    
                    import ${Mutable::class.qualifiedName}
                    
                    @Mutable
                    data class KTest(val value: Map<String, Int>)
                
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
                         * Mutator for field [KTest.value]
                         *
                         * Info:
                         *   - type:         [Map<String, Int>]
                         *   - reflected by: [com.squareup.kotlinpoet.ParameterizedTypeName]
                         */ 
                        val value by lazy {
                            getResult().value.mutator(
                                { modify(getResult()::value, getResult().value, it) },
                                { it1 -> it1 },
                                { it1, on1 -> 
                                    it1
                                }
                            )
                        }
                    }
                    
                """.trimIndent()
            )
        }
    }
})
