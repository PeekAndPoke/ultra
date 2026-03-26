package io.peekandpoke.kraft.addons.prismjs

import org.w3c.dom.HTMLPreElement
import org.w3c.dom.set

/** Base class for PrismJS plugins that can be loaded dynamically and applied to `<pre>` elements. */
sealed class PrismPlugin {

    /** Loads the plugin's JS and CSS assets. */
    abstract suspend fun load()

    /** Applies the plugin's configuration to the given [pre] element. */
    abstract fun applyTo(pre: HTMLPreElement)

    /** Plugin that adds a "Copy to Clipboard" button to code blocks. */
    data class CopyToClipboard(
        val copy: String,
    ) : PrismPlugin() {
        companion object {
            fun Prism.OptionsBuilder.copyToClipboard(
                copy: String = "Copy",
            ) = usePlugin(
                CopyToClipboard(
                    copy = copy,
                )
            )
        }

        override suspend fun load() {
            prismJsInternals.plugins.loadCopyToClipboard()
        }

        override fun applyTo(pre: HTMLPreElement) {
            pre.className += " copy-to-clipboard"
            pre.dataset["prismjsCopy"] = copy
        }
    }

    /** Plugin that renders inline color previews for CSS color values. */
    data class InlineColor(
        val enabled: Boolean = true,
    ) : PrismPlugin() {
        companion object {
            fun Prism.OptionsBuilder.inlineColor() = usePlugin(
                InlineColor()
            )
        }

        override suspend fun load() {
            prismJsInternals.plugins.loadInlineColor()
        }

        override fun applyTo(pre: HTMLPreElement) {
            pre.className += " inline-color"
        }
    }

    /** Plugin that displays line numbers alongside code. */
    data class LineNumbers(
        val start: Int,
        val softWrap: Boolean,
    ) : PrismPlugin() {
        companion object {
            fun Prism.OptionsBuilder.lineNumbers(
                start: Int = 1,
                softWrap: Boolean = false,
            ) = usePlugin(
                LineNumbers(start = start, softWrap = softWrap)
            )
        }

        override suspend fun load() {
            prismJsInternals.plugins.loadLineNumbers()
        }

        override fun applyTo(pre: HTMLPreElement) {
            pre.className += " line-numbers"
            pre.dataset["start"] = start.toString()
            if (softWrap) {
                pre.style.whiteSpace = "pre-wrap"
            }
        }
    }

    /** Plugin that displays the language name in the toolbar. */
    data class ShowLanguage(
        val language: String?,
    ) : PrismPlugin() {
        companion object {
            fun Prism.OptionsBuilder.showLanguage(
                language: String? = null,
            ) = usePlugin(
                ShowLanguage(language = language)
            )
        }

        override suspend fun load() {
            prismJsInternals.plugins.loadShowLanguage()
        }

        override fun applyTo(pre: HTMLPreElement) {
            pre.className += " show-language"
            language?.let {
                pre.dataset["language"] = it
            }
        }
    }
}
