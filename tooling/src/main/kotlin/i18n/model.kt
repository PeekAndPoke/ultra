package io.peekandpoke.ultra.tooling.i18n

/** Configuration for generating one module's i18n code. */
data class I18nGenConfig(
    val packageName: String,
    /** Unique per module; drives the catalog object and file names, e.g. `"KraftCore"`. */
    val moduleName: String,
) {
    val catalogObjectName: String get() = "${moduleName}Catalog"
    val catalogFileName: String get() = "${moduleName}Catalog.kt"
    val accessorsFileName: String get() = "${moduleName}I18n.kt"
}

/** A generated Kotlin source file. */
data class GeneratedFile(val fileName: String, val content: String)
