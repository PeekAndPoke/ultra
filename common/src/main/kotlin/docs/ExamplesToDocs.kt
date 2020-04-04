package de.peekandpoke.ultra.common.docs

import de.peekandpoke.ultra.common.ensureDirectory
import java.io.File

class ExamplesToDocs(
    private val chapters: List<ExampleChapter>,
    private val sourceLocation: File = File("src/examples"),
    private val outputLocation: File = File("docs/ultra::docs")
) {

    init {
        outputLocation.ensureDirectory()
    }

    fun run() {

        val builder = StringBuilder()

        builder.appendln("# Examples").appendln()

        chapters.forEach { group ->

            builder.appendln("## ${group.name}").appendln()

            val srcDir = File(sourceLocation, group.packageLocation)

            group.examples.forEach { example ->

                val exampleCode = ExampleCodeExtractor.extract(example, srcDir)

                builder
                    .appendln("### ${example.title}")
                    .appendln("```kotlin")
                    .appendln(exampleCode ?: "no code available")
                    .appendln("```")

                val output = example.runAndRecordOutput()

                if (output.isNotEmpty()) {
                    builder
                        .appendln("Will output:")
                        .appendln("```")
                        .appendln(output.trim())
                        .appendln("```")
                        .appendln()
                }
            }
        }

        File(outputLocation, "index.md").apply {
            writeText(builder.toString())
        }
    }
}
