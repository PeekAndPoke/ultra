package io.peekandpoke.ultra.i18n.model

// Source-included into buildSrc — read the contract at the top of `I18nCatalogModel.kt` before editing.

/**
 * A node in the namespace tree built from the fallback catalog.
 *
 * The tree is target-agnostic: the Kotlin emitter turns a namespace into a receiver class and a message
 * into an extension function, a TypeScript emitter into a nested object and a function taking an
 * options object. Neither shape belongs here.
 */
sealed interface I18nNode {
    /** The local segment name, e.g. `forms`, `minLength`. */
    val name: String
}

/** A namespace groups child nodes, e.g. `forms { ... }`. */
data class I18nNamespace(
    override val name: String,
    val children: List<I18nNode>,
) : I18nNode

/** A leaf message = one generated accessor. */
data class I18nMessage(
    override val name: String,
    /** Full dotted key WITHOUT any plural suffix, e.g. `forms.minLength`. */
    val key: String,
    /** Placeholder names in first-seen order, excluding the implicit plural `count`. */
    val placeholders: List<String>,
    /** True when the message has `_one`/`_other`/... plural forms. */
    val plural: Boolean,
) : I18nNode

/**
 * Builds the namespace tree from the FALLBACK catalog — the sole source of the API surface (D8).
 *
 * The tree is a pure function of [LocaleCatalog.entries]: namespaces come from splitting dotted keys,
 * placeholders from scanning the templates, plural flags from the key suffixes. Nothing else is
 * consulted, which is what lets a generator re-derive the identical tree from a catalog that was baked
 * into Kotlin at build time.
 */
object I18nModelBuilder {

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
                .flatMap { template -> I18nPlaceholders.namesIn(template) }
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
