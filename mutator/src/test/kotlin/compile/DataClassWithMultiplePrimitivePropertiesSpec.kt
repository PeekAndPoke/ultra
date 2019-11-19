package de.peekandpoke.ultra.mutator.compile

import de.peekandpoke.ultra.meta.compileTest
import de.peekandpoke.ultra.meta.expectFileToMatch
import de.peekandpoke.ultra.mutator.Mutable
import de.peekandpoke.ultra.mutator.meta.MutatorAnnotationProcessor
import io.kotlintest.specs.StringSpec
import java.time.LocalDateTime

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
                    
                    fun KTest.mutate(mutation: KTestMutator.() -> Unit) = 
                        mutator().apply(mutation).getResult()
                    
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
                         *   - type:         java.lang.String
                         *   - reflected by: com.squareup.kotlinpoet.ClassName
                         */ 
                        var text: kotlin.String
                            get() = getResult().text
                            set(v) = modify(getResult()::text, getResult().text, v)

                        /**
                         * Mutator for field [KTest.num]
                         *
                         * Info:
                         *   - type:         kotlin.Int
                         *   - reflected by: com.squareup.kotlinpoet.ClassName
                         */ 
                        var num: kotlin.Int
                            get() = getResult().num
                            set(v) = modify(getResult()::num, getResult().num, v)

                        /**
                         * Mutator for field [KTest.date]
                         *
                         * Info:
                         *   - type:         java.time.LocalDateTime
                         *   - reflected by: com.squareup.kotlinpoet.ClassName
                         */ 
                        var date: java.time.LocalDateTime
                            get() = getResult().date
                            set(v) = modify(getResult()::date, getResult().date, v)
                            // Currently there is no better way to mutate a 'java.time.LocalDateTime' ... sorry!
                    
                    }
                    
                """.trimIndent()
            )
        }
    }
})
