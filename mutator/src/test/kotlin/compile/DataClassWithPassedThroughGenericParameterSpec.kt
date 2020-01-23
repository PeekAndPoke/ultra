package de.peekandpoke.ultra.mutator.compile

import de.peekandpoke.ultra.meta.compileTest
import de.peekandpoke.ultra.meta.expectFileToMatch
import de.peekandpoke.ultra.mutator.Mutable
import de.peekandpoke.ultra.mutator.meta.MutatorAnnotationProcessor
import io.kotlintest.specs.StringSpec

class DataClassWithPassedThroughGenericParameterSpec : StringSpec({

    "Compiling a data class with passed through generic parameter" {

        compileTest {

            processor(MutatorAnnotationProcessor())

            kotlin(
                "KTest.kt",
                """
                    package mutator.compile
                    
                    import ${Mutable::class.qualifiedName}
                    
                    @Mutable
                    data class Container(val value: PassThrough<Int>)
                    
                    @Mutable
                    data class PassThrough<T>(
                        val v1: Generic<String, T>,
                        val v2: Generic<T, Int>    
                    )

                    @Mutable
                    data class Generic<T, R>(val t: T, val r: R)

                """.trimIndent()
            )

            expectFileToMatch(
                "Container${"$$"}mutator.kt",
                """
                    @file:Suppress("UNUSED_ANONYMOUS_PARAMETER")
                    
                    package mutator.compile
                    
                    import de.peekandpoke.ultra.mutator.*
                    
                    
                    @JvmName("mutateContainerMutator")
                    fun Container.mutate(mutation: ContainerMutator.() -> Unit) = 
                        mutator({ x: Container -> Unit }).apply(mutation).getResult()
                    
                    @JvmName("mutatorContainerMutator")
                    fun Container.mutator(onModify: OnModify<Container> = {}) = 
                        ContainerMutator(this, onModify)
                    
                    class ContainerMutator(
                        target: Container, 
                        onModify: OnModify<Container> = {}
                    ) : DataClassMutator<Container>(target, onModify) {
                    
                        /**
                         * Mutator for field [Container.value]
                         *
                         * Info:
                         *   - type:         [PassThrough<Int>]
                         *   - reflected by: [com.squareup.kotlinpoet.ParameterizedTypeName]
                         */ 
                        val value by lazy { 
                            getResult().value.mutator { modify(getResult()::value, getResult().value, it) } 
                        }
                        
                    }

                """.trimIndent()
            )

            expectFileToMatch(
                "PassThrough${"$$"}mutator.kt",
                """
                    @file:Suppress("UNUSED_ANONYMOUS_PARAMETER")
                    
                    package mutator.compile
                    
                    import de.peekandpoke.ultra.mutator.*
                    
                    
                    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                    // Mutator for PassThrough<Int> -> PassThroughMutator_0
                    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                    
                    @JvmName("mutatePassThroughMutator0")
                    fun PassThrough<Int>.mutate(mutation: PassThroughMutator_0.() -> Unit) = 
                        mutator({ x: PassThrough<Int> -> Unit }).apply(mutation).getResult()
                    
                    @JvmName("mutatorPassThroughMutator0")
                    fun PassThrough<Int>.mutator(onModify: OnModify<PassThrough<Int>> = {}) = 
                        PassThroughMutator_0(this, onModify)
                    
                    class PassThroughMutator_0(
                        target: PassThrough<Int>, 
                        onModify: OnModify<PassThrough<Int>> = {}
                    ) : DataClassMutator<PassThrough<Int>>(target, onModify) {
                    
                        /**
                         * Mutator for field [PassThrough<Int>.v1]
                         *
                         * Info:
                         *   - type:         [Generic<String, Int>]
                         *   - reflected by: [com.squareup.kotlinpoet.ParameterizedTypeName]
                         */ 
                        val v1 by lazy { 
                            getResult().v1.mutator { modify(getResult()::v1, getResult().v1, it) } 
                        }
                        
                        /**
                         * Mutator for field [PassThrough<Int>.v2]
                         *
                         * Info:
                         *   - type:         [Generic<Int, Int>]
                         *   - reflected by: [com.squareup.kotlinpoet.ParameterizedTypeName]
                         */ 
                        val v2 by lazy { 
                            getResult().v2.mutator { modify(getResult()::v2, getResult().v2, it) } 
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
                    // Mutator for Generic<String, Int> -> GenericMutator_0
                    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                    
                    @JvmName("mutateGenericMutator0")
                    fun Generic<String, Int>.mutate(mutation: GenericMutator_0.() -> Unit) = 
                        mutator({ x: Generic<String, Int> -> Unit }).apply(mutation).getResult()
                    
                    @JvmName("mutatorGenericMutator0")
                    fun Generic<String, Int>.mutator(onModify: OnModify<Generic<String, Int>> = {}) = 
                        GenericMutator_0(this, onModify)
                    
                    class GenericMutator_0(
                        target: Generic<String, Int>, 
                        onModify: OnModify<Generic<String, Int>> = {}
                    ) : DataClassMutator<Generic<String, Int>>(target, onModify) {
                    
                        /**
                         * Mutator for field [Generic<String, Int>.t]
                         *
                         * Info:
                         *   - type:         [String]
                         *   - reflected by: [com.squareup.kotlinpoet.ClassName]
                         */ 
                        var t
                            get() = getResult().t
                            set(v) = modify(getResult()::t, getResult().t, v)
                    
                        /**
                         * Mutator for field [Generic<String, Int>.r]
                         *
                         * Info:
                         *   - type:         [Int]
                         *   - reflected by: [com.squareup.kotlinpoet.ClassName]
                         */ 
                        var r
                            get() = getResult().r
                            set(v) = modify(getResult()::r, getResult().r, v)
                    
                    }
                    
                    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                    // Mutator for Generic<Int, Int> -> GenericMutator_1
                    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                    
                    @JvmName("mutateGenericMutator1")
                    fun Generic<Int, Int>.mutate(mutation: GenericMutator_1.() -> Unit) = 
                        mutator({ x: Generic<Int, Int> -> Unit }).apply(mutation).getResult()
                    
                    @JvmName("mutatorGenericMutator1")
                    fun Generic<Int, Int>.mutator(onModify: OnModify<Generic<Int, Int>> = {}) = 
                        GenericMutator_1(this, onModify)
                    
                    class GenericMutator_1(
                        target: Generic<Int, Int>, 
                        onModify: OnModify<Generic<Int, Int>> = {}
                    ) : DataClassMutator<Generic<Int, Int>>(target, onModify) {
                    
                        /**
                         * Mutator for field [Generic<Int, Int>.t]
                         *
                         * Info:
                         *   - type:         [Int]
                         *   - reflected by: [com.squareup.kotlinpoet.ClassName]
                         */ 
                        var t
                            get() = getResult().t
                            set(v) = modify(getResult()::t, getResult().t, v)
                    
                        /**
                         * Mutator for field [Generic<Int, Int>.r]
                         *
                         * Info:
                         *   - type:         [Int]
                         *   - reflected by: [com.squareup.kotlinpoet.ClassName]
                         */ 
                        var r
                            get() = getResult().r
                            set(v) = modify(getResult()::r, getResult().r, v)
                    
                    }

                """.trimIndent()
            )
        }
    }
})
