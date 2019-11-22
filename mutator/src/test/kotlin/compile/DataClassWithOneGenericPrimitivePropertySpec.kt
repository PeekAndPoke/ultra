package de.peekandpoke.ultra.mutator.compile

import de.peekandpoke.ultra.meta.compileTest
import de.peekandpoke.ultra.meta.expectFileToMatch
import de.peekandpoke.ultra.mutator.Mutable
import de.peekandpoke.ultra.mutator.meta.MutatorAnnotationProcessor
import io.kotlintest.specs.StringSpec

class DataClassWithOneGenericPrimitivePropertySpec : StringSpec({

    "Compiling a data class with one generic property" {

        compileTest {

            processor(MutatorAnnotationProcessor())

            kotlin(
                "KTest.kt",
                """
                    package mutator.compile
                    
                    import ${Mutable::class.qualifiedName}
                    
                    @Mutable
                    data class GenericUser(val value: Generic<Int>)

                    @Mutable
                    data class Generic<T>(val value: T)

                """.trimIndent()
            )

            expectFileToMatch(
                "GenericUser${"$$"}mutator.kt",
                """
                    @file:Suppress("UNUSED_ANONYMOUS_PARAMETER")
                    
                    package mutator.compile
                    
                    import de.peekandpoke.ultra.mutator.*
                    
                    
                    @JvmName("mutateGenericUserMutator")
                    fun GenericUser.mutate(mutation: GenericUserMutator.() -> Unit) = 
                        mutator().apply(mutation).getResult()
                    
                    @JvmName("mutatorGenericUserMutator")
                    fun GenericUser.mutator(onModify: OnModify<GenericUser> = {}) = 
                        GenericUserMutator(this, onModify)
                    
                    class GenericUserMutator(
                        target: GenericUser, 
                        onModify: OnModify<GenericUser> = {}
                    ) : DataClassMutator<GenericUser>(target, onModify) {
                    
                        /**
                         * Mutator for field [GenericUser.value]
                         *
                         * Info:
                         *   - type:         [Generic<Int>]
                         *   - reflected by: [com.squareup.kotlinpoet.ParameterizedTypeName]
                         */ 
                        val value by lazy { 
                            getResult().value.mutator { modify(getResult()::value, getResult().value, it) } 
                        }
                        
                    }

                """.trimIndent()
            )


            expectFileToMatch(
                "Generic${"$$"}mutator.kt",
                """
                    @file:Suppress("UNUSED_ANONYMOUS_PARAMETER")
                    
                    package mutator.compile
                    
                    import de.peekandpoke.ultra.mutator.*
                    
                    
                    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                    // Mutator for Generic<Int> -> GenericMutator_0
                    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                    
                    @JvmName("mutateGenericMutator0")
                    fun Generic<Int>.mutate(mutation: GenericMutator_0.() -> Unit) = 
                        mutator().apply(mutation).getResult()
                    
                    @JvmName("mutatorGenericMutator0")
                    fun Generic<Int>.mutator(onModify: OnModify<Generic<Int>> = {}) = 
                        GenericMutator_0(this, onModify)
                    
                    class GenericMutator_0(
                        target: Generic<Int>, 
                        onModify: OnModify<Generic<Int>> = {}
                    ) : DataClassMutator<Generic<Int>>(target, onModify) {
                    
                        /**
                         * Mutator for field [Generic<Int>.value]
                         *
                         * Info:
                         *   - type:         [Int]
                         *   - reflected by: [com.squareup.kotlinpoet.ClassName]
                         */ 
                        var value
                            get() = getResult().value
                            set(v) = modify(getResult()::value, getResult().value, v)
                    
                    }

                """.trimIndent()
            )
        }
    }
})
