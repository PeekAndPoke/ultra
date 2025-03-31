package de.peekandpoke.kraft.addons.prismjs

import js.import.Module
import kotlinx.browser.document
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

    internal class LanguageLoader {

        private val loaded = mutableMapOf<String, Promise<dynamic>>()

        suspend fun load(language: String): dynamic {
            return when (val existing = PrismJsDefinition.languages[language]) {
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
                        else -> PrismJsDefinition.languages.plain
                    }
                } catch (t: Throwable) {
                    console.error("Could not load PrismJs language $language", t)
                    // fallback
                    PrismJsDefinition.languages.plain
                }

                else -> existing
            }
        }

        // Prerequisite loaders ////////////////////////////////////////////////////////////////////////////////////////

        private suspend fun loadMarkupTemplating(): dynamic = load("markup-templating") {
            js("import('prismjs/components/prism-markup-templating')") as Promise<Module<*>>
        }

        // Language loaders ////////////////////////////////////////////////////////////////////////////////////////////

        private suspend fun loadDart(): dynamic = load("dart") {
            js("import('prismjs/components/prism-dart')") as Promise<Module<*>>
        }

        private suspend fun loadJava(): dynamic = load("java") {
            js("import('prismjs/components/prism-java')") as Promise<Module<*>>
        }

        private suspend fun loadJson(): dynamic = load("json") {
            js("import('prismjs/components/prism-json')") as Promise<Module<*>>
        }

        private suspend fun loadJson5(): dynamic {
            // requires json to be loaded
            loadJson()

            return load("json5") {
                js("import('prismjs/components/prism-json5')") as Promise<Module<*>>
            }
        }

        private suspend fun loadJsonp(): dynamic {
            // requires json to be loaded
            loadJson()

            return load("jsonp") {
                js("import('prismjs/components/prism-jsonp')") as Promise<Module<*>>
            }
        }

        private suspend fun loadKotlin(): dynamic = load("kotlin") {
            js("import('prismjs/components/prism-kotlin')") as Promise<Module<*>>
        }

        private suspend fun loadLess(): dynamic = load("less") {
            js("import('prismjs/components/prism-less')") as Promise<Module<*>>
        }

        private suspend fun loadPhp(): dynamic {

            // Why is that? https://github.com/PrismJS/prism/issues/1400#issuecomment-485847919
            loadMarkupTemplating()

            return load("php") {
                js("import('prismjs/components/prism-php')") as Promise<Module<*>>
            }
        }

        private suspend fun loadRegex(): dynamic = load("regex") {
            js("import('prismjs/components/prism-regex')") as Promise<Module<*>>
        }

        private suspend fun loadRuby(): dynamic = load("ruby") {
            js("import('prismjs/components/prism-ruby')") as Promise<Module<*>>
        }

        private suspend fun loadRust(): dynamic = load("rust") {
            js("import('prismjs/components/prism-rust')") as Promise<Module<*>>
        }

        private suspend fun loadSass(): dynamic = load("sass") {
            js("import('prismjs/components/prism-sass')") as Promise<Module<*>>
        }

        private suspend fun loadScss(): dynamic = load("scss") {
            js("import('prismjs/components/prism-scss')") as Promise<Module<*>>
        }

        private suspend fun loadTypescript(): dynamic = load("typescript") {
            js("import('prismjs/components/prism-typescript')") as Promise<Module<*>>
        }

        private suspend fun load(language: String, promise: () -> Promise<Module<*>>): dynamic {

            loaded.getOrPut(language) {
                promise()
            }

            val start = Date.now()

            @Suppress("UnsafeCastFromDynamic")
            while (PrismJsDefinition.languages[language] == undefined && Date.now() - start < 10_000) {
                delay(10)
            }

            return PrismJsDefinition.languages[language]
        }
    }

    internal class PluginsLoader(private val styles: StylesLoader) {

        private val loaded = mutableMapOf<String, Promise<dynamic>>()

        // Plugin loaders //////////////////////////////////////////////////////////////////////////////////////////////

        suspend fun loadCopyToClipboard(): dynamic {
            // Depends on the Toolbar plugin to be loaded
            loadToolbar()

            return load("copyToClipboard", { _, duration -> duration < 100 }) {
                js("import('prismjs/plugins/copy-to-clipboard/prism-copy-to-clipboard')") as Promise<Module<*>>
            }
        }

        suspend fun loadInlineColor(): dynamic {
            styles.loadInlineColor()

            return load("inlineColor", { _, duration -> duration < 100 }) {
                js("import('prismjs/plugins/inline-color/prism-inline-color')") as Promise<Module<*>>
            }
        }

        suspend fun loadLineNumbers(): dynamic {
            styles.loadLineNumbers()

            return load("lineNumbers") {
                js("import('prismjs/plugins/line-numbers/prism-line-numbers')") as Promise<Module<*>>
            }
        }

        suspend fun loadShowLanguage(): dynamic {
            // Depends on the Toolbar plugin to be loaded
            loadToolbar()

            return load("showLanguage", { _, duration -> duration < 10 }) {
                js("import('prismjs/plugins/show-language/prism-show-language')") as Promise<Module<*>>
            }
        }

        suspend fun loadToolbar(): dynamic {
            styles.loadToolbar()

            return load("toolbar") {
                js("import('prismjs/plugins/toolbar/prism-toolbar')") as Promise<Module<*>>
            }
        }

        private suspend fun load(
            plugin: String,
            isLoading: (pluginName: String, duration: Double) -> Boolean = { pluginName, duration ->
                PrismJsDefinition.plugins[pluginName] == undefined && duration < 10_000
            },
            promise: () -> Promise<Module<*>>,
        ): dynamic {

            loaded.getOrPut(plugin) {
                promise()
            }

            val start = Date.now()

            @Suppress("UnsafeCastFromDynamic")
            while (isLoading(plugin, Date.now() - start)) {
                delay(10)
            }

            return PrismJsDefinition.plugins[plugin]
        }
    }

    internal val styles = StylesLoader()
    internal val languages = LanguageLoader()
    internal val plugins = PluginsLoader(styles)
}

val prismJsInternals = PrismJsInternals()
