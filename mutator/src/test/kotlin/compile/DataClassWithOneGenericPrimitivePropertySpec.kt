package de.peekandpoke.ultra.mutator.compile

import de.peekandpoke.ultra.meta.compileTest
import de.peekandpoke.ultra.meta.expectFileToMatch
import de.peekandpoke.ultra.mutator.Mutable
import de.peekandpoke.ultra.mutator.meta.MutatorAnnotationProcessor
import io.kotest.core.spec.style.StringSpec

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
                        mutator({ x: GenericUser -> Unit }).apply(mutation).getResult()
                    
                    @JvmName("mutatorGenericUserMutator")
                    fun GenericUser.mutator(onModify: OnModify<GenericUser> = {}) = 
                        GenericUserMutator(this, onModify)
                    
                    class GenericUserMutator(
                        target: GenericUser, 
                        onModify: OnModify<GenericUser> = {}
                    ) : DataClassMutator<GenericUser>(target, onModify) {
                    
                        /**
                         * Backing field for [value]
                         */
                        private var `value@mutator` : GenericMutator_7ca16fdb07368b3c8b6342a44d2011b8? = null
                        
                        /**
                         * Mutator for field [GenericUser.value]
                         *
                         * Info:
                         *   - type:         [Generic<Int>]
                         *   - reflected by: [com.squareup.kotlinpoet.ParameterizedTypeName]
                         */ 
                        var value : GenericMutator_7ca16fdb07368b3c8b6342a44d2011b8
                            get() = `value@mutator` ?: getResult().value.mutator { it: Generic<Int> -> 
                                modify(getResult()::value, getResult().value, it) 
                            }.apply {
                                `value@mutator` = this
                            }
                            set(value) {
                                `value@mutator` = null
                                modify(getResult()::value, getResult().value, value.getResult()) 
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
                    // Mutator for Generic<Int> -> GenericMutator_7ca16fdb07368b3c8b6342a44d2011b8
                    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                    
                    @JvmName("mutateGenericMutator7ca16fdb07368b3c8b6342a44d2011b8")
                    fun Generic<Int>.mutate(mutation: GenericMutator_7ca16fdb07368b3c8b6342a44d2011b8.() -> Unit) = 
                        mutator({ x: Generic<Int> -> Unit }).apply(mutation).getResult()
                    
                    @JvmName("mutatorGenericMutator7ca16fdb07368b3c8b6342a44d2011b8")
                    fun Generic<Int>.mutator(onModify: OnModify<Generic<Int>> = {}) = 
                        GenericMutator_7ca16fdb07368b3c8b6342a44d2011b8(this, onModify)
                    
                    class GenericMutator_7ca16fdb07368b3c8b6342a44d2011b8(
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
