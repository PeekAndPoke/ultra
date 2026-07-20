package io.peekandpoke.ultra.tooling.i18n

/** Severity of a catalog check finding. */
enum class CheckSeverity { ERROR, WARNING, INFO }

/** One catalog consistency finding (D8). */
data class CheckFinding(
    val severity: CheckSeverity,
    val locale: String,
    val key: String,
    val message: String,
)

/** Aggregate verdict over a set of findings at a given strictness (the build fail/pass decision). */
data class CheckOutcome(val errors: Int, val warnings: Int, val infos: Int, val failed: Boolean)

/** The build-failure decision: any ERROR fails; WARNINGs fail only in [strict] mode. */
fun checkOutcome(findings: List<CheckFinding>, strict: Boolean): CheckOutcome {
    val errors = findings.count { it.severity == CheckSeverity.ERROR }
    val warnings = findings.count { it.severity == CheckSeverity.WARNING }
    val infos = findings.count { it.severity == CheckSeverity.INFO }
    return CheckOutcome(errors, warnings, infos, failed = errors > 0 || (strict && warnings > 0))
}

/**
 * Checks non-fallback catalogs against the fallback (the sole API-surface source, D8).
 *
 * - A **base** language (no region, e.g. `de`) is checked for completeness: missing key → WARNING
 *   (ERROR if the locale is in [required]); a dead/superfluous key → WARNING.
 * - A **regional** variant (e.g. `de-CH`) is checked against its base: missing keys are EXPECTED (it
 *   inherits, no finding); a dead key → WARNING; a value identical to the base → redundant INFO.
 * - A dead/superfluous KEY is a WARNING for both (harmless but un-callable); an **introduced
 *   placeholder** is an ERROR for both (it renders a broken `{{x}}` at runtime). A locale may **omit**
 *   a placeholder (INFO). Plural placeholders are compared against the **union** of the fallback's
 *   `_one`/`_other` forms (D8), so a form legitimately using a union member is not flagged.
 * - [required] locales that are entirely absent, or missing a key, are ERRORs.
 */
object I18nChecker {

    private val PLACEHOLDER = Regex("""\{\{([a-zA-Z0-9_-]+)\}\}""")
    private val PLURAL_SUFFIXES = setOf("zero", "one", "two", "few", "many", "other")

    fun check(
        fallback: LocaleCatalog,
        others: List<LocaleCatalog>,
        required: Set<String> = emptySet(),
    ): List<CheckFinding> {
        val findings = mutableListOf<CheckFinding>()
        val apiKeys = fallback.entries.keys
        val byTag = others.associateBy { it.localeTag }
        val requiredTags = required.map { YamlCatalogParser.normalizeLocaleTag(it) }.toSet()
        val fallbackUnion = placeholderUnionByBase(fallback)

        // A required locale with no catalog at all is the important one to catch — nothing else would.
        requiredTags.forEach { tag ->
            if (tag != fallback.localeTag && tag !in byTag) {
                findings += CheckFinding(CheckSeverity.ERROR, tag, "*", "required locale has no catalog")
            }
        }

        others.forEach { loc ->
            if (loc.localeTag.contains('-')) {
                checkRegional(loc, apiKeys, base = byTag[loc.localeTag.substringBefore('-')], findings)
            } else {
                checkBase(loc, apiKeys, required = loc.localeTag in requiredTags, findings)
            }
            checkPlaceholders(loc, fallbackUnion, findings)
        }
        return findings
    }

    private fun checkBase(
        loc: LocaleCatalog,
        apiKeys: Set<String>,
        required: Boolean,
        findings: MutableList<CheckFinding>,
    ) {
        val missing = if (required) CheckSeverity.ERROR else CheckSeverity.WARNING
        val suffix = if (required) " (required language)" else ""
        (apiKeys - loc.entries.keys).forEach {
            findings += CheckFinding(missing, loc.localeTag, it, "missing translation$suffix")
        }
        (loc.entries.keys - apiKeys).forEach {
            findings += CheckFinding(CheckSeverity.WARNING, loc.localeTag, it, "superfluous key (not in the fallback)")
        }
    }

    private fun checkRegional(
        loc: LocaleCatalog,
        apiKeys: Set<String>,
        base: LocaleCatalog?,
        findings: MutableList<CheckFinding>,
    ) {
        (loc.entries.keys - apiKeys).forEach {
            findings += CheckFinding(CheckSeverity.WARNING, loc.localeTag, it, "superfluous key (not in the fallback API surface)")
        }
        if (base != null) {
            loc.entries.forEach { (key, value) ->
                if (base.entries[key] == value) {
                    findings += CheckFinding(
                        CheckSeverity.INFO, loc.localeTag, key,
                        "value is identical to base '${base.localeTag}' — redundant override",
                    )
                }
            }
        }
        // Missing keys are expected for a regional variant (it inherits from the base) — no finding.
    }

    private fun checkPlaceholders(
        loc: LocaleCatalog,
        fallbackUnion: Map<String, Set<String>>,
        findings: MutableList<CheckFinding>,
    ) {
        loc.entries.forEach { (key, template) ->
            // Compare against the fallback's UNION across the message's plural forms (D8), minus the
            // implicit plural driver `count`, whose presence varies per form by design.
            val reference = (fallbackUnion[baseKeyOf(key)] ?: return@forEach) - "count"
            val local = placeholders(template) - "count"

            (local - reference).forEach {
                findings += CheckFinding(CheckSeverity.ERROR, loc.localeTag, key, "introduces placeholder {{$it}} not in the fallback")
            }
            (reference - local).forEach {
                findings += CheckFinding(CheckSeverity.INFO, loc.localeTag, key, "omits placeholder {{$it}} present in the fallback")
            }
        }
    }

    private fun placeholderUnionByBase(cat: LocaleCatalog): Map<String, Set<String>> {
        val out = LinkedHashMap<String, MutableSet<String>>()
        cat.entries.forEach { (key, template) ->
            out.getOrPut(baseKeyOf(key)) { mutableSetOf() }.addAll(placeholders(template))
        }
        return out
    }

    /** Strips a trailing plural suffix so plural forms map to one base key. */
    private fun baseKeyOf(key: String): String {
        val i = key.lastIndexOf('_')
        if (i <= 0) return key
        return if (key.substring(i + 1) in PLURAL_SUFFIXES) key.substring(0, i) else key
    }

    private fun placeholders(template: String): Set<String> =
        PLACEHOLDER.findAll(template).map { it.groupValues[1] }.toSet()
}
