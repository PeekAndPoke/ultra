package de.peekandpoke.ultra.mutator.compile

import de.peekandpoke.ultra.meta.compileTest
import de.peekandpoke.ultra.meta.expectFileToMatch
import de.peekandpoke.ultra.mutator.Mutable
import de.peekandpoke.ultra.mutator.meta.MutatorAnnotationProcessor
import io.kotest.core.spec.style.StringSpec
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi

@OptIn(ExperimentalCompilerApi::class)
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
                         * Backing field for [value]
                         */
                        private var `value@mutator` : PassThroughMutator_7ca16fdb07368b3c8b6342a44d2011b8? = null
                        
                        /**
                         * Mutator for field [Container.value]
                         *
                         * Info:
                         *   - type:         [PassThrough<Int>]
                         *   - reflected by: [com.squareup.kotlinpoet.ParameterizedTypeName]
                         */ 
                        var value : PassThroughMutator_7ca16fdb07368b3c8b6342a44d2011b8
                            get() = `value@mutator` ?: getResult().value.mutator { it: PassThrough<Int> -> 
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
                "PassThrough${"$$"}mutator.kt",
                """
                    @file:Suppress("UNUSED_ANONYMOUS_PARAMETER")
                    
                    package mutator.compile
                    
                    import de.peekandpoke.ultra.mutator.*
                    
                    
                    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                    // Mutator for PassThrough<Int> -> PassThroughMutator_7ca16fdb07368b3c8b6342a44d2011b8
                    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                    
                    @JvmName("mutatePassThroughMutator7ca16fdb07368b3c8b6342a44d2011b8")
                    fun PassThrough<Int>.mutate(mutation: PassThroughMutator_7ca16fdb07368b3c8b6342a44d2011b8.() -> Unit) = 
                        mutator({ x: PassThrough<Int> -> Unit }).apply(mutation).getResult()
                    
                    @JvmName("mutatorPassThroughMutator7ca16fdb07368b3c8b6342a44d2011b8")
                    fun PassThrough<Int>.mutator(onModify: OnModify<PassThrough<Int>> = {}) = 
                        PassThroughMutator_7ca16fdb07368b3c8b6342a44d2011b8(this, onModify)
                    
                    class PassThroughMutator_7ca16fdb07368b3c8b6342a44d2011b8(
                        target: PassThrough<Int>, 
                        onModify: OnModify<PassThrough<Int>> = {}
                    ) : DataClassMutator<PassThrough<Int>>(target, onModify) {
                    
                        /**
                         * Backing field for [v1]
                         */
                        private var `v1@mutator` : GenericMutator_a54cb119eaef222dc57bc6466b84e12f? = null
                        
                        /**
                         * Mutator for field [PassThrough<Int>.v1]
                         *
                         * Info:
                         *   - type:         [Generic<String, Int>]
                         *   - reflected by: [com.squareup.kotlinpoet.ParameterizedTypeName]
                         */ 
                        var v1 : GenericMutator_a54cb119eaef222dc57bc6466b84e12f
                            get() = `v1@mutator` ?: getResult().v1.mutator { it: Generic<String, Int> -> 
                                modify(getResult()::v1, getResult().v1, it) 
                            }.apply {
                                `v1@mutator` = this
                            }
                            set(value) {
                                `v1@mutator` = null
                                modify(getResult()::v1, getResult().v1, value.getResult()) 
                            }
                        
                        /**
                         * Backing field for [v2]
                         */
                        private var `v2@mutator` : GenericMutator_c90bcd8073a7379b80d8bcb3a7b95bdc? = null
                        
                        /**
                         * Mutator for field [PassThrough<Int>.v2]
                         *
                         * Info:
                         *   - type:         [Generic<Int, Int>]
                         *   - reflected by: [com.squareup.kotlinpoet.ParameterizedTypeName]
                         */ 
                        var v2 : GenericMutator_c90bcd8073a7379b80d8bcb3a7b95bdc
                            get() = `v2@mutator` ?: getResult().v2.mutator { it: Generic<Int, Int> -> 
                                modify(getResult()::v2, getResult().v2, it) 
                            }.apply {
                                `v2@mutator` = this
                            }
                            set(value) {
                                `v2@mutator` = null
                                modify(getResult()::v2, getResult().v2, value.getResult()) 
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
                    // Mutator for Generic<String, Int> -> GenericMutator_a54cb119eaef222dc57bc6466b84e12f
                    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                    
                    @JvmName("mutateGenericMutatora54cb119eaef222dc57bc6466b84e12f")
                    fun Generic<String, Int>.mutate(mutation: GenericMutator_a54cb119eaef222dc57bc6466b84e12f.() -> Unit) = 
                        mutator({ x: Generic<String, Int> -> Unit }).apply(mutation).getResult()
                    
                    @JvmName("mutatorGenericMutatora54cb119eaef222dc57bc6466b84e12f")
                    fun Generic<String, Int>.mutator(onModify: OnModify<Generic<String, Int>> = {}) = 
                        GenericMutator_a54cb119eaef222dc57bc6466b84e12f(this, onModify)
                    
                    class GenericMutator_a54cb119eaef222dc57bc6466b84e12f(
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
                    // Mutator for Generic<Int, Int> -> GenericMutator_c90bcd8073a7379b80d8bcb3a7b95bdc
                    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                    
                    @JvmName("mutateGenericMutatorc90bcd8073a7379b80d8bcb3a7b95bdc")
                    fun Generic<Int, Int>.mutate(mutation: GenericMutator_c90bcd8073a7379b80d8bcb3a7b95bdc.() -> Unit) = 
                        mutator({ x: Generic<Int, Int> -> Unit }).apply(mutation).getResult()
                    
                    @JvmName("mutatorGenericMutatorc90bcd8073a7379b80d8bcb3a7b95bdc")
                    fun Generic<Int, Int>.mutator(onModify: OnModify<Generic<Int, Int>> = {}) = 
                        GenericMutator_c90bcd8073a7379b80d8bcb3a7b95bdc(this, onModify)
                    
                    class GenericMutator_c90bcd8073a7379b80d8bcb3a7b95bdc(
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
