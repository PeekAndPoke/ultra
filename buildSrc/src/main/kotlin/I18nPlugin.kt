import io.peekandpoke.ultra.tooling.i18n.CheckFinding
import io.peekandpoke.ultra.tooling.i18n.CheckSeverity
import io.peekandpoke.ultra.tooling.i18n.I18nChecker
import io.peekandpoke.ultra.tooling.i18n.I18nGenConfig
import io.peekandpoke.ultra.tooling.i18n.KotlinEmitter
import io.peekandpoke.ultra.tooling.i18n.LocaleCatalog
import io.peekandpoke.ultra.tooling.i18n.YamlCatalogParser
import io.peekandpoke.ultra.tooling.i18n.checkOutcome
import io.peekandpoke.ultra.tooling.i18n.kotlinClassPart
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.register
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import java.io.File

/**
 * Configuration for the i18n code generator (see `.claude/tasks/20260720-i18n-l10n-foundation.md`).
 *
 * Yaml catalogs live at `src/<sourceSet>/i18n/messages.<locale>.yaml` by convention.
 */
abstract class I18nExtension {
    /** Package of the generated Kotlin files. Required. */
    abstract val packageName: Property<String>

    /** Unique module prefix for generated names, e.g. `KraftCore`. Default: project name PascalCased. */
    abstract val moduleName: Property<String>

    /** The fallback language — sole source of the generated API surface (D8). Default `en`. */
    abstract val fallbackLang: Property<String>

    /** Base languages that must be at full key-parity with the fallback (missing key/catalog = error). */
    abstract val requiredLangs: SetProperty<String>

    /** The Kotlin source set the yaml lives under and the generated code is registered on. */
    abstract val sourceSet: Property<String>

    fun requiredLangs(vararg langs: String) {
        requiredLangs.addAll(langs.toList())
    }
}

/** Reads all `messages.<locale>.yaml` and emits the baked catalog + typed accessors (S2 emitter). */
abstract class GenerateI18nTask : DefaultTask() {

    @get:InputFiles
    @get:SkipWhenEmpty
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    abstract val yamlFiles: ConfigurableFileCollection

    @get:Input
    abstract val packageName: Property<String>

    @get:Input
    abstract val moduleName: Property<String>

    @get:Input
    abstract val fallbackLang: Property<String>

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun run() {
        val catalogs = parseCatalogs(yamlFiles.files)
        val fallback = findFallback(catalogs, fallbackLang.get())

        val config = I18nGenConfig(packageName = packageName.get(), moduleName = moduleName.get())
        val generated = KotlinEmitter.emit(config, fallback = fallback, allLocales = catalogs)

        val out = outputDir.get().asFile
        out.deleteRecursively()
        out.mkdirs()
        generated.forEach { file -> File(out, file.fileName).writeText(file.content) }
    }
}

/**
 * Verifies non-fallback catalogs against the fallback (D8 rules). ERROR findings always fail the
 * build; WARNINGs fail under `-Pi18n.strict`; INFO is logged. Wired into `check`.
 *
 * NOT `@SkipWhenEmpty` — a `requiredLangs` module whose catalogs are all absent must still fail, which
 * a skipped task could not do.
 */
abstract class CheckI18nTask : DefaultTask() {

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    abstract val yamlFiles: ConfigurableFileCollection

    @get:Input
    abstract val fallbackLang: Property<String>

    @get:Input
    abstract val requiredLangs: SetProperty<String>

    @get:Input
    abstract val strict: Property<Boolean>

    @get:OutputFile
    abstract val report: RegularFileProperty

    @TaskAction
    fun run() {
        val required = requiredLangs.get().map { YamlCatalogParser.normalizeLocaleTag(it) }.toSet()
        val regionalRequired = required.filter { it.contains('-') }
        if (regionalRequired.isNotEmpty()) {
            throw GradleException(
                "i18n requiredLangs targets base languages; regional variant(s) " +
                        "${regionalRequired.joinToString()} are not allowed (a region inherits its base)."
            )
        }

        val catalogs = parseCatalogs(yamlFiles.files)
        if (catalogs.isEmpty()) {
            if (required.isNotEmpty()) {
                throw GradleException("i18n requiredLangs ${required.joinToString()} but no catalogs were found.")
            }
            return
        }

        val fallback = findFallback(catalogs, fallbackLang.get())
        val others = catalogs.filter { it.localeTag != fallback.localeTag }
        val findings = I18nChecker.check(fallback, others, required = required)

        findings.forEach { f ->
            val line = f.render()
            when (f.severity) {
                CheckSeverity.ERROR -> logger.error(line)
                CheckSeverity.WARNING -> logger.warn(line)
                CheckSeverity.INFO -> logger.lifecycle(line)
            }
        }
        report.get().asFile.apply { parentFile.mkdirs() }.writeText(findings.joinToString("\n") { it.render() })

        val outcome = checkOutcome(findings, strict.get())
        if (outcome.failed) {
            throw GradleException(
                "i18n check failed: ${outcome.errors} error(s), ${outcome.warnings} warning(s)" +
                        if (outcome.errors == 0) " (strict mode)" else ""
            )
        }
    }

