package de.peekandpoke.ultra.mutator.compile

import de.peekandpoke.ultra.meta.testing.expectFileCount
import de.peekandpoke.ultra.meta.testing.expectFileToMatch
import de.peekandpoke.ultra.meta.testing.expectFileToNotExist
import de.peekandpoke.ultra.meta.testing.kaptCompileTest
import de.peekandpoke.ultra.mutator.Mutable
import de.peekandpoke.ultra.mutator.NotMutable
import de.peekandpoke.ultra.mutator.meta.MutatorAnnotationProcessor
import io.kotest.core.spec.style.StringSpec
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi

@OptIn(ExperimentalCompilerApi::class)
class ClassWithNotMutableAnnotationSpec : StringSpec({

    "Compiling a data class with passed through generic parameter" {

        kaptCompileTest {

            processor(MutatorAnnotationProcessor())

            kotlin(
                "KTest.kt",
                """
                    package mutator.compile
                    
                    import ${Mutable::class.qualifiedName}
                    import ${NotMutable::class.qualifiedName}
                    
                    @NotMutable
                    data class Immutable(val value: Int)
                    
                    @NotMutable
                    data class ImmutableGeneric<T>(val value: T)
                    
                    @Mutable
                    data class Owner(
                        val num: Int,
                        val notMutable: Immutable,
                        val notMutableGeneric: ImmutableGeneric<String>,
                    )

                """.trimIndent()
            )

            expectFileCount(1)

            expectFileToMatch(
                "Owner${"$$"}mutator.kt",
                """
                    @file:Suppress("UNUSED_ANONYMOUS_PARAMETER")
                    
                    package mutator.compile
                    
                    import de.peekandpoke.ultra.mutator.*
                    
                    
                    @JvmName("mutateOwnerMutator")
                    fun Owner.mutate(mutation: OwnerMutator.() -> Unit) = 
                        mutator({ x: Owner -> Unit }).apply(mutation).getResult()
                    
                    @JvmName("mutatorOwnerMutator")
                    fun Owner.mutator(onModify: OnModify<Owner> = {}) = 
                        OwnerMutator(this, onModify)
                    
                    class OwnerMutator(
                        target: Owner, 
                        onModify: OnModify<Owner> = {}
                    ) : DataClassMutator<Owner>(target, onModify) {
                    
                        /**
                         * Mutator for field [Owner.num]
                         *
                         * Info:
                         *   - type:         [Int]
                         *   - reflected by: [com.squareup.kotlinpoet.ClassName]
                         */ 
                        var num
                            get() = getResult().num
                            set(v) = modify(getResult()::num, getResult().num, v)
                    
                        /**
                         * Mutator for field [Owner.notMutable]
                         *
                         * Info:
                         *   - type:         [Immutable]
                         *   - reflected by: [com.squareup.kotlinpoet.ClassName]
                         */ 
                        var notMutable
                            get() = getResult().notMutable
                            set(v) = modify(getResult()::notMutable, getResult().notMutable, v)
 
                        /**
                         * Mutator for field [Owner.notMutableGeneric]
                         *
                         * Info:
                         *   - type:         [ImmutableGeneric<String>]
                         *   - reflected by: [com.squareup.kotlinpoet.ParameterizedTypeName]
                         */ 
                        var notMutableGeneric
                            get() = getResult().notMutableGeneric
                            set(v) = modify(getResult()::notMutableGeneric, getResult().notMutableGeneric, v)

                    }                    
                """.trimIndent()
            )

            expectFileToNotExist(
                "Immutable${"$$"}mutator.kt"
            )
        }
    }
})
