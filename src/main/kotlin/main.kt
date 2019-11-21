import de.peekandpoke.ultra.meta.compileTest
import de.peekandpoke.ultra.mutator.Mutable
import de.peekandpoke.ultra.mutator.meta.MutatorAnnotationProcessor

fun main() {
    val result = compileTest {
        processor(MutatorAnnotationProcessor())

        kotlin(
            "source.kt",
            """
                
                package xyz
                
                import ${Mutable::class.qualifiedName}
                
                @Mutable
                data class GenericTypeInAction(
                    val generic: Generic<Type>,
                    val genInt: Generic<Int>
                )

                data class Generic<T>(val value: T)
                
                data class Type(val value: String)
                
            """.trimIndent()
        )
    }

    result.sourcesGeneratedByAnnotationProcessor.forEach {

        println(it.absoluteFile)
        println("---------------------------------------------------------------------")
        println(String(it.readBytes()))

        println("\n\n")
    }
}

