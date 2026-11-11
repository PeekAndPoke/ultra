package io.peekandpoke.kraft.addons.prismjs

import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.await
import kotlinx.coroutines.delay
import org.w3c.dom.Element
import org.w3c.dom.HTMLHeadElement
import org.w3c.dom.HTMLLinkElement
import kotlin.js.Date
import kotlin.js.Promise

/** Loads Prism, its languages, plugins and stylesheets, all from the npm package `prismjs`. */
class PrismJsInternals {

    /** The dynamically loaded Prism module. Set by [CoreLoader.ensureLoaded]. */
    internal var prism: dynamic = null
        private set

    /** Ensures the core prismjs library is loaded via dynamic import. */
    internal inner class CoreLoader {
        private var importPromise: Promise<dynamic>? = null

        @Suppress("UnsafeCastFromDynamic")
        suspend fun ensureLoaded() {
            if (prism != null) return

            // Reuse the same promise for concurrent callers
            val promise = importPromise ?: run {
                // Manual mode, read by Prism when it loads: otherwise it highlights every `language-*` block on the
                // page. An app's own global Prism keeps its own setting.
                val global = window.asDynamic()
                if (global.Prism == null) global.Prism = js("({ manual: true })")

                (js("import('prismjs')") as Promise<dynamic>).also { importPromise = it }
            }

            val module: dynamic = try {
                promise.await()
            } catch (e: Throwable) {
                // A failed download is asked again next time, not remembered
                importPromise = null
                throw e
            }

            if (prism == null) {
                prism = module.default ?: module
            }
        }
    }

    /**
     * Prism's stylesheets, from the npm package `prismjs` (ULTRA-00306): `new URL(<file>, import.meta.url)` makes the
     * app's webpack copy the file into its own build output and gives back its URL, so the CSS comes from the app's
     * origin, not a CDN, and is still requested only when first needed. Webpack sees only a literal path, hence one
     * `js()` per file.
     */
    internal class StylesLoader {

        private val loaded = mutableMapOf<String, Element>()

        private val head: HTMLHeadElement?
            get() = document.getElementsByTagName("head").item(0) as? HTMLHeadElement

        fun loadDefaultTheme() {
            loadStyle(js("new URL('prismjs/themes/prism.min.css', import.meta.url)"))

            loadLineNumbers()
        }

        fun loadInlineColor() {
            loadStyle(js("new URL('prismjs/plugins/inline-color/prism-inline-color.min.css', import.meta.url)"))
        }

        fun loadLineNumbers() {
            loadStyle(js("new URL('prismjs/plugins/line-numbers/prism-line-numbers.min.css', import.meta.url)"))
        }

        fun loadToolbar() {
            loadStyle(js("new URL('prismjs/plugins/toolbar/prism-toolbar.min.css', import.meta.url)"))
        }

        private fun loadStyle(url: dynamic) {
            val href: String = url.href.unsafeCast<String>()

            head?.let { h ->
                loaded.getOrPut(href) {
                    (document.createElement("link") as HTMLLinkElement).apply {
                        this.rel = "stylesheet"
                        this.href = href

                        h.append(this)
                    }
                }
            }
        }
    }

