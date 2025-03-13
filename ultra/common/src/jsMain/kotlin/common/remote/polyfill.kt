package de.peekandpoke.ultra.common.remote

import kotlinx.browser.window

@Suppress("unused")
@JsModule("whatwg-fetch")
@JsNonModule
external fun fetch()

/**
 * This function does nothing.
 *
 * But calling it will import the polyfill for fetch.
 */
fun polyfillFetch() {
    val win = window.asDynamic()

    if (win.fetch == undefined || win.fetch == null) {
        win.fetch = ::fetch
    }
}
