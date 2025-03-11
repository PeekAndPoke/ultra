package de.peekandpoke.ultra.mutator.compile

import de.peekandpoke.ultra.meta.testing.expectFileToMatch
import de.peekandpoke.ultra.meta.testing.kaptCompileTest
import de.peekandpoke.ultra.mutator.Mutable
import de.peekandpoke.ultra.mutator.meta.MutatorAnnotationProcessor
import io.kotest.core.spec.style.StringSpec
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi

@OptIn(ExperimentalCompilerApi::class)
class DataClassWithListListPropertySpec : StringSpec({

    "Compiling a data class with one list of lists property" {

        kaptCompileTest {

            processor(MutatorAnnotationProcessor())

            kotlin(
                "KTest.kt",
                """
                    package mutator.compile
                    
                    import ${Mutable::class.qualifiedName}
                    
                    @Mutable
                    data class StringLists(val value: List<List<String>>)

                    @Mutable
                    data class DataObjectLists(val value: List<List<DataObject>>)

                    data class DataObject(val text: String)

                """.trimIndent()
            )

            expectFileToMatch(
                "StringLists${"$$"}mutator.kt",
                """
                    @file:Suppress("UNUSED_ANONYMOUS_PARAMETER")
                    
                    package mutator.compile
                    
                    import de.peekandpoke.ultra.mutator.*
                    
                    
                    @JvmName("mutateStringListsMutator")
                    fun StringLists.mutate(mutation: StringListsMutator.() -> Unit) = 
                        mutator({ x: StringLists -> Unit }).apply(mutation).getResult()
                    
                    @JvmName("mutatorStringListsMutator")
                    fun StringLists.mutator(onModify: OnModify<StringLists> = {}) = 
                        StringListsMutator(this, onModify)
                    
                    class StringListsMutator(
                        target: StringLists, 
                        onModify: OnModify<StringLists> = {}
                    ) : DataClassMutator<StringLists>(target, onModify) {
                    
                        /**
                         * Mutator for field [StringLists.value]
                         *
                         * Info:
                         *   - type:         [List<List<String>>]
                         *   - reflected by: [com.squareup.kotlinpoet.ParameterizedTypeName]
                         */ 
                        val value by lazy {
                            getResult().value.mutator(
                                { modify(getResult()::value, getResult().value, it) },
                                { it1 -> it1.getResult() },
                                { it1, on1 -> 
                                    it1.mutator(on1, { it2 -> it2 }) { it2, on2 -> 
                                        it2
                                    }
                                }
                            )
                        }
                    }
                    
                """.trimIndent()
            )

            expectFileToMatch(
                "DataObjectLists${"$$"}mutator.kt",
                """
                    @file:Suppress("UNUSED_ANONYMOUS_PARAMETER")
                    
                    package mutator.compile
                    
                    import de.peekandpoke.ultra.mutator.*
                    
                    
                    @JvmName("mutateDataObjectListsMutator")
                    fun DataObjectLists.mutate(mutation: DataObjectListsMutator.() -> Unit) = 
                        mutator({ x: DataObjectLists -> Unit }).apply(mutation).getResult()
                    
                    @JvmName("mutatorDataObjectListsMutator")
                    fun DataObjectLists.mutator(onModify: OnModify<DataObjectLists> = {}) = 
                        DataObjectListsMutator(this, onModify)
                    
                    class DataObjectListsMutator(
                        target: DataObjectLists, 
                        onModify: OnModify<DataObjectLists> = {}
                    ) : DataClassMutator<DataObjectLists>(target, onModify) {
                    
                        /**
                         * Mutator for field [DataObjectLists.value]
                         *
                         * Info:
                         *   - type:         [List<List<DataObject>>]
                         *   - reflected by: [com.squareup.kotlinpoet.ParameterizedTypeName]
                         */ 
                        val value by lazy {
                            getResult().value.mutator(
                                { modify(getResult()::value, getResult().value, it) },
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
        }
    }
})
