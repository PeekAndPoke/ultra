package de.peekandpoke.ultra.mutator.compile

import de.peekandpoke.ultra.meta.compileTest
import de.peekandpoke.ultra.meta.expectFileToMatch
import de.peekandpoke.ultra.mutator.Mutable
import de.peekandpoke.ultra.mutator.meta.MutatorAnnotationProcessor
import io.kotest.core.spec.style.StringSpec

class DataClassImplementingMultipleInterfacesSpec : StringSpec({

    "Compiling a data classes that implements multiple interfaces" {

        compileTest {

            processor(MutatorAnnotationProcessor())

            kotlin(
                "KTest.kt",
                """
                    package mutator.compile
                    
                    import ${Mutable::class.qualifiedName}
                    
                    interface KRoot1 {
                        val root1: Int?
                    }

                    interface KRoot2 {
                        val root2: Int?
                    }

                    @Mutable
                    data class KChild(
                        override val root1: Int?,
                        override val root2: Int?
                    ) : KRoot1, KRoot2

                """.trimIndent()
            )

            // NOTICE: interface DO NOT enclose any variables ... so the body is empty
            expectFileToMatch(
                "KRoot1${"$$"}mutator.kt",
                """
                    @file:Suppress("UNUSED_ANONYMOUS_PARAMETER")

                    package mutator.compile

                    import de.peekandpoke.ultra.mutator.*


                    @JvmName("mutateKRoot1Mutator")
                    fun KRoot1.mutate(mutation: KRoot1Mutator.() -> Unit) =
                        mutator({ x: KRoot1 -> Unit }).apply(mutation).getResult()

                    @JvmName("mutatorKRoot1Mutator")
                    fun KRoot1.mutator(onModify: OnModify<KRoot1> = {}): KRoot1Mutator = when (this) {
                        is KChild -> mutator(onModify as OnModify<KChild>)
                        else -> error("Unknown child type ${"$"}{this::class}")
                    }

                    interface KRoot1Mutator {
                        fun getResult(): KRoot1
                    }

                """.trimIndent()
            )

            // NOTICE: interface DO NOT enclose any variables ... so the body is empty
            expectFileToMatch(
                "KRoot2${"$$"}mutator.kt",
                """
                    @file:Suppress("UNUSED_ANONYMOUS_PARAMETER")

                    package mutator.compile

                    import de.peekandpoke.ultra.mutator.*


                    @JvmName("mutateKRoot2Mutator")
                    fun KRoot2.mutate(mutation: KRoot2Mutator.() -> Unit) =
                        mutator({ x: KRoot2 -> Unit }).apply(mutation).getResult()

                    @JvmName("mutatorKRoot2Mutator")
                    fun KRoot2.mutator(onModify: OnModify<KRoot2> = {}): KRoot2Mutator = when (this) {
                        is KChild -> mutator(onModify as OnModify<KChild>)
                        else -> error("Unknown child type ${"$"}{this::class}")
                    }

                    interface KRoot2Mutator {
                        fun getResult(): KRoot2
                    }

                """.trimIndent()
            )

            expectFileToMatch(
                "KChild${"$$"}mutator.kt",
                """
                    @file:Suppress("UNUSED_ANONYMOUS_PARAMETER")
                    
                    package mutator.compile
                    
                    import de.peekandpoke.ultra.mutator.*
                    
                    
                    @JvmName("mutateKChildMutator")
                    fun KChild.mutate(mutation: KChildMutator.() -> Unit) = 
                        mutator({ x: KChild -> Unit }).apply(mutation).getResult()
                    
                    @JvmName("mutatorKChildMutator")
                    fun KChild.mutator(onModify: OnModify<KChild> = {}) = 
                        KChildMutator(this, onModify)
                    
                    class KChildMutator(
                        target: KChild, 
                        onModify: OnModify<KChild> = {}
                    ) : DataClassMutator<KChild>(target, onModify), KRoot1Mutator, KRoot2Mutator  {
                    
                        /**
                         * Mutator for field [KChild.root1]
                         *
                         * Info:
                         *   - type:         [Int]
                         *   - reflected by: [com.squareup.kotlinpoet.ClassName]
                         */ 
                        var root1
                            get() = getResult().root1
                            set(v) = modify(getResult()::root1, getResult().root1, v)
                    
                        /**
                         * Mutator for field [KChild.root2]
                         *
                         * Info:
                         *   - type:         [Int]
                         *   - reflected by: [com.squareup.kotlinpoet.ClassName]
                         */ 
                        var root2
                            get() = getResult().root2
                            set(v) = modify(getResult()::root2, getResult().root2, v) 
                    
                    }

                """.trimIndent()
            )
        }
    }
})
