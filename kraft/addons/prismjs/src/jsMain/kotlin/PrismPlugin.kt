package de.peekandpoke.kraft.addons.prismjs

import org.w3c.dom.HTMLPreElement
import org.w3c.dom.set

sealed class PrismPlugin {

    abstract suspend fun load()
    abstract fun applyTo(pre: HTMLPreElement)

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
