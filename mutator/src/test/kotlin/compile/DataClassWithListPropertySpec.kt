package de.peekandpoke.ultra.mutator.compile

import de.peekandpoke.ultra.meta.compileTest
import de.peekandpoke.ultra.meta.expectFileToMatch
import de.peekandpoke.ultra.mutator.Mutable
import de.peekandpoke.ultra.mutator.meta.MutatorAnnotationProcessor
import io.kotlintest.specs.StringSpec

class DataClassWithListPropertySpec : StringSpec({

    "Compiling a data class with one list property" {

        compileTest {

            processor(MutatorAnnotationProcessor())

            kotlin(
                "KTest.kt",
                """
                    package mutator.compile
                    
                    import ${Mutable::class.qualifiedName}
                    
                    @Mutable
                    data class StringList(val value: List<String>)
                
                    @Mutable
                    data class DataObjectList(val value: List<DataObject>)
                
                    data class DataObject(val text: String)
                
                """.trimIndent()
            )

            expectFileToMatch(
                "StringList${"$$"}mutator.kt",
                """
                    @file:Suppress("UNUSED_ANONYMOUS_PARAMETER")
                    
                    package mutator.compile
                    
                    import de.peekandpoke.ultra.mutator.*
                    
                    fun StringList.mutate(mutation: StringListMutator.() -> Unit) = 
                        mutator().apply(mutation).getResult()
                    
                    fun StringList.mutator(onModify: OnModify<StringList> = {}) = 
                        StringListMutator(this, onModify)
                    
                    class StringListMutator(
                        target: StringList, 
                        onModify: OnModify<StringList> = {}
                    ) : DataClassMutator<StringList>(target, onModify) {

                        /**
                         * Mutator for field [StringList.value]
                         *
                         * Info:
                         *   - type:         java.util.List<java.lang.String>
                         *   - reflected by: com.squareup.kotlinpoet.ParameterizedTypeName
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

            expectFileToMatch(
                "DataObjectList${"$$"}mutator.kt",
                """
                    @file:Suppress("UNUSED_ANONYMOUS_PARAMETER")
                    
                    package mutator.compile
                    
                    import de.peekandpoke.ultra.mutator.*
                    
                    fun DataObjectList.mutate(mutation: DataObjectListMutator.() -> Unit) = 
                        mutator().apply(mutation).getResult()
                    
                    fun DataObjectList.mutator(onModify: OnModify<DataObjectList> = {}) = 
                        DataObjectListMutator(this, onModify)
                    
                    class DataObjectListMutator(
                        target: DataObjectList, 
                        onModify: OnModify<DataObjectList> = {}
                    ) : DataClassMutator<DataObjectList>(target, onModify) {
                    
                        /**
                         * Mutator for field [DataObjectList.value]
                         *
                         * Info:
                         *   - type:         java.util.List<mutator.compile.DataObject>
                         *   - reflected by: com.squareup.kotlinpoet.ParameterizedTypeName
                         */ 
                        val value by lazy {
                            getResult().value.mutator(
                                { modify(getResult()::value, getResult().value, it) },
                                { it1 -> it1.getResult() },
                                { it1, on1 ->
                                    it1.mutator(on1)
                                }
                            )
                        }
                        
                    }
                    
                """.trimIndent()
            )
        }
    }
})
