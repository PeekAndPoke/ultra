package io.peekandpoke.kraft.testing

import io.peekandpoke.kraft.testing.KQuery.Selector.Companion.cssSelector
import org.w3c.dom.Element

// =====================================================================================================================
// Core select
// =====================================================================================================================

/** Queries the current elements using the given [selector] and returns a new [KQuery] of matches. */
suspend fun <T : Element> KQuery<Element>.select(selector: KQuery.Selector<T>): KQuery<T> {
    return selector.applyTo(this)
}

// =====================================================================================================================
// selectCss
// =====================================================================================================================

/** Queries the current elements using a CSS selector string. */
suspend fun KQuery<Element>.selectCss(css: String): KQuery<Element> {
    return select(css.cssSelector)
}

/** Queries the current elements using a CSS selector string, filtering to type [T]. */
suspend inline fun <reified T : Element> KQuery<Element>.selectCss(css: String = "*"): KQuery<T> {
    return select(css.cssSelector<T>())
}

// =====================================================================================================================
// selectDebugId
// =====================================================================================================================

/** Queries the current elements for elements with the given `debug-id` attribute. */
suspend fun KQuery<Element>.selectDebugId(id: String): KQuery<Element> {
    return selectCss("""[debug-id="$id"]""")
}

/** Queries the current elements for elements with the given `debug-id` attribute, filtering to type [T]. */
suspend inline fun <reified T : Element> KQuery<Element>.selectDebugId(id: String): KQuery<T> {
    return selectCss<T>("""[debug-id="$id"]""")
}

// =====================================================================================================================
// awaitCss
// =====================================================================================================================

/**
 * Waits for elements matching [css] to appear inside the current query.
 *
 * Polls every [intervalMs] milliseconds up to [timeoutMs]. Returns the matching query
 * once found, or an empty query if the timeout is reached.
 */
suspend fun KQuery<Element>.awaitCss(
    css: String,
    timeoutMs: Int = 2000,
    intervalMs: Int = 50,
): KQuery<Element> {
    var remaining = timeoutMs

    while (remaining > 0) {
        val result = selectCss(css)
        if (result.isNotEmpty()) return result
        kotlinx.coroutines.delay(intervalMs.toLong())
        remaining -= intervalMs
    }

    return selectCss(css)
}
