package io.peekandpoke.ultra.tooling.i18n

import org.yaml.snakeyaml.LoaderOptions
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.SafeConstructor

/** Parses one YAML catalog file into a flat, dotted-key [LocaleCatalog]. */
object YamlCatalogParser {

    /** Parses [yaml] for the given [localeTag] (e.g. `"en"`, `"de-CH"`); the tag is normalized. */
    fun parse(localeTag: String, yaml: String): LocaleCatalog {
        // SafeConstructor: never instantiate arbitrary Java types from `!!`-tagged YAML.
        val loaded = Yaml(SafeConstructor(LoaderOptions())).load<Any?>(yaml)
        val flat = LinkedHashMap<String, String>()
        flatten(prefix = "", node = loaded, out = flat)
        return LocaleCatalog(localeTag = normalizeLocaleTag(localeTag), entries = flat)
    }

    private fun flatten(prefix: String, node: Any?, out: MutableMap<String, String>) {
        when (node) {
            null -> Unit
            is Map<*, *> -> node.forEach { (rawKey, value) ->
                val key = if (prefix.isEmpty()) rawKey.toString() else "$prefix.$rawKey"
                flatten(key, value, out)
            }
            else -> out[prefix] = node.toString()
        }
    }

    /**
     * Normalizes a locale tag the same way `ultra:i18n` `Locale.tag` does (language lower-case, region
     * upper-case) so baked catalog keys match runtime lookups. Pinned by `YamlCatalogParserSpec`.
     */
    fun normalizeLocaleTag(tag: String): String {
        val parts = tag.trim().split('-', '_', limit = 2)
        val language = parts[0].lowercase()
        val region = parts.getOrNull(1)?.takeIf { it.isNotBlank() }?.uppercase()
        return if (region == null) language else "$language-$region"
    }
}
