package io.peekandpoke.kraft.addons.prismjs

import kotlinx.browser.document
import kotlinx.coroutines.await
import kotlinx.coroutines.delay
import org.w3c.dom.Element
import org.w3c.dom.HTMLHeadElement
import org.w3c.dom.HTMLLinkElement
import kotlin.js.Date
import kotlin.js.Promise

/**
 * See: https://cdnjs.com/libraries/prism/1.28.0 for available assets
 */
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
            val promise = importPromise ?: (js("import('prismjs')") as Promise<dynamic>).also {
                importPromise = it
            }

            val module: dynamic = promise.await()

            if (prism == null) {
                prism = module.default ?: module
            }
        }
    }

    internal class StylesLoader {

        private val loaded = mutableMapOf<String, Element>()

        private val head: HTMLHeadElement?
            get() = document.getElementsByTagName("head").item(0) as? HTMLHeadElement

        fun loadDefaultTheme() {
            loadStyle(
                "https://cdnjs.cloudflare.com/ajax/libs/prism/1.28.0/themes/prism.min.css",
                "sha512-tN7Ec6zAFaVSG3TpNAKtk4DOHNpSwKHxxrsiw4GHKESGPs5njn/0sMCUMl2svV4wo4BK/rCP7juYz+zx+l6oeQ==",
            )

            loadLineNumbers()
        }

        fun loadInlineColor() {
            loadStyle(
                "https://cdnjs.cloudflare.com/ajax/libs/prism/1.28.0/plugins/inline-color/prism-inline-color.min.css",
                "sha512-jPGdTBr51+zDG6sY0smU+6rV19GOIN9RXAdVT8Gyvb55dToNJwq2n9SgCa764+z0xMuGA3/idik1tkQQhmALSA==",
            )
        }

        fun loadLineNumbers() {
            loadStyle(
                "https://cdnjs.cloudflare.com/ajax/libs/prism/1.28.0/plugins/line-numbers/prism-line-numbers.min.css",
                "sha512-cbQXwDFK7lj2Fqfkuxbo5iD1dSbLlJGXGpfTDqbggqjHJeyzx88I3rfwjS38WJag/ihH7lzuGlGHpDBymLirZQ==",
            )
        }

        fun loadToolbar() {
            loadStyle(
                "https://cdnjs.cloudflare.com/ajax/libs/prism/1.28.0/plugins/toolbar/prism-toolbar.min.css",
                "sha512-Dqf5696xtofgH089BgZJo2lSWTvev4GFo+gA2o4GullFY65rzQVQLQVlzLvYwTo0Bb2Gpb6IqwxYWtoMonfdhQ==",
            )
        }

        private fun loadStyle(
            href: String,
            integrity: String? = null,
            crossOrigin: String = "anonymous",
            referrerPolicy: String = "no-referrer",
        ) {
            head?.let { h ->
                loaded.getOrPut(href) {
                    (document.createElement("link") as HTMLLinkElement).apply {
                        this.rel = "stylesheet"
                        this.href = href
                        integrity?.let {
                            this.asDynamic().integrity = it
                        }
                        this.crossOrigin = crossOrigin
                        this.referrerPolicy = referrerPolicy

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
            js("import('prismjs/components/prism-markup-templating')") as Promise<dynamic>
        }

        // Language loaders ////////////////////////////////////////////////////////////////////////////////////////////

        private suspend fun loadDart(): dynamic = loadInternal("dart") {
            js("import('prismjs/components/prism-dart')") as Promise<dynamic>
        }

        private suspend fun loadJava(): dynamic = loadInternal("java") {
            js("import('prismjs/components/prism-java')") as Promise<dynamic>
        }

        private suspend fun loadJson(): dynamic = loadInternal("json") {
            js("import('prismjs/components/prism-json')") as Promise<dynamic>
        }

        private suspend fun loadJson5(): dynamic {
            // requires json to be loaded
            loadJson()

            return loadInternal("json5") {
                js("import('prismjs/components/prism-json5')") as Promise<dynamic>
            }
        }

        private suspend fun loadJsonp(): dynamic {
            // requires json to be loaded
            loadJson()

            return loadInternal("jsonp") {
                js("import('prismjs/components/prism-jsonp')") as Promise<dynamic>
            }
        }

        private suspend fun loadKotlin(): dynamic = loadInternal("kotlin") {
            js("import('prismjs/components/prism-kotlin')") as Promise<dynamic>
        }

        private suspend fun loadLess(): dynamic = loadInternal("less") {
            js("import('prismjs/components/prism-less')") as Promise<dynamic>
        }

        private suspend fun loadPhp(): dynamic {

            // Why is that? https://github.com/PrismJS/prism/issues/1400#issuecomment-485847919
            loadMarkupTemplating()

            return loadInternal("php") {
                js("import('prismjs/components/prism-php')") as Promise<dynamic>
            }
        }

        private suspend fun loadRegex(): dynamic = loadInternal("regex") {
            js("import('prismjs/components/prism-regex')") as Promise<dynamic>
        }

        private suspend fun loadRuby(): dynamic = loadInternal("ruby") {
            js("import('prismjs/components/prism-ruby')") as Promise<dynamic>
        }

        private suspend fun loadRust(): dynamic = loadInternal("rust") {
            js("import('prismjs/components/prism-rust')") as Promise<dynamic>
        }

        private suspend fun loadSass(): dynamic = loadInternal("sass") {
            js("import('prismjs/components/prism-sass')") as Promise<dynamic>
        }

        private suspend fun loadScss(): dynamic = loadInternal("scss") {
            js("import('prismjs/components/prism-scss')") as Promise<dynamic>
        }

        private suspend fun loadTypescript(): dynamic = loadInternal("typescript") {
            js("import('prismjs/components/prism-typescript')") as Promise<dynamic>
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
                js("import('prismjs/plugins/copy-to-clipboard/prism-copy-to-clipboard')") as Promise<dynamic>
            }
        }

        suspend fun loadInlineColor(): dynamic {
            styles.loadInlineColor()

            return loadInternal("inlineColor", { _, duration -> duration < 100 }) {
                js("import('prismjs/plugins/inline-color/prism-inline-color')") as Promise<dynamic>
            }
        }

        suspend fun loadLineNumbers(): dynamic {
            styles.loadLineNumbers()

            return loadInternal("lineNumbers") {
                js("import('prismjs/plugins/line-numbers/prism-line-numbers')") as Promise<dynamic>
            }
        }

        suspend fun loadShowLanguage(): dynamic {
            // Depends on the Toolbar plugin to be loaded
            loadToolbar()

            return loadInternal("showLanguage", { _, duration -> duration < 10 }) {
                js("import('prismjs/plugins/show-language/prism-show-language')") as Promise<dynamic>
            }
        }

        suspend fun loadToolbar(): dynamic {
            styles.loadToolbar()

            return loadInternal("toolbar") {
                js("import('prismjs/plugins/toolbar/prism-toolbar')") as Promise<dynamic>
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