    internal inner class LanguageLoader {

        private val loaded = mutableMapOf<String, Promise<dynamic>>()

        suspend fun load(language: String): dynamic {
            core.ensureLoaded()

            return when (val existing = prism.languages[language]) {
                null -> try {
                    when (language) {
                        "aql" -> loadAql()
                        "dart" -> loadDart()
                        "java" -> loadJava()
                        "json" -> loadJson()
                        "json5" -> loadJson5()
                        "jsonp" -> loadJsonp()
                        "kotlin" -> loadKotlin()
                        "kts" -> loadKotlin()
                        "less" -> loadLess()
                        "php" -> loadPhp()
                        "regex" -> loadRegex()
                        "ruby" -> loadRuby()
                        "rust" -> loadRust()
                        "sass" -> loadSass()
                        "scss" -> loadScss()
                        "typescript" -> loadTypescript()
                        // fallback
                        else -> prism.languages.plain
                    }
                } catch (t: Throwable) {
                    console.error("Could not load PrismJs language $language", t)
                    // fallback
                    prism.languages.plain
                }

                else -> existing
            }
        }

        // Prerequisite loaders ////////////////////////////////////////////////////////////////////////////////////////

        private suspend fun loadMarkupTemplating(): dynamic = loadInternal("markup-templating") {
            js("import('prismjs/components/prism-markup-templating.js')") as Promise<dynamic>
        }

        // Language loaders ////////////////////////////////////////////////////////////////////////////////////////////

        private suspend fun loadAql(): dynamic = loadInternal("aql") {
            js("import('prismjs/components/prism-aql.js')") as Promise<dynamic>
        }

        private suspend fun loadDart(): dynamic = loadInternal("dart") {
            js("import('prismjs/components/prism-dart.js')") as Promise<dynamic>
        }

        private suspend fun loadJava(): dynamic = loadInternal("java") {
            js("import('prismjs/components/prism-java.js')") as Promise<dynamic>
        }

        private suspend fun loadJson(): dynamic = loadInternal("json") {
            js("import('prismjs/components/prism-json.js')") as Promise<dynamic>
        }

        private suspend fun loadJson5(): dynamic {
            // requires json to be loaded
            loadJson()

            return loadInternal("json5") {
                js("import('prismjs/components/prism-json5.js')") as Promise<dynamic>
            }
        }

        private suspend fun loadJsonp(): dynamic {
            // requires json to be loaded
            loadJson()

            return loadInternal("jsonp") {
                js("import('prismjs/components/prism-jsonp.js')") as Promise<dynamic>
            }
        }

        private suspend fun loadKotlin(): dynamic = loadInternal("kotlin") {
            js("import('prismjs/components/prism-kotlin.js')") as Promise<dynamic>
        }

        private suspend fun loadLess(): dynamic = loadInternal("less") {
            js("import('prismjs/components/prism-less.js')") as Promise<dynamic>
        }

        private suspend fun loadPhp(): dynamic {

            // Why is that? https://github.com/PrismJS/prism/issues/1400#issuecomment-485847919
            loadMarkupTemplating()

            return loadInternal("php") {
                js("import('prismjs/components/prism-php.js')") as Promise<dynamic>
            }
        }

        private suspend fun loadRegex(): dynamic = loadInternal("regex") {
            js("import('prismjs/components/prism-regex.js')") as Promise<dynamic>
        }

        private suspend fun loadRuby(): dynamic = loadInternal("ruby") {
            js("import('prismjs/components/prism-ruby.js')") as Promise<dynamic>
        }

        private suspend fun loadRust(): dynamic = loadInternal("rust") {
            js("import('prismjs/components/prism-rust.js')") as Promise<dynamic>
        }

        private suspend fun loadSass(): dynamic = loadInternal("sass") {
            js("import('prismjs/components/prism-sass.js')") as Promise<dynamic>
        }

        private suspend fun loadScss(): dynamic = loadInternal("scss") {
            js("import('prismjs/components/prism-scss.js')") as Promise<dynamic>
        }

        private suspend fun loadTypescript(): dynamic = loadInternal("typescript") {
            js("import('prismjs/components/prism-typescript.js')") as Promise<dynamic>
        }

        private suspend fun loadInternal(language: String, promise: () -> Promise<dynamic>): dynamic {

            loaded.getOrPut(language) {
                promise()
            }

            val start = Date.now()

            @Suppress("UnsafeCastFromDynamic")
            while (prism.languages[language] == undefined && Date.now() - start < 10_000) {
                delay(10)
            }

            return prism.languages[language]
        }
    }

    internal inner class PluginsLoader {

        private val loaded = mutableMapOf<String, Promise<dynamic>>()

        // Plugin loaders //////////////////////////////////////////////////////////////////////////////////////////////

        suspend fun loadCopyToClipboard(): dynamic {
            // Depends on the Toolbar plugin to be loaded
            loadToolbar()

            return loadInternal("copyToClipboard", { _, duration -> duration < 100 }) {
                js("import('prismjs/plugins/copy-to-clipboard/prism-copy-to-clipboard.js')") as Promise<dynamic>
            }
        }

        suspend fun loadInlineColor(): dynamic {
            styles.loadInlineColor()

            return loadInternal("inlineColor", { _, duration -> duration < 100 }) {
                js("import('prismjs/plugins/inline-color/prism-inline-color.js')") as Promise<dynamic>
            }
        }

        suspend fun loadLineNumbers(): dynamic {
            styles.loadLineNumbers()

            return loadInternal("lineNumbers") {
                js("import('prismjs/plugins/line-numbers/prism-line-numbers.js')") as Promise<dynamic>
            }
        }

        suspend fun loadShowLanguage(): dynamic {
            // Depends on the Toolbar plugin to be loaded
            loadToolbar()

            return loadInternal("showLanguage", { _, duration -> duration < 10 }) {
                js("import('prismjs/plugins/show-language/prism-show-language.js')") as Promise<dynamic>
            }
        }

        suspend fun loadToolbar(): dynamic {
            styles.loadToolbar()

            return loadInternal("toolbar") {
                js("import('prismjs/plugins/toolbar/prism-toolbar.js')") as Promise<dynamic>
            }
        }

        private suspend fun loadInternal(
            plugin: String,
            isLoading: (pluginName: String, duration: Double) -> Boolean = { pluginName, duration ->
                prism.plugins[pluginName] == undefined && duration < 10_000
            },
            promise: () -> Promise<dynamic>,
        ): dynamic {
            core.ensureLoaded()

            loaded.getOrPut(plugin) {
                promise()
            }

            val start = Date.now()

            @Suppress("UnsafeCastFromDynamic")
            while (isLoading(plugin, Date.now() - start)) {
                delay(10)
            }

            return prism.plugins[plugin]
        }
    }

    internal val core = CoreLoader()
    internal val styles = StylesLoader()
    internal val languages = LanguageLoader()
    internal val plugins = PluginsLoader()
}

/** Global singleton for PrismJS asset loading (styles, languages, plugins). */
val prismJsInternals = PrismJsInternals()
