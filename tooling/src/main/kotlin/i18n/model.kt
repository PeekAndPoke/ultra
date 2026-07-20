package io.peekandpoke.ultra.tooling.i18n

/** Configuration for generating one module's i18n code. */
data class I18nGenConfig(
    val packageName: String,
    /** Unique per module; drives the catalog object and file names, e.g. `"KraftCore"`. */
    val moduleName: String,
    /**
     * Locales that must be at full parity with the fallback (e.g. `setOf("de")`). For these the
     * checker escalates: a missing catalog or a missing key becomes an ERROR instead of a warning.
     * Surfaced in the Gradle plugin as `requiredLangs(...)`.
     */
    val requiredLocales: Set<String> = emptySet(),
) {
    val catalogObjectName: String get() = "${moduleName}Catalog"
    val catalogFileName: String get() = "${moduleName}Catalog.kt"
    val accessorsFileName: String get() = "${moduleName}I18n.kt"
}

/** A generated Kotlin source file. */
data class GeneratedFile(val fileName: String, val content: String)

/** A parsed per-locale catalog: normalized locale tag -> (flat dotted key -> template). */
data class LocaleCatalog(
    val localeTag: String,
    val entries: Map<String, String>,
)

/** A node in the namespace tree built from the fallback catalog. */
sealed interface I18nNode {
    /** The local segment name, e.g. `forms`, `minLength`. */
    val name: String
}

/** A namespace groups child nodes, e.g. `forms { ... }` — becomes a receiver class + accessor. */
data class I18nNamespace(
    override val name: String,
    val children: List<I18nNode>,
) : I18nNode

/** A leaf message = one generated accessor function. */
data class I18nMessage(
    override val name: String,
    /** Full dotted key WITHOUT any plural suffix, e.g. `forms.minLength`. */
    val key: String,
    /** Placeholder names in first-seen order, excluding the implicit plural `count`. */
    val placeholders: List<String>,
    /** True when the message has `_one`/`_other`/... plural forms. */
    val plural: Boolean,
) : I18nNode
