package de.peekandpoke.ultra.mutator.compile

import de.peekandpoke.ultra.meta.compileTest
import de.peekandpoke.ultra.meta.expectFileToMatch
import de.peekandpoke.ultra.mutator.Mutable
import de.peekandpoke.ultra.mutator.meta.MutatorAnnotationProcessor
import io.kotlintest.specs.StringSpec

class DataClassWithMultipleGenericPrimitivePropertiesSpec : StringSpec({

    "Compiling a data class with multiple generic properties" {

        compileTest {

            processor(MutatorAnnotationProcessor())

            kotlin(
                "KTest.kt",
                """
                    package mutator.compile
                    
                    import ${Mutable::class.qualifiedName}
                    
                    @Mutable
                    data class GenericUser(
                        val intVal: Generic<Int>,
                        val strVal: Generic<String>,
                        val intList: Generic<List<Int>>,
                        val listInt: List<Generic<Int>>
                    )

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
                         * Mutator for field [GenericUser.intVal]
                         *
                         * Info:
                         *   - type:         [Generic<Int>]
                         *   - reflected by: [com.squareup.kotlinpoet.ParameterizedTypeName]
                         */ 
                        val intVal by lazy { 
                            getResult().intVal.mutator { modify(getResult()::intVal, getResult().intVal, it) } 
                        }
                        
                        /**
                         * Mutator for field [GenericUser.strVal]
                         *
                         * Info:
                         *   - type:         [Generic<String>]
                         *   - reflected by: [com.squareup.kotlinpoet.ParameterizedTypeName]
                         */ 
                        val strVal by lazy { 
                            getResult().strVal.mutator { modify(getResult()::strVal, getResult().strVal, it) } 
                        }
                        
                        /**
                         * Mutator for field [GenericUser.intList]
                         *
                         * Info:
                         *   - type:         [Generic<List<Int>>]
                         *   - reflected by: [com.squareup.kotlinpoet.ParameterizedTypeName]
                         */ 
                        val intList by lazy { 
                            getResult().intList.mutator { modify(getResult()::intList, getResult().intList, it) } 
                        }
                        
                        /**
                         * Mutator for field [GenericUser.listInt]
                         *
                         * Info:
                         *   - type:         [List<Generic<Int>>]
                         *   - reflected by: [com.squareup.kotlinpoet.ParameterizedTypeName]
                         */ 
                        val listInt by lazy {
                            getResult().listInt.mutator(
                                { modify(getResult()::listInt, getResult().listInt, it) },
                                { it1 -> it1.getResult() },
                                { it1, on1 -> 
                                    it1.mutator(on1)
                                }
                            )
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
                        mutator({ x: Generic<Int> -> Unit }).apply(mutation).getResult()
                    
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
                    
                    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                    // Mutator for Generic<String> -> GenericMutator_1
                    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                    
                    @JvmName("mutateGenericMutator1")
                    fun Generic<String>.mutate(mutation: GenericMutator_1.() -> Unit) = 
                        mutator({ x: Generic<String> -> Unit }).apply(mutation).getResult()
                    
                    @JvmName("mutatorGenericMutator1")
                    fun Generic<String>.mutator(onModify: OnModify<Generic<String>> = {}) = 
                        GenericMutator_1(this, onModify)
                    
                    class GenericMutator_1(
                        target: Generic<String>, 
                        onModify: OnModify<Generic<String>> = {}
                    ) : DataClassMutator<Generic<String>>(target, onModify) {
                    
                        /**
                         * Mutator for field [Generic<String>.value]
                         *
                         * Info:
                         *   - type:         [String]
                         *   - reflected by: [com.squareup.kotlinpoet.ClassName]
                         */ 
                        var value
                            get() = getResult().value
                            set(v) = modify(getResult()::value, getResult().value, v)
                    
                    }
                    
                    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                    // Mutator for Generic<List<Int>> -> GenericMutator_2
                    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                    
                    @JvmName("mutateGenericMutator2")
                    fun Generic<List<Int>>.mutate(mutation: GenericMutator_2.() -> Unit) = 
                        mutator({ x: Generic<List<Int>> -> Unit }).apply(mutation).getResult()
                    
                    @JvmName("mutatorGenericMutator2")
                    fun Generic<List<Int>>.mutator(onModify: OnModify<Generic<List<Int>>> = {}) = 
                        GenericMutator_2(this, onModify)
                    
                    class GenericMutator_2(
                        target: Generic<List<Int>>, 
                        onModify: OnModify<Generic<List<Int>>> = {}
                    ) : DataClassMutator<Generic<List<Int>>>(target, onModify) {
                    
                        /**
                         * Mutator for field [Generic<List<Int>>.value]
                         *
                         * Info:
                         *   - type:         [List<Int>]
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
