package io.peekandpoke.kraft.testing

import io.peekandpoke.kraft.testing.KQuery.Selector.Companion.cssSelector
import kotlinx.coroutines.delay
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
 * Polls every [intervalMs] milliseconds up to [timeoutMs]. Returns the matching query once found; fails with an
 * [AssertionError] naming [css] when the timeout is reached.
 */
suspend fun KQuery<Element>.awaitCss(
    css: String,
    timeoutMs: Int = 2000,
    intervalMs: Int = 50,
): KQuery<Element> {
    require(intervalMs > 0) { "intervalMs must be > 0" }
    var remaining = timeoutMs

    while (true) {
        val result = selectCss(css)
        if (result.isNotEmpty()) return result
        if (remaining <= 0) throw AssertionError("awaitCss: nothing matched '$css' within $timeoutMs ms")
        delay(intervalMs.toLong())
        remaining -= intervalMs
    }
}

/**
 * Waits until no element matches [css] inside the current query.
 *
 * Polls every [intervalMs] milliseconds; fails with an [AssertionError] naming [css] after [timeoutMs].
 */
suspend fun KQuery<Element>.awaitGone(
    css: String,
    timeoutMs: Int = 2000,
    intervalMs: Int = 50,
) {
    require(intervalMs > 0) { "intervalMs must be > 0" }
    var remaining = timeoutMs

    while (true) {
        val found = selectCss(css).size
        if (found == 0) return
        if (remaining <= 0) throw AssertionError("awaitGone: '$css' still matched $found element(s) after $timeoutMs ms")
        delay(intervalMs.toLong())
        remaining -= intervalMs
    }
}
