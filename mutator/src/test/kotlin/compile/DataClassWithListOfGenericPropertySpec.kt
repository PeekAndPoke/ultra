package de.peekandpoke.ultra.mutator.compile

import de.peekandpoke.ultra.meta.compileTest
import de.peekandpoke.ultra.meta.expectFileToMatch
import de.peekandpoke.ultra.mutator.Mutable
import de.peekandpoke.ultra.mutator.meta.MutatorAnnotationProcessor
import io.kotest.core.spec.style.StringSpec
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi

@OptIn(ExperimentalCompilerApi::class)
class DataClassWithListOfGenericPropertySpec : StringSpec({

    "Compiling a data class with a list of a generic property" {

        compileTest {

            processor(MutatorAnnotationProcessor())

            kotlin(
                "KTest.kt",
                """
                    package mutator.compile
                    
                    import ${Mutable::class.qualifiedName}
                    
                    @Mutable
                    data class GenericUser(
                        val intList: List<Generic<Int>>,
                        val intListList: List<List<Generic<Int>>>
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
                         * Mutator for field [GenericUser.intList]
                         *
                         * Info:
                         *   - type:         [List<Generic<Int>>]
                         *   - reflected by: [com.squareup.kotlinpoet.ParameterizedTypeName]
                         */ 
                        val intList by lazy {
                            getResult().intList.mutator(
                                { modify(getResult()::intList, getResult().intList, it) },
                                { it1 -> it1.getResult() },
                                { it1, on1 -> 
                                    it1.mutator(on1)
                                }
                            )
                        }
                        /**
                         * Mutator for field [GenericUser.intListList]
                         *
                         * Info:
                         *   - type:         [List<List<Generic<Int>>>]
                         *   - reflected by: [com.squareup.kotlinpoet.ParameterizedTypeName]
                         */ 
                        val intListList by lazy {
                            getResult().intListList.mutator(
                                { modify(getResult()::intListList, getResult().intListList, it) },
                                { it1 -> it1.getResult() },
                                { it1, on1 -> 
                                    it1.mutator(on1, { it2 -> it2.getResult() }) { it2, on2 -> 
                                        it2.mutator(on2)
                                    }
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

                """.trimIndent()
            )
        }
    }
})
