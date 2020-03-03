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
                    fun KRoot.mutator(onModify: OnModify<KRoot> = {}): KRootMutator = when(this) {
                        is KBase -> (this as KBase).mutator(onModify)
                        else -> error("Unknown child type ${"$"}{this::class}")
                    }

                    interface KRootMutator : Mutator<KRoot>

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
                    fun KBase.mutator(onModify: OnModify<KBase> = {}): KBaseMutator = when(this) {
                        is KChildOne -> (this as KChildOne).mutator(onModify)
                        is KChildTwo -> (this as KChildTwo).mutator(onModify)
                        else -> error("Unknown child type ${"$"}{this::class}")
                    }

                    interface KBaseMutator : Mutator<KBase>, KRootMutator

                """.trimIndent()
            )

            expectFileToMatch(
                "KChildOne${"$$"}mutator.kt",
                """
                    @file:Suppress("UNUSED_ANONYMOUS_PARAMETER")

                    package mutator.compile

                    import de.peekandpoke.ultra.mutator.*


                    @JvmName("mutateKBaseMutator")
                    fun KBase.mutate(mutation: KBaseMutator.() -> Unit) =
                        mutator({ x: KBase -> Unit }).apply(mutation).getResult()

                    @JvmName("mutatorKBaseMutator")
                    fun KBase.mutator(onModify: OnModify<KBase> = {}): KBaseMutator = when(this) {
                        is KChildOne -> (this as KChildOne).mutator(onModify)
                        is KChildTwo -> (this as KChildTwo).mutator(onModify)
                        else -> error("Unknown child type ${"$"}{this::class}")
                    }

                    interface KBaseMutator : Mutator<KBase>, KRootMutator

                """.trimIndent()
            )
        }
    }
})
