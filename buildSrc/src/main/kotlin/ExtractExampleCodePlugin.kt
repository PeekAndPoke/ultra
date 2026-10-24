import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import java.io.File

class ExtractExampleCodePlugin : Plugin<Project> {

    private data class Entry(
        val file: File,
        val prefix: String,
        val name: String,
        val content: String,
    )

    override fun apply(project: Project) {

//        val ktFiles = FileUtils.listFiles(
//            target.projectDir,
//            new RegexFileFilter("^(.*?)\.kt"),
//            DirectoryFileFilter.DIRECTORY
//        );

        val sourcesDir = "/src/main/kotlin"
        // Under the build directory, so it follows a per-user buildRoot (MC-00031)
        val outputDir = project.layout.buildDirectory.dir("generated/extracted-code-blocks").get().asFile
        val outputFileName = "extracted-code-blocks.kt"

        project.extensions.configure<KotlinProjectExtension>() {
            val mainSet = listOfNotNull(
                sourceSets.findByName("main"),
                sourceSets.findByName("commonMain"),
                sourceSets.findByName("jsMain"),
                sourceSets.findByName("jvmMain")
            ).firstOrNull()

            val sets = listOfNotNull(mainSet)

            sets.forEach {
                it.kotlin.srcDir(outputDir)
            }
        }

        project.tasks.create("extractCodeBlocks") {
            implementExtractCodeBlocks(
                sourcesDir = sourcesDir,
                outputDir = outputDir,
                outputFileName = outputFileName,
            )
        }

        val tasksThatNeedCode = listOf(
            "mainClasses", "testClasses", "compileKotlinJs",
        )

        tasksThatNeedCode.forEach { name ->
            project.tasks.findByName(name)?.let { task ->
                task.dependsOn(project.tasks.getByName("extractCodeBlocks"))
            }
        }
    }

    private fun Task.implementExtractCodeBlocks(
        sourcesDir: String,
        outputDir: File,
        outputFileName: String,
    ) {
        group = "extractCodeBlocks"

        val projectDir = project.projectDir.absoluteFile
        // Sources only: skip build output (the IDE's build/, any per-user build root) and node_modules
        val ktFiles = projectDir.walkTopDown()
            .onEnter { it.name != "build" && !it.name.startsWith(".build-") && it.name != "node_modules" }
            .filter { it.isFile && it.extension == "kt" }

        doFirst {

            val startExampleRegex = "<CodeBlock ([^>]+)>".toRegex()
            val alphaNumRegex = "[^a-zA-Z0-9]+".toRegex()

            val entries = ktFiles.flatMap { file ->

                val entryPrefix = file.absoluteFile.toString()
                    .replaceFirst(projectDir.absolutePath, "")
                    .replaceFirst("src/[^/]*/kotlin/".toRegex(), "")
                    .replaceFirst(sourcesDir, "")
                    .replace(alphaNumRegex, "_")
                    .trim('_')

                val fileContent = file.readText()
                val matches = startExampleRegex.findAll(fileContent)

//            println("found file: ${file.absolutePath}")

                matches.mapNotNull { match ->
                    val name = match.groupValues[1].trim().replace(alphaNumRegex, "_")
                    val startIdx = match.range.last + 1
                    val endIdx = fileContent.indexOf("</CodeBlock>", startIdx) - 3

                    if (endIdx >= 0) {
                        val content = fileContent.substring(startIdx, endIdx)
                            .trimIndent()
                            .lines()
                            .dropWhile { it.isBlank() }
                            .dropLastWhile { it.isBlank() }
                            .joinToString("\n")

                        Entry(file = file, prefix = entryPrefix, name = name, content = content)
                    } else {
                        null
                    }
                }
            }

//        entries.forEach { entry ->
//            println("==== Found entry ${entry.prefix} ${entry.name} ================================")
//            println(entry.content)
//        }

            val examplesCodeFile = StringBuilder().apply {

                appendLine("@file:Suppress(\"unused\", \"PackageDirectoryMismatch\")")
                appendLine("package generated")
                appendLine()

                appendLine("object ExtractedCodeBlocks {")

                entries.forEach { entry ->
                    appendLine("    const val ${entry.prefix}_${entry.name} = \"\"\"")
                    appendLine(
                        entry.content
                            // escape multiple quotes
                            .replace("\"\"\"", "\\\"\\\"\\\"")
                            // escape $ signs
                            .replace("$", "${"$"}{\"$\"}")
                    )
                    appendLine("\"\"\"")
                    appendLine()
                }

                appendLine("}")
            }

            val exampleFileContent = examplesCodeFile.toString()

            val targetDir = outputDir.also {
                if (!it.exists()) {
                    it.mkdirs()
                }
            }

            val targetFile = File(targetDir, outputFileName)

            targetFile.writeText(exampleFileContent)
        }
    }
}
