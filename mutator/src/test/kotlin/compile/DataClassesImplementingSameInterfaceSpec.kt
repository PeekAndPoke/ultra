package de.peekandpoke.ultra.mutator.compile

import de.peekandpoke.ultra.meta.compileTest
import de.peekandpoke.ultra.meta.expectFileToMatch
import de.peekandpoke.ultra.mutator.Mutable
import de.peekandpoke.ultra.mutator.meta.MutatorAnnotationProcessor
import io.kotlintest.specs.StringSpec

class DataClassesImplementingSameInterfaceSpec : StringSpec({

    "Compiling data classes that implement that same interface" {

        compileTest {

            processor(MutatorAnnotationProcessor())

            kotlin(
                "KTest.kt",
                """
                    package mutator.compile
                    
                    import ${Mutable::class.qualifiedName}
                    
                    interface KRoot {
                        val root: Int
                    }
                    
                    interface KBase: KRoot {
                        val base: String
                    }
                    
                    @Mutable
                    data class KChildOne(
                        override val root: Int,
                        override val base: String,
                        val myFieldOne: Number 
                    ) : KBase

                    @Mutable
                    data class KChildTwo(
                        override val root: Int,
                        override val base: String,
                        val myFieldTwo: Number 
                    ) : KBase

                """.trimIndent()
            )

            // NOTICE: interface DO NOT enclose any variables ... so the body is empty
            expectFileToMatch(
                "KRoot${"$$"}mutator.kt",
                """
                    @file:Suppress("UNUSED_ANONYMOUS_PARAMETER")

                    package mutator.compile

                    import de.peekandpoke.ultra.mutator.*


                    @JvmName("mutateKRootMutator")
                    fun KRoot.mutate(mutation: KRootMutator.() -> Unit) =
                        mutator({ x: KRoot -> Unit }).apply(mutation).getResult()

                    @JvmName("mutatorKRootMutator")
                    fun KRoot.mutator(onModify: OnModify<KRoot> = {}): KRootMutator = when (this) {
                        is KBase -> mutator(onModify as OnModify<KBase>)
                        else -> error("Unknown child type ${"$"}{this::class}")
                    }

                    interface KRootMutator {
                        fun getResult(): KRoot
                    }

                """.trimIndent()
            )

            // NOTICE: interface DO NOT enclose any variables ... so the body is empty
            expectFileToMatch(
                "KBase${"$$"}mutator.kt",
                """
                    @file:Suppress("UNUSED_ANONYMOUS_PARAMETER")

                    package mutator.compile

                    import de.peekandpoke.ultra.mutator.*


                    @JvmName("mutateKBaseMutator")
                    fun KBase.mutate(mutation: KBaseMutator.() -> Unit) =
                        mutator({ x: KBase -> Unit }).apply(mutation).getResult()

                    @JvmName("mutatorKBaseMutator")
                    fun KBase.mutator(onModify: OnModify<KBase> = {}): KBaseMutator = when (this) {
                        is KChildOne -> mutator(onModify as OnModify<KChildOne>)
                        is KChildTwo -> mutator(onModify as OnModify<KChildTwo>)
                        else -> error("Unknown child type ${"$"}{this::class}")
                    }

                    interface KBaseMutator : KRootMutator {
                        fun getResult(): KBase
                    }

                """.trimIndent()
            )

            expectFileToMatch(
                "KChildOne${"$$"}mutator.kt",
                """
                    @file:Suppress("UNUSED_ANONYMOUS_PARAMETER")
                    
                    package mutator.compile
                    
                    import de.peekandpoke.ultra.mutator.*
                    
                    
                    @JvmName("mutateKChildOneMutator")
                    fun KChildOne.mutate(mutation: KChildOneMutator.() -> Unit) = 
                        mutator({ x: KChildOne -> Unit }).apply(mutation).getResult()
                    
                    @JvmName("mutatorKChildOneMutator")
                    fun KChildOne.mutator(onModify: OnModify<KChildOne> = {}) = 
                        KChildOneMutator(this, onModify)
                    
                    class KChildOneMutator(
                        target: KChildOne, 
                        onModify: OnModify<KChildOne> = {}
                    ) : DataClassMutator<KChildOne>(target, onModify), KBaseMutator  {
                    
                        /**
                         * Mutator for field [KChildOne.root]
                         *
                         * Info:
                         *   - type:         [Int]
                         *   - reflected by: [com.squareup.kotlinpoet.ClassName]
                         */ 
                        var root
                            get() = getResult().root
                            set(v) = modify(getResult()::root, getResult().root, v)
                    
                        /**
                         * Mutator for field [KChildOne.base]
                         *
                         * Info:
                         *   - type:         [String]
                         *   - reflected by: [com.squareup.kotlinpoet.ClassName]
                         */ 
                        var base
                            get() = getResult().base
                            set(v) = modify(getResult()::base, getResult().base, v)
                    
                        /**
                         * Mutator for field [KChildOne.myFieldOne]
                         *
                         * Info:
                         *   - type:         [Number]
                         *   - reflected by: [com.squareup.kotlinpoet.ClassName]
                         */ 
                        var myFieldOne
                            get() = getResult().myFieldOne
                            set(v) = modify(getResult()::myFieldOne, getResult().myFieldOne, v)
                    
                    }

                """.trimIndent()
            )

            expectFileToMatch(
                "KChildTwo${"$$"}mutator.kt",
                """
                    @file:Suppress("UNUSED_ANONYMOUS_PARAMETER")
                    
                    package mutator.compile
                    
                    import de.peekandpoke.ultra.mutator.*
                    
                    
                    @JvmName("mutateKChildTwoMutator")
                    fun KChildTwo.mutate(mutation: KChildTwoMutator.() -> Unit) = 
                        mutator({ x: KChildTwo -> Unit }).apply(mutation).getResult()
                    
                    @JvmName("mutatorKChildTwoMutator")
                    fun KChildTwo.mutator(onModify: OnModify<KChildTwo> = {}) = 
                        KChildTwoMutator(this, onModify)
                    
                    class KChildTwoMutator(
                        target: KChildTwo, 
                        onModify: OnModify<KChildTwo> = {}
                    ) : DataClassMutator<KChildTwo>(target, onModify), KBaseMutator  {
                    
                        /**
                         * Mutator for field [KChildTwo.root]
                         *
                         * Info:
                         *   - type:         [Int]
                         *   - reflected by: [com.squareup.kotlinpoet.ClassName]
                         */ 
                        var root
                            get() = getResult().root
                            set(v) = modify(getResult()::root, getResult().root, v)
                    
                        /**
                         * Mutator for field [KChildTwo.base]
                         *
                         * Info:
                         *   - type:         [String]
                         *   - reflected by: [com.squareup.kotlinpoet.ClassName]
                         */ 
                        var base
                            get() = getResult().base
                            set(v) = modify(getResult()::base, getResult().base, v)
                    
                        /**
                         * Mutator for field [KChildTwo.myFieldTwo]
                         *
                         * Info:
                         *   - type:         [Number]
                         *   - reflected by: [com.squareup.kotlinpoet.ClassName]
                         */ 
                        var myFieldTwo
                            get() = getResult().myFieldTwo
                            set(v) = modify(getResult()::myFieldTwo, getResult().myFieldTwo, v)
                    
                    }

                """.trimIndent()
            )
        }
    }
})
