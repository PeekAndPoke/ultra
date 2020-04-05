package de.peekandpoke.ultra.common.docs

import de.peekandpoke.ultra.common.ensureDirectory
import de.peekandpoke.ultra.common.toUri
import java.io.File

fun examplesToDocs(
    title: String,
    chapters: List<ExampleChapter>,
    sourceLocation: File = File("src/examples"),
    outputLocation: File = File("docs/ultra::docs")
) {
    ExamplesToDocs(
        title = title,
        chapters = chapters,
        sourceLocation = sourceLocation,
        outputLocation = outputLocation
    ).run()
}

class ExamplesToDocs internal constructor(
    private val title: String,
    private val chapters: List<ExampleChapter>,
    private val sourceLocation: File,
    private val outputLocation: File
) {

    private val builder = StringBuilder()

    init {
        outputLocation.deleteRecursively()
        outputLocation.ensureDirectory()
    }

    fun run() {

        builder.appendln("# $title").appendln()

        generateToc()

        generateExamples()

        File(outputLocation, "index.md").apply {
            writeText(builder.toString())
        }
    }

    /**
     * Generates the table of contents
     */
    private fun generateToc() {

        builder.appendln("## TOC").appendln()

        chapters.forEachIndexed { chapterIndex, chapter ->

            chapter.title.let {
                builder.appendln("${chapterIndex + 1}. [${it}](#${it.toAnchor()})").appendln()
            }

            chapter.examples.forEachIndexed { exampleIndex, example ->

                example.title.let {
                    builder.appendln("    ${exampleIndex + 1}. [${it}](#${it.toAnchor()})")
                }
            }
        }

        builder.appendln()
    }

    /**
     * Generates the docs for all examples
     */
    private fun generateExamples() {

        chapters.forEach { chapter ->

            builder.appendln("## ${chapter.title}").appendln()

            val srcDir = File(sourceLocation, chapter.packageLocation)

            chapter.examples.forEach { example ->

                val exampleCode = ExampleCodeExtractor.extract(example, srcDir)

                builder.appendln("### ${example.title}").appendln()
                    .appendln(example.description).appendln()

                val codeLocation = File(srcDir.relativeTo(outputLocation), "${example::class.simpleName}.kt")

                builder.appendln("@see the [runnable example]($codeLocation)").appendln()

                builder.appendKotlinCode(exampleCode ?: "no code available")

                val output = example.runAndRecordOutput()

                if (output.isNotEmpty()) {
                    builder.appendln("Will output:").appendPlainCode(output.trim())
                }
            }
        }
    }

    private fun String.toAnchor() = toLowerCase().toUri().replace(" ", "-")

    private fun StringBuilder.appendKotlinCode(code: String) = apply {
        appendln("```kotlin")
        appendln(code)
        appendln("```")
    }

    private fun StringBuilder.appendPlainCode(code: String) = apply {
        appendln("```")
        appendln(code)
        appendln("```")
    }
}
