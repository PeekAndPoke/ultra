package io.peekandpoke.ultra.tooling.i18n

import io.peekandpoke.ultra.i18n.model.LocaleCatalog
import io.peekandpoke.ultra.i18n.model.normalizeLocaleTag
import org.yaml.snakeyaml.LoaderOptions
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.SafeConstructor

/** Parses one YAML catalog file into a flat, dotted-key [LocaleCatalog]. */
object YamlCatalogParser {

    /**
     * Parses [yaml] for the given [localeTag] (e.g. `"en"`, `"de-CH"`).
     *
     * The tag goes through [normalizeLocaleTag], which shares its grammar with `Locale.parse`, so baked
     * catalog keys are normalized exactly the way runtime lookups are.
     */
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
}
