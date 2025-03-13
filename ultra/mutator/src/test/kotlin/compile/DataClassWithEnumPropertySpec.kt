package de.peekandpoke.ultra.mutator.compile

import de.peekandpoke.ultra.meta.testing.expectFileToMatch
import de.peekandpoke.ultra.meta.testing.kaptCompileTest
import de.peekandpoke.ultra.mutator.Mutable
import de.peekandpoke.ultra.mutator.meta.MutatorAnnotationProcessor
import io.kotest.core.spec.style.StringSpec
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi

@OptIn(ExperimentalCompilerApi::class)
class DataClassWithEnumPropertySpec : StringSpec({

    "Compiling a data class with one enum property" {

        kaptCompileTest {

            processor(MutatorAnnotationProcessor())

            kotlin(
                "KTest.kt",
                """
                    package mutator.compile
                    
                    import ${Mutable::class.qualifiedName}
                    
                    enum class MyEnum {
                        First,
                        Second
                    }
                    
                    @Mutable
                    data class WithEnum(val value: MyEnum)

                """.trimIndent()
            )

            expectFileToMatch(
                "WithEnum${"$$"}mutator.kt",
                """
                    @file:Suppress("UNUSED_ANONYMOUS_PARAMETER")
                    
                    package mutator.compile
                    
                    import de.peekandpoke.ultra.mutator.*
                    
                    
                    @JvmName("mutateWithEnumMutator")
                    fun WithEnum.mutate(mutation: WithEnumMutator.() -> Unit) = 
                        mutator({ x: WithEnum -> Unit }).apply(mutation).getResult()
                    
                    @JvmName("mutatorWithEnumMutator")
                    fun WithEnum.mutator(onModify: OnModify<WithEnum> = {}) = 
                        WithEnumMutator(this, onModify)
                    
                    class WithEnumMutator(
                        target: WithEnum, 
                        onModify: OnModify<WithEnum> = {}
                    ) : DataClassMutator<WithEnum>(target, onModify) {
                    
                        /**
                         * Mutator for field [WithEnum.value]
                         *
                         * Info:
                         *   - type:         [MyEnum]
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
