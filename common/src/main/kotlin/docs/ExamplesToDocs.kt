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

        builder.appendLine("# $title").appendLine()

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

        builder.appendLine("## TOC").appendLine()

        chapters.forEachIndexed { chapterIndex, chapter ->

            chapter.title.let {
                builder.appendLine("${chapterIndex + 1}. [${it}](#${it.toAnchor()})").appendLine()
            }

            chapter.examples.forEachIndexed { exampleIndex, example ->

                example.title.let {
                    builder.appendLine("    ${exampleIndex + 1}. [${it}](#${it.toAnchor()})")
                }
            }

            builder.appendLine()
        }
    }

    /**
     * Generates the docs for all examples
     */
    private fun generateExamples() {

        chapters.forEach { chapter ->

            builder.appendLine("## ${chapter.title}").appendLine()

            if (chapter.description.isNotEmpty()) {
                builder.appendLine(chapter.description).appendLine()
            }

            chapter.examples.forEach { example ->

                // Getting the directory where the code for this example is in
                val srcDir = sourceLocation.child(this::class.getRelativePackagePath(example::class))
                // Extracting the code block from the given example
                val exampleCode = ExampleCodeExtractor.extract(example, srcDir)

                // Render the title
                builder.appendLine("### ${example.title}").appendLine()
                // Render the description
                builder.appendLine(example.description).appendLine()

                // Render the link to the code of the example and the example code
                val codeLocation = File(srcDir.relativeTo(outputLocation), "${example::class.simpleName}.kt")

                builder.appendLine("(see the full [example]($codeLocation))").appendLine()

                when {
                    exampleCode.isEmpty() -> builder.appendPlainCode("no code available")
                    else -> builder.appendKotlinCode(exampleCode)
                }

                // Runs the example and record all console output
                val output = example.runAndRecordOutput()

                // Render the console output
                if (output.isNotEmpty()) {
                    builder.appendLine("Will output:").appendPlainCode(output.trim()).appendLine()
                }

                // Render addition explanation if the is any
                if (example.additionalInfo.isNotEmpty()) {
                    builder.appendLine(example.additionalInfo).appendLine()
                }
            }
        }
    }

    private fun String.toAnchor() = toLowerCase().toUri()
        .replace(" ", "-")
        .replace(".", "")

    private fun StringBuilder.appendKotlinCode(code: String) = apply {
        appendLine("```kotlin")
        appendLine(code)
        appendLine("```")
    }

    private fun StringBuilder.appendPlainCode(code: String) = apply {
        appendLine("```")
        appendLine(code)
        appendLine("```")
    }
}
