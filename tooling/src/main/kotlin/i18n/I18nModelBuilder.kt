package io.peekandpoke.ultra.tooling.i18n

/** Builds the namespace tree from the FALLBACK catalog — the sole source of the API surface (D8). */
object I18nModelBuilder {

    /** CLDR plural category suffixes recognised on message keys (i18next-style, D7). */
    private val PLURAL_SUFFIXES = setOf("zero", "one", "two", "few", "many", "other")

    private val PLACEHOLDER = Regex("""\{\{([a-zA-Z0-9_-]+)\}\}""")

    /** A message accumulated across its plural forms while grouping. */
    private class Acc {
        var plural = false
        val templates = mutableListOf<String>()
    }

    /** The top-level nodes (namespaces/messages) of the module. */
    fun build(fallback: LocaleCatalog): List<I18nNode> {
        // 1. Collapse plural siblings; gather all templates per message path (for placeholder detection).
        val messages = LinkedHashMap<String, Acc>()
        fallback.entries.forEach { (flatKey, template) ->
            val (path, isPlural) = splitPluralSuffix(flatKey)
            val acc = messages.getOrPut(path) { Acc() }
            if (isPlural) acc.plural = true
            acc.templates.add(template)
        }

        // 2. Build each leaf message with its detected placeholders (count excluded — implicit).
        val leaves = messages.map { (path, acc) ->
            val placeholders = acc.templates
                .flatMap { template -> PLACEHOLDER.findAll(template).map { it.groupValues[1] } }
                .distinct()
                // `count` is the implicit plural driver ONLY for plural messages; for a non-plural
                // message `{{count}}` is an ordinary placeholder and must stay a parameter.
                .filter { !(acc.plural && it == "count") }
            I18nMessage(
                name = path.substringAfterLast('.'),
                key = path,
                placeholders = placeholders,
                plural = acc.plural,
            )
        }

        // 3. Assemble the namespace tree from the dotted paths.
        return buildLevel(prefix = "", leaves = leaves)
    }

    /** Splits a trailing `_one`/`_other`/... plural suffix off [flatKey]; returns (path, isPlural). */
    private fun splitPluralSuffix(flatKey: String): Pair<String, Boolean> {
        val lastSep = flatKey.lastIndexOf('_')
        if (lastSep <= 0) return flatKey to false
        val suffix = flatKey.substring(lastSep + 1)
        return if (suffix in PLURAL_SUFFIXES) flatKey.substring(0, lastSep) to true else flatKey to false
    }

    /** Recursively groups leaves whose path continues past [prefix] into child namespaces. */
    private fun buildLevel(prefix: String, leaves: List<I18nMessage>): List<I18nNode> {
        val directMessages = mutableListOf<I18nMessage>()
        val byNext = LinkedHashMap<String, MutableList<I18nMessage>>()

        leaves.forEach { leaf ->
            val remaining = if (prefix.isEmpty()) leaf.key else leaf.key.removePrefix("$prefix.")
            val segments = remaining.split('.')
            if (segments.size == 1) {
                directMessages.add(leaf)
            } else {
                byNext.getOrPut(segments.first()) { mutableListOf() }.add(leaf)
            }
        }

        val collisions = directMessages.map { it.name }.toSet() intersect byNext.keys
        require(collisions.isEmpty()) {
            val where = prefix.ifEmpty { "<root>" }
            "i18n key collision under '$where': ${collisions.joinToString()} is both a message and a namespace."
        }

        val result = mutableListOf<I18nNode>()
        result.addAll(directMessages)
        byNext.forEach { (segment, group) ->
            val childPrefix = if (prefix.isEmpty()) segment else "$prefix.$segment"
            result.add(I18nNamespace(name = segment, children = buildLevel(childPrefix, group)))
        }
        return result
    }
}
