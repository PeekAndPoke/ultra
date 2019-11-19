package de.peekandpoke.ultra.meta

import com.github.difflib.DiffUtils
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import de.peekandpoke.ultra.meta.CompileTest.Builder
import javax.annotation.processing.Processor

/**
 * Compile testing DSL entry point
 */
fun compileTest(builder: Builder.() -> Unit) = Builder().apply(builder).build().run()

/**
 * Adds an expectation that matches the contents of a [file] with the [expectedContent]
 *
 * @see [CompileTest.ExpectFileToMatch]
 */
fun Builder.expectFileToMatch(file: String, expectedContent: String) =
    expect(CompileTest.ExpectFileToMatch(file, expectedContent))

/**
 * The compile test runner.
 *
 * Compiles all [sourcesFiles] with the given [processors] (Annotation processors).
 * Then checks all expectations.
 *
 * When there are any errors an [IllegalStateException] is raised with text containing detailed information
 * about all failed expectations.
 *
 * @see compileTest
 * @see Builder
 */
data class CompileTest internal constructor(
    /** annotations processors to be used for compilation */
    val processors: List<Processor>,
    /** source files to be compiled */
    val sourcesFiles: List<SourceFile>,
    /** expectations to be checked */
    val expectations: List<Expectation>
) {
    /**
     * Compiles and checks expectations
     */
    fun run() {

        // compile all sources
        val compiled: KotlinCompilation.Result = KotlinCompilation().apply {
            sources = sourcesFiles
            annotationProcessors = processors
            inheritClassPath = true
        }.compile()

        // check expectations
        val errors = expectations.flatMap { it.apply(compiled) }

        // errors?
        if (errors.isNotEmpty()) {
            error(
                errors.joinToString("\n")
            )
        }
    }

    /**
     * Builder for creating a [CompileTest]
     */
    class Builder internal constructor() {
        /** source files */
        private val sourceFiles = mutableListOf<SourceFile>()
        /** annotation processors */
        private val processors = mutableListOf<Processor>()
        /** expectations */
        private val expectations = mutableListOf<Expectation>()

        /**
         * Creates the CompileTest
         */
        internal fun build() = CompileTest(
            sourcesFiles = sourceFiles,
            processors = processors,
            expectations = expectations
        )

        /**
         * Adds source files
         */
        fun source(vararg sourceFile: SourceFile) = apply {
            sourceFiles.addAll(sourceFile)
        }

        /**
         * Adds a kotlin source file
         */
        fun kotlin(filename: String, content: String) = source(SourceFile.kotlin(filename, content))

        /**
         * Adds annotation processors
         */
        fun processor(vararg processor: Processor) = apply {
            processors.addAll(processor)
        }

        /**
         * Adds expectations
         */
        fun expect(vararg expectation: Expectation) = apply {
            expectations.addAll(expectation)
        }
    }

    /**
     * Defines an expectation to be checked by a [CompileTest]
     */
    interface Expectation {
        /**
         * Returns a list of error string or empty when all is well.
         */
        fun apply(result: KotlinCompilation.Result): List<String>
    }

    /**
     * Checks if the result has a generated [file] which matches the [expectedContent]
     *
     * The content are not compared for pure equality.
     *
     * In order to make tests more stable the sources strings are preprocessed:
     * - line endings are trimmed
     */
    data class ExpectFileToMatch(val file: String, val expectedContent: String) : Expectation {

        override fun apply(result: KotlinCompilation.Result): List<String> {

            val output = result.sourcesGeneratedByAnnotationProcessor.firstOrNull { it.name == file }
                ?: return listOf(
                    "The file '$file' has not been generated"
                )

            val content = String(output.readBytes())

            val diff = DiffUtils.diff(
                expectedContent.trimLineEnds(),
                content.trimLineEnds()
            )

            return when {
                diff.deltas.isEmpty() -> listOf()
                else -> listOf(
                    """
                        Generated code in file 
                        $output
                        does not match expectations. The following deltas where found:
                        
                        
                    """.trimIndent() + diff.deltas.joinToString("\n") + """
                        
                        
                        Expected:
                        =========

                    """.trimIndent() + expectedContent + """
                        
                        Actual:
                        =======
                        
                    """.trimIndent() + content + """
                        
                        Deltas:
                        
                    """.trimIndent() + diff.deltas.joinToString("\n") + """
                        
                        
                        File: $output
                         
                    """.trimIndent()

                )
            }
        }
    }
}

/**
 * Normalizes code by trimming line ends.
 *
 * This is handy for have a more relaxed comparison of code blocks.
 */
private fun String.trimLineEnds(delimiter: String = "\n") = split(delimiter).joinToString(delimiter) { it.trimEnd() }