    /** Renders a finding for logs/report, stripping control chars so a crafted key can't forge lines. */
    private fun CheckFinding.render(): String {
        val safeKey = key.filterNot { it.isISOControl() }
        val safeMsg = message.filterNot { it.isISOControl() }
        return "i18n [$severity] $locale :: $safeKey — $safeMsg"
    }
}

private fun parseCatalogs(files: Set<File>): List<LocaleCatalog> {
    val catalogs = files.sortedBy { it.name }.map { file ->
        val tag = file.name.removePrefix("messages.").removeSuffix(".yaml")
        try {
            YamlCatalogParser.parse(tag, file.readText())
        } catch (e: Exception) {
            throw GradleException("i18n: failed to parse '${file.name}': ${e.message}", e)
        }
    }
    val dupes = catalogs.groupingBy { it.localeTag }.eachCount().filterValues { it > 1 }.keys
    if (dupes.isNotEmpty()) {
        throw GradleException("i18n: multiple catalog files normalize to the same locale tag: ${dupes.joinToString()}")
    }
    return catalogs
}

private fun findFallback(catalogs: List<LocaleCatalog>, fallbackLang: String): LocaleCatalog {
    val tag = YamlCatalogParser.normalizeLocaleTag(fallbackLang)
    return catalogs.find { it.localeTag == tag } ?: throw GradleException(
        "i18n: no catalog for the fallback language '$tag' " +
                "(expected a messages.$tag.yaml; found: ${catalogs.map { it.localeTag }})"
    )
}

/**
 * Wires the i18n codegen into a Kotlin (KMP or JVM) module:
 *
 * ```kotlin
 * apply<I18nPlugin>()
 * configure<I18nExtension> {
 *     packageName.set("io.peekandpoke.kraft.core.i18n")
 *     requiredLangs("de")
 *     // sourceSet.set("main") for a pure kotlin("jvm") module (default is commonMain)
 * }
 * ```
 *
 * Yaml at `src/<sourceSet>/i18n/messages.<locale>.yaml` → generated code in
 * `build/generated/i18n/<sourceSet>/kotlin`, registered on the source set via the task provider so
 * every compile (metadata + platforms) depends on generation automatically.
 */
class I18nPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val ext = project.extensions.create<I18nExtension>("i18n").apply {
            fallbackLang.convention("en")
            sourceSet.convention("commonMain")
            moduleName.convention(kotlinClassPart(project.name, "module name"))
        }

        val sourceDir = project.layout.projectDirectory.dir(ext.sourceSet.map { "src/$it/i18n" })
        // Directory.asFileTree captures no Project reference (configuration-cache safe).
        val yamlTree = sourceDir.map { it.asFileTree.matching { include("messages.*.yaml") } }

        val generate = project.tasks.register<GenerateI18nTask>("generateI18n") {
            group = "build"
            description = "Generates typed i18n accessors + baked catalog from yaml"
            yamlFiles.from(yamlTree)
            packageName.set(ext.packageName)
            moduleName.set(ext.moduleName)
            fallbackLang.set(ext.fallbackLang)
            outputDir.set(project.layout.buildDirectory.dir(ext.sourceSet.map { "generated/i18n/$it/kotlin" }))
        }

        val check = project.tasks.register<CheckI18nTask>("checkI18n") {
            group = "verification"
            description = "Checks i18n catalogs for missing keys and placeholder mismatches"
            yamlFiles.from(yamlTree)
            fallbackLang.set(ext.fallbackLang)
            requiredLangs.set(ext.requiredLangs)
            strict.set(project.providers.gradleProperty("i18n.strict").map { it != "false" }.orElse(false))
            report.set(project.layout.buildDirectory.file("reports/i18n/check.txt"))
        }

        project.plugins.withType(LifecycleBasePlugin::class.java) {
            project.tasks.named(LifecycleBasePlugin.CHECK_TASK_NAME) { dependsOn(check) }
        }

        // The source-set name must be resolved after the build script configured the extension.
        project.afterEvaluate {
            val setName = ext.sourceSet.get()
            require(setName.matches(Regex("[A-Za-z][A-Za-z0-9]*"))) {
                "i18n sourceSet must be a simple source-set name (e.g. commonMain, jsMain, main); got '$setName'."
            }
            project.extensions.configure<KotlinProjectExtension> {
                val set = sourceSets.findByName(setName)
                    ?: throw GradleException(
                        "i18n: source set '$setName' not found. A pure kotlin(\"jvm\") module should set " +
                                "i18n { sourceSet.set(\"main\") }; a KMP module uses commonMain (default)."
                    )
                set.kotlin.srcDir(generate)
            }
        }
    }
}
