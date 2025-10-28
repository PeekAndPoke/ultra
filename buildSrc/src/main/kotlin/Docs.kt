@file:Suppress("detekt:all")

import org.gradle.api.Project
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Sync
import org.gradle.kotlin.dsl.provideDelegate
import org.gradle.kotlin.dsl.register
import java.io.File

object Docs {

    operator fun invoke(block: Docs.() -> Unit) {
        this.block()
    }

    fun Project.useEmptyJavadoc() {

        @Suppress("LocalVariableName")
        val VERSION_NAME: String by project

        tasks.findByName("dokkaHtml")?.let {
            it.enabled = false
        }

        tasks.register<Copy>("useEmptyJavadoc") {
            val fromDir = rootProject.projectDir
            val buildDir = layout.buildDirectory.get().asFile
            val intoDir = File(buildDir, "/libs")
            val targetFile = "${project.name}-$VERSION_NAME-javadoc.jar"

//            println("useEmptyJavadoc")
//            println("from: $fromDir")
//            println("into: $intoDir")
//            println("targetFile: $targetFile")

            from(fromDir)
            into(intoDir)

            include("empty-javadoc.jar")
            rename("empty-javadoc.jar", targetFile)
        }
    }

    fun Project.distributeJsExample(dir: String) {

        val taskName = "distributeJsExample"

        tasks.named("build") { finalizedBy(taskName) }

        tasks.register<Sync>(taskName) {
            dependsOn("assemble")
            dependsOn("jsBrowserProductionWebpack")

            val buildDir = layout.buildDirectory.get().asFile
            val fromDir = File(buildDir, "dist/js/productionExecutable")
            val intoDir = File(rootProject.projectDir, "${dir.trimEnd('/')}/${project.name}")

            println("== distributeJsExample '${project.name}' ===========================================")
            println("from: $fromDir")
            println("into: $intoDir")

            from(fromDir)
            into(intoDir)
        }
    }
}
