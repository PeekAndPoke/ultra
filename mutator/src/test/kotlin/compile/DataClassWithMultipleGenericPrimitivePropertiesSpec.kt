package de.peekandpoke.ultra.mutator.compile

import de.peekandpoke.ultra.meta.compileTest
import de.peekandpoke.ultra.meta.expectFileToMatch
import de.peekandpoke.ultra.mutator.Mutable
import de.peekandpoke.ultra.mutator.meta.MutatorAnnotationProcessor
import io.kotest.core.spec.style.StringSpec

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
                         * Backing field for [intVal]
                         */
                        private var `intVal@mutator` : GenericMutator_7ca16fdb07368b3c8b6342a44d2011b8? = null
                        
                        /**
                         * Mutator for field [GenericUser.intVal]
                         *
                         * Info:
                         *   - type:         [Generic<Int>]
                         *   - reflected by: [com.squareup.kotlinpoet.ParameterizedTypeName]
                         */ 
                        var intVal : GenericMutator_7ca16fdb07368b3c8b6342a44d2011b8
                            get() = `intVal@mutator` ?: getResult().intVal.mutator { it: Generic<Int> -> 
                                modify(getResult()::intVal, getResult().intVal, it) 
                            }.apply {
                                `intVal@mutator` = this
                            }
                            set(value) {
                                `intVal@mutator` = null
                                modify(getResult()::intVal, getResult().intVal, value.getResult()) 
                            }
                        
                        /**
                         * Backing field for [strVal]
                         */
                        private var `strVal@mutator` : GenericMutator_42c25be3a5db3f1f8377f4c2db62bfe9? = null
                        
                        /**
                         * Mutator for field [GenericUser.strVal]
                         *
                         * Info:
                         *   - type:         [Generic<String>]
                         *   - reflected by: [com.squareup.kotlinpoet.ParameterizedTypeName]
                         */ 
                        var strVal : GenericMutator_42c25be3a5db3f1f8377f4c2db62bfe9
                            get() = `strVal@mutator` ?: getResult().strVal.mutator { it: Generic<String> -> 
                                modify(getResult()::strVal, getResult().strVal, it) 
                            }.apply {
                                `strVal@mutator` = this
                            }
                            set(value) {
                                `strVal@mutator` = null
                                modify(getResult()::strVal, getResult().strVal, value.getResult()) 
                            }
                        
                        /**
                         * Backing field for [intList]
                         */
                        private var `intList@mutator` : GenericMutator_59e9d61229bcf4ec991d2d7262078600? = null
                        
                        /**
                         * Mutator for field [GenericUser.intList]
                         *
                         * Info:
                         *   - type:         [Generic<List<Int>>]
                         *   - reflected by: [com.squareup.kotlinpoet.ParameterizedTypeName]
                         */ 
                        var intList : GenericMutator_59e9d61229bcf4ec991d2d7262078600
                            get() = `intList@mutator` ?: getResult().intList.mutator { it: Generic<List<Int>> -> 
                                modify(getResult()::intList, getResult().intList, it) 
                            }.apply {
                                `intList@mutator` = this
                            }
                            set(value) {
                                `intList@mutator` = null
                                modify(getResult()::intList, getResult().intList, value.getResult()) 
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
                    
                    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                    // Mutator for Generic<String> -> GenericMutator_42c25be3a5db3f1f8377f4c2db62bfe9
                    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                    
                    @JvmName("mutateGenericMutator42c25be3a5db3f1f8377f4c2db62bfe9")
                    fun Generic<String>.mutate(mutation: GenericMutator_42c25be3a5db3f1f8377f4c2db62bfe9.() -> Unit) = 
                        mutator({ x: Generic<String> -> Unit }).apply(mutation).getResult()
                    
                    @JvmName("mutatorGenericMutator42c25be3a5db3f1f8377f4c2db62bfe9")
                    fun Generic<String>.mutator(onModify: OnModify<Generic<String>> = {}) = 
                        GenericMutator_42c25be3a5db3f1f8377f4c2db62bfe9(this, onModify)
                    
                    class GenericMutator_42c25be3a5db3f1f8377f4c2db62bfe9(
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
                    // Mutator for Generic<List<Int>> -> GenericMutator_59e9d61229bcf4ec991d2d7262078600
                    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                    
                    @JvmName("mutateGenericMutator59e9d61229bcf4ec991d2d7262078600")
                    fun Generic<List<Int>>.mutate(mutation: GenericMutator_59e9d61229bcf4ec991d2d7262078600.() -> Unit) = 
                        mutator({ x: Generic<List<Int>> -> Unit }).apply(mutation).getResult()
                    
                    @JvmName("mutatorGenericMutator59e9d61229bcf4ec991d2d7262078600")
                    fun Generic<List<Int>>.mutator(onModify: OnModify<Generic<List<Int>>> = {}) = 
                        GenericMutator_59e9d61229bcf4ec991d2d7262078600(this, onModify)
                    
                    class GenericMutator_59e9d61229bcf4ec991d2d7262078600(
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
