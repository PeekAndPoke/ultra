package de.peekandpoke.ultra.common.docs

import de.peekandpoke.ultra.common.child
import de.peekandpoke.ultra.common.cleanDirectory
import de.peekandpoke.ultra.common.getRelativePackagePath
import de.peekandpoke.ultra.common.toUri
import java.io.File

//fun examplesToDocs(
//    title: String,
//    chapters: List<ExampleChapter>,
//    sourceLocation: File = File("src/examples"),
//    outputLocation: File = File("docs/ultra::docs")
//) {
//    ExamplesToDocs(
//        title = title,
//        chapters = chapters,
//        sourceLocation = sourceLocation,
//        outputLocation = outputLocation
//    ).run()
//}

abstract class ExamplesToDocs(
    private val title: String,
    private val chapters: List<ExampleChapter>,
    private val sourceLocation: File = File("src/examples"),
    private val outputLocation: File = File("docs/ultra::docs")
) {
    private val builder = StringBuilder()

    init {
        outputLocation.cleanDirectory()
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

            builder.appendln()
        }
    }

    /**
     * Generates the docs for all examples
     */
    private fun generateExamples() {

        chapters.forEach { chapter ->

            builder.appendln("## ${chapter.title}").appendln()

            val srcDir = sourceLocation.child(this::class.getRelativePackagePath(chapter::class))

            chapter.examples.forEach { example ->

                val exampleCode = ExampleCodeExtractor.extract(example, srcDir)

                builder.appendln("### ${example.title}").appendln()
                    .appendln(example.description).appendln()

                val codeLocation = File(srcDir.relativeTo(outputLocation), "${example::class.simpleName}.kt")

                builder.appendln("(see the full [example]($codeLocation))").appendln()

                when {
                    exampleCode.isEmpty() -> builder.appendPlainCode("no code available")
                    else -> builder.appendKotlinCode(exampleCode)
                }

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
