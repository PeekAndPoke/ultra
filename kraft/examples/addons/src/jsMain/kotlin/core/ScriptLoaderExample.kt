package de.peekandpoke.kraft.examples.jsaddons.core

import de.peekandpoke.kraft.components.NoProps
import de.peekandpoke.kraft.components.PureComponent
import de.peekandpoke.kraft.components.comp
import de.peekandpoke.kraft.components.onClick
import de.peekandpoke.kraft.semanticui.icon
import de.peekandpoke.kraft.semanticui.ui
import de.peekandpoke.kraft.utils.ScriptLoader
import de.peekandpoke.kraft.utils.launch
import de.peekandpoke.kraft.vdom.VDom
import kotlinx.browser.window
import kotlinx.coroutines.asDeferred
import kotlinx.html.*
import kotlin.js.Promise

@Suppress("FunctionName")
fun Tag.ScriptLoaderExample() = comp {
    ScriptLoaderExample(it)
}

class ScriptLoaderExample(ctx: NoProps) : PureComponent(ctx) {

    //  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    private val javascripts by subscribingTo(ScriptLoader.javascripts)

    private var unifiedResult: String? by value("")

    //  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    override fun VDom.render() {

        link {
            rel = "stylesheet"
            href = "https://cdn.jsdelivr.net/npm/katex@0.16.2/dist/katex.min.css"
            attributes["crossorigin"] = "anonymous"
        }

        ui.header H2 { +"KRAFT Core examples" }

        ui.header H3 { +"Script Loader" }

        p { +"Loaded scripts" }

        ui.celled.divided.table Table {
            thead {
                tr {
                    td { +"Type" }
                    td { +"Source" }
                    td { }
                }
            }

            tbody {
                javascripts.forEach { script ->
                    tr {
                        td { +script.type }
                        td { +script.src }
                        td {
                            ui.icon.button {
                                onClick {
                                    ScriptLoader.unload(script)
                                }
                                icon.trash()
                            }
                        }
                    }
                }
            }
        }

        p { +"Load scripts" }

        ui.button {
            onClick {
                launch {
                    val result = ScriptLoader.loadJavascript("https://js.stripe.com/v3/").await()

                    console.log("Stripe loaded", result, window.asDynamic().Stripe)
                }
            }
            +"Load Stripe"
        }

        ui.button {
            onClick {
                launch {
                    ScriptLoader.load(
                        ScriptLoader.Javascript.Default(
                            src = "https://code.jquery.com/jquery-3.6.1.min.js",
                            integrity = "sha256-o88AwQnZB+VDvE9tvIXrMQaPlFFSUTR+nldQm1LuPXQ=",
                            crossOrigin = "anonymous"

                        )
                    ).await()
                }
            }
            +"Load jQuery"
        }

