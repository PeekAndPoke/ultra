@file:OptIn(ExperimentalCompilerApi::class)

package de.peekandpoke.ultra.meta.testing

import com.github.difflib.DiffUtils
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.tschuchort.compiletesting.CompilationResult
import com.tschuchort.compiletesting.JvmCompilationResult
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.configureKsp
import com.tschuchort.compiletesting.symbolProcessorProviders
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import java.io.File

/**
 * Compile testing DSL entry point
 */
fun kspCompileTest(builder: KspCompileTest.Builder.() -> Unit): CompilationResult {
    return KspCompileTest.Builder().apply(builder).build().run()
}

/**
 * Adds an expectation that matches the contents of a [file] with the [contents]
 *
 * @see [KspCompileTest.ExpectFileToMatch]
 */
fun KspCompileTest.Builder.expectFileToMatch(file: String, contents: String) =
    expect(KspCompileTest.ExpectFileToMatch(file, contents))

/**
 * Adds an expectation that checks that the given file was NOT created
 */
fun KspCompileTest.Builder.expectFileToNotExist(file: String) =
    expect(KspCompileTest.ExpectFileToNotExist(file))

/**
 * Adds an expectation that checks for the exact number of files to be created
 */
fun KspCompileTest.Builder.expectFileCount(count: Int) =
    expect(KspCompileTest.ExpectFileCount(count))


fun CompilationResult.getGeneratedSources(): List<File> {
    return when (this) {
        is JvmCompilationResult -> File(outputDirectory, "../ksp/sources")
            .canonicalFile.walkTopDown()
            .filter { it.isFile }.toList()

        else -> emptyList()
    }
}

/**
 * The compile test runner.
 *
 * Compiles all [sourcesFiles] with the given [processors] (Annotation processors).
 * Then checks all expectations.
 *
 * When there are any errors an [IllegalStateException] is raised with text containing detailed information
 * about all failed expectations.
 *
 * @see kspCompileTest
 * @see Builder
 */
class KspCompileTest internal constructor(
    /** annotations processors to be used for compilation */
    val processors: List<SymbolProcessorProvider>,
    /** source files to be compiled */
    val sourcesFiles: List<SourceFile>,
    /** expectations to be checked */
    val expectations: List<Expectation>,
    /** Options */
    val options: Options,
) {
    data class Options(
        val jvmTargetVersion: String = "17",
        val inheritClassPath: Boolean,
    )

    /**
     * Compiles and checks expectations
     */
    fun run(): CompilationResult {

        // compile all sources
        val compilation: KotlinCompilation = KotlinCompilation().apply {
            configureKsp {}

            sources = sourcesFiles
            symbolProcessorProviders.addAll(processors)
            inheritClassPath = options.inheritClassPath

            kotlincArguments = kotlincArguments + listOf("-jvm-target", options.jvmTargetVersion)

//            workingDir = File("tmp/Kotlin-Compilation/${LocalDateTime.now()}").absoluteFile.ensureDirectory()
        }

        val compiled: JvmCompilationResult = compilation.compile()

        // check expectations
        val errors = expectations.flatMap { it.apply(compiled) }

        // errors?
        if (errors.isNotEmpty()) {
            error(
                errors.joinToString("\n")
            )
        }

        return compiled
    }

    /**
     * Builder for creating a [KspCompileTest]
     */
    class Builder internal constructor() {
        /** source files */
        private val sourceFiles = mutableListOf<SourceFile>()

        /** annotation processors */
        private val processors = mutableListOf<SymbolProcessorProvider>()

        /** expectations */
        private val expectations = mutableListOf<Expectation>()

        /** options */
        private var options = Options(
            inheritClassPath = false,
        )

        /**
         * Creates the CompileTest
         */
        internal fun build() = KspCompileTest(
            sourcesFiles = sourceFiles,
            processors = processors,
            expectations = expectations,
            options = options,
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
        fun kotlin(file: String, contents: String) =
            source(SourceFile.kotlin(file, contents))

        /**
         * Adds annotation processors
         */
        fun processor(vararg processor: SymbolProcessorProvider) = apply {
            processors.addAll(processor)
        }

        /**
         * Adds expectations
         */
        fun expect(vararg expectation: Expectation) = apply {
            expectations.addAll(expectation)
        }

        /**
         * Setting this to true will make the KotlinCompilation inherit the classpath of the calling process
         */
        fun inheritClassPath(value: Boolean) = apply {
            options = options.copy(
                inheritClassPath = value,
            )
        }

        fun jvmTargetVersion(value: String) = apply {
            options = options.copy(
                jvmTargetVersion = value,
            )
        }
    }

    /**
     * Defines an expectation to be checked by a [KspCompileTest]
     */
    interface Expectation {
        /**
         * Returns a list of error string or empty when all is well.
         */
        fun apply(result: CompilationResult): List<String>
    }

    /**
     * Checks if the result has a generated [file] which matches the [contents]
     *
     * The content are not compared for pure equality.
     *
     * In order to make tests more stable the sources strings are preprocessed:
     * - line endings are trimmed
     */
    data class ExpectFileToMatch(val file: String, val contents: String) : Expectation {

        override fun apply(result: CompilationResult): List<String> {

            val sources = result.getGeneratedSources()

            val output = sources.firstOrNull { it.name == file }
                ?: return listOf(
                    "The file '$file' has not been generated"
                )

            val content = String(output.readBytes())

            val diff = DiffUtils.diff(
                contents.trimLineEnds().lines().dropLastWhile { it.isBlank() },
                content.trimLineEnds().lines().dropLastWhile { it.isBlank() },
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

                    """.trimIndent() + contents + """
                        
                        Actual:
                        =======
                        
                    """.trimIndent() + content + """
                        
                        Deltas:
                        
                    """.trimIndent() + diff.deltas.joinToString("\n") + """
                        
                        
                        File: 
                        $output
                         
                    """.trimIndent()
                )
            }
        }
    }

    /**
     * Expectation that checks that the given [file] does NOT exist.
     */
    data class ExpectFileToNotExist(val file: String) : Expectation {

        override fun apply(result: CompilationResult): List<String> {

            val sources = result.getGeneratedSources()

            return sources
                .filter { it.name == file }
                .map { "The file '$file' should NOT be created." }
        }
    }

    /**
     * Expectation that checks for the exact number of files to be created
     */
    data class ExpectFileCount(val count: Int) : Expectation {

        override fun apply(result: CompilationResult): List<String> {
            val sources = result.getGeneratedSources()

            val actual = sources.size

            if (actual != count) {
                return listOf(
                    "Expected $count files to be created. But actually $actual files where created:",
                ).plus(
                    sources.mapIndexed { index, file -> "${index + 1}. ${file.name}" }
                )
            }

            return emptyList()
        }
    }
}

/**
 * Normalizes code by trimming line ends.
 *
 * This is handy for have a more relaxed comparison of code blocks.
 */
private fun String.trimLineEnds(delimiter: String = "\n") = split(delimiter).joinToString(delimiter) { it.trimEnd() }
