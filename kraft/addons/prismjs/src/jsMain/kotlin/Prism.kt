@file:Suppress("FunctionName", "NOTHING_TO_INLINE")

package de.peekandpoke.kraft.addons.prismjs

import de.peekandpoke.kraft.components.Component
import de.peekandpoke.kraft.components.Ctx
import de.peekandpoke.kraft.components.comp
import de.peekandpoke.kraft.utils.launch
import de.peekandpoke.kraft.vdom.VDom
import kotlinx.browser.document
import kotlinx.dom.clear
import kotlinx.html.Tag
import kotlinx.html.code
import kotlinx.html.div
import kotlinx.html.pre
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLPreElement

typealias PrismOptsBuilder = Prism.OptionsBuilder.() -> Unit

fun Tag.Prism(language: String, code: String, options: PrismOptsBuilder) = comp(
    Prism.Props(
        language = language,
        code = code,
        options = Prism.OptionsBuilder().apply(options).build()
    )
) { Prism(it) }

inline fun Tag.PrismAtom(code: String, noinline options: PrismOptsBuilder = {}) =
    Prism(language = "atom", code = code, options = options)

inline fun Tag.PrismCLike(code: String, noinline options: PrismOptsBuilder = {}) =
    Prism(language = "clike", code = code, options = options)

inline fun Tag.PrismCss(code: String, noinline options: PrismOptsBuilder = {}) =
    Prism(language = "css", code = code, options = options)

inline fun Tag.PrismDart(code: String, noinline options: PrismOptsBuilder = {}) =
    Prism(language = "dart", code = code, options = options)

inline fun Tag.PrismHtml(code: String, noinline options: PrismOptsBuilder = {}) =
    Prism(language = "html", code = code, options = options)

inline fun Tag.PrismKotlin(code: String, noinline options: PrismOptsBuilder = {}) =
    Prism(language = "kotlin", code = code, options = options)

inline fun Tag.PrismKotlinScript(code: String, noinline options: PrismOptsBuilder = {}) =
    Prism(language = "kts", code = code, options = options)

inline fun Tag.PrismJava(code: String, noinline options: PrismOptsBuilder = {}) =
    Prism(language = "java", code = code, options = options)

inline fun Tag.PrismJavascript(code: String, noinline options: PrismOptsBuilder = {}) =
    Prism(language = "javascript", code = code, options = options)

inline fun Tag.PrismJson(code: String, noinline options: PrismOptsBuilder = {}) =
    Prism(language = "json", code = code, options = options)

inline fun Tag.PrismJson5(code: String, noinline options: PrismOptsBuilder = {}) =
    Prism(language = "json5", code = code, options = options)

inline fun Tag.PrismJsonp(code: String, noinline options: PrismOptsBuilder = {}) =
    Prism(language = "jsonp", code = code, options = options)

inline fun Tag.PrismLess(code: String, noinline options: PrismOptsBuilder = {}) =
    Prism(language = "less", code = code, options = options)

inline fun Tag.PrismMarkup(code: String, noinline options: PrismOptsBuilder = {}) =
    Prism(language = "markup", code = code, options = options)

inline fun Tag.PrismPhp(code: String, noinline options: PrismOptsBuilder = {}) =
    Prism(language = "php", code = code, options = options)

inline fun Tag.PrismPlain(code: String, noinline options: PrismOptsBuilder = {}) =
    Prism(language = "plain", code = code, options = options)

inline fun Tag.PrismRegex(code: String, noinline options: PrismOptsBuilder = {}) =
    Prism(language = "regex", code = code, options = options)

inline fun Tag.PrismRuby(code: String, noinline options: PrismOptsBuilder = {}) =
    Prism(language = "ruby", code = code, options = options)

inline fun Tag.PrismRust(code: String, noinline options: PrismOptsBuilder = {}) =
    Prism(language = "rust", code = code, options = options)

inline fun Tag.PrismRss(code: String, noinline options: PrismOptsBuilder = {}) =
    Prism(language = "rss", code = code, options = options)

inline fun Tag.PrismSass(code: String, noinline options: PrismOptsBuilder = {}) =
    Prism(language = "sass", code = code, options = options)

inline fun Tag.PrismScss(code: String, noinline options: PrismOptsBuilder = {}) =
    Prism(language = "scss", code = code, options = options)

inline fun Tag.PrismSsml(code: String, noinline options: PrismOptsBuilder = {}) =
    Prism(language = "ssml", code = code, options = options)

inline fun Tag.PrismSvg(code: String, noinline options: PrismOptsBuilder = {}) =
    Prism(language = "svg", code = code, options = options)

inline fun Tag.PrismText(code: String, noinline options: PrismOptsBuilder = {}) =
    Prism(language = "text", code = code, options = options)

inline fun Tag.PrismTypescript(code: String, noinline options: PrismOptsBuilder = {}) =
    Prism(language = "typescript", code = code, options = options)

inline fun Tag.PrismXml(code: String, noinline options: PrismOptsBuilder = {}) =
    Prism(language = "xml", code = code, options = options)

class Prism(ctx: Ctx<Props>) : Component<Prism.Props>(ctx) {

    //  PROPS  //////////////////////////////////////////////////////////////////////////////////////////////////

    data class Props(
        val language: String,
        val code: String,
        val options: Options,
    )

    data class Options(
        val plugins: List<PrismPlugin>,
    )

    class OptionsBuilder internal constructor() {
        private val plugins = mutableListOf<PrismPlugin>()

        internal fun build() = Options(
            plugins = plugins,
        )

        fun usePlugin(plugin: PrismPlugin) {
            plugins.add(plugin)
        }
    }

    //  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    //  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    init {
        launch {
            prismJsInternals.styles.loadDefaultTheme()

            props.options.plugins.forEach { it.load() }

            prismJsInternals.languages.load(props.language)

            createContent()
        }
    }

    private fun createContent() {

        dom?.let { d ->
            val pre = (document.createElement("pre") as HTMLPreElement).also { pre ->

                pre.className = "language-${props.language} copy-to-clipboard"

                props.options.plugins.forEach { it.applyTo(pre) }

                (document.createElement("code") as HTMLElement).also { code ->

                    val text = document.createTextNode(props.code)

                    code.append(text)
                    pre.append(code)
                }
            }

            d.clear()
            d.append(pre)

            PrismJsDefinition.highlightAllUnder(d)
        }
    }

    override fun VDom.render() {
        // Initially we create a placeholder which is filled by [createContent]
        div("prism") {

            pre("language-${props.language}") {
                code {
                    +props.code
                }
            }
        }
    }
}