        ui.button {
            onClick {
                launch {
                    val unified = ScriptLoader.load(
                        ScriptLoader.Javascript.Module<UnifiedModule>(src = "https://esm.sh/unified@11?bundle")
                    ).await()

                    console.log("Unified", unified)

                    val remarkParse = ScriptLoader.load(
                        ScriptLoader.Javascript.Module<dynamic>(src = "https://esm.sh/remark-parse@11?bundle")
                    ).await()

                    console.log("Remark Parse", remarkParse)

                    val remarkMath = ScriptLoader.load(
                        ScriptLoader.Javascript.Module<dynamic>(src = "https://esm.sh/remark-math@6?bundle")
                    ).await()

                    console.log("Remark Math", remarkMath)

                    val remarkRehype = ScriptLoader.load(
                        ScriptLoader.Javascript.Module<dynamic>(src = "https://esm.sh/remark-rehype@11?bundle")
                    ).await()

                    console.log("Remark Rehype", remarkRehype)

                    val rehypeKatex = ScriptLoader.load(
                        ScriptLoader.Javascript.Module<dynamic>(src = "https://esm.sh/rehype-katex@7?bundle")
                    ).await()

                    console.log("rehype-katex", rehypeKatex)

                    val rehypeParse = ScriptLoader.load(
                        ScriptLoader.Javascript.Module<dynamic>(src = "https://esm.sh/rehype-parse@9?bundle")
                    ).await()

                    console.log("rehype-parse", rehypeParse)

                    val rehypeFormat = ScriptLoader.load(
                        ScriptLoader.Javascript.Module<dynamic>(src = "https://esm.sh/rehype-format@5?bundle")
                    ).await()

                    console.log("rehype-format", rehypeFormat)

                    val rehypeStringify = ScriptLoader.load(
                        ScriptLoader.Javascript.Module<dynamic>(src = "https://esm.sh/rehype-stringify@10?bundle")
                    ).await()

                    console.log("rehype-stringify", rehypeStringify)

                    val content = """
                        # Hello World
                        
                        - this
                        - is
                        - an
                        - example
                        
                        ## Here are some formulas:
                        
                        Here is some inline math: \( E = mc^2 \)

                        And a display math block:

                        \\[
                        \\int_0^\\infty e^{-x} \\,dx = 1
                        \\]
                        
                        
                        \\( F(0) = 0 \\) - \\( F(1) = 1 \\) - \\( F(n) = F(n-1) + F(n-2) \\)

                        \\[ F(n) = \\frac{\\phi^n - \\psi^n}{\\sqrt{5}} \\]
                        
                        ## Here are some more formulas with $$ delimiters
                        
                        
                        Lift (${"$$"}L${"$$"}) can be determined by Lift Coefficient (${"$$"}C_L${"$$"}) like the following
                        equation.

                        ${"$$"}
                        L = \frac{1}{2} \rho v^2 S C_L
                        ${"$$"}
                                                
                    """.trimIndent()

                    console.log("Content", content)

                    fun convertMathDelimiters(markdown: String): String {
                        return markdown
                            // Replace inline math: \\( ... \\) → $ ... $
                            .replace(Regex("""\\\\\((.+?)\\\\\)""")) { matchResult ->
                                "$" + matchResult.groupValues[1] + "$"
                            }
                            // Replace inline math: \( ... \) → $ ... $
                            .replace(Regex("""\\\((.+?)\\\)""")) { matchResult ->
                                "$" + matchResult.groupValues[1] + "$"
                            }
                            // Replace display math: \\[ ... \\] → $$ ... $$
                            .replace(Regex("""\\\\\[([\s\S]+?)\\\\\]""")) { matchResult ->
                                "$$" + matchResult.groupValues[1] + "$$"
                            }

                            // Replace display math: \[ ... \] → $$ ... $$
                            .replace(Regex("""\\\[([\s\S]+?)\\\]""")) { matchResult ->
                                "$$" + matchResult.groupValues[1] + "$$"
                            }
                    }

                    val contentWithConvertedMathDelimiters = convertMathDelimiters(content)

                    console.log("Content with converted math delimiters", contentWithConvertedMathDelimiters)

                    val promise = unified.module.unified()
                        .use(remarkParse.module.default)
                        .use(remarkMath.module.default)
                        .use(remarkRehype.module.default)
                        .use(rehypeKatex.module.default)
                        .use(rehypeStringify.module.default)
                        .process(contentWithConvertedMathDelimiters)

                    console.log("Promise", promise)

                    val result = promise.asDeferred().await().also { console.log("Result", it) }

                    unifiedResult = result.value

                }
            }
            +"Load 'unified' as module"
        }

        unifiedResult?.let {
            ui.segment {
                unsafe { +it }
            }
        }
    }
}


external object UnifiedModule {
    fun unified(): Unified

    interface Unified {
        fun use(fn: dynamic): Unified
        fun process(source: String): Promise<UnifiedResult>
    }

    interface UnifiedResult {
        var value: String
    }
}
