package io.peekandpoke.kraft.addons.prismjs

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.kraft.components.Component
import io.peekandpoke.kraft.components.Ctx
import io.peekandpoke.kraft.components.comp
import io.peekandpoke.kraft.testing.TestBed
import io.peekandpoke.kraft.testing.eventually
import io.peekandpoke.kraft.testing.selectCss
import io.peekandpoke.kraft.testing.textContent
import io.peekandpoke.kraft.vdom.VDom
import io.peekandpoke.ultra.streams.StreamSource
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.delay
import kotlinx.html.Tag
import kotlinx.html.div
import org.w3c.dom.HTMLElement

private data class Shown(val language: String, val code: String)

// Host that renders a Prism block with whatever the stream holds
@Suppress("TestFunctionName")
private fun Tag.PrismHost(source: StreamSource<Shown>) = comp(PrismHost.Props(source)) { PrismHost(it) }

private class PrismHost(ctx: Ctx<Props>) : Component<PrismHost.Props>(ctx) {

    data class Props(val source: StreamSource<Shown>)

    private val shown by subscribingTo(props.source)

    override fun VDom.render() {
        div(classes = "host") {
            Prism(language = shown.language, code = shown.code, options = {})
        }
    }
}

class PrismComponentSpec : StringSpec() {

    init {
        // First in the spec: Prism's automatic pass runs once, when it loads
        "a language block outside the component is left alone" {
            val foreign = (document.createElement("pre") as HTMLElement).apply {
                innerHTML = """<code class="language-json">{"foreign": 1}</code>"""
            }
            document.body!!.append(foreign)

            try {
                val source = StreamSource(Shown("json", """{"own": 1}"""))

                TestBed.preact(view = { PrismHost(source) }) { root ->
                    eventually { root.selectCss("code .token.property").textContent() shouldBe "\"own\"" }
                    // Prism's automatic pass is due 16 ms after it loads
                    delay(200)
                }

                (window.asDynamic().Prism.manual as Boolean?) shouldBe true
                foreign.querySelectorAll(".token").length shouldBe 0
            } finally {
                foreign.remove()
            }
        }

        "AQL is highlighted" {
            val source = StreamSource(Shown("aql", "FOR d IN users FILTER d.name == @name RETURN d"))

            TestBed.preact(view = { PrismHost(source) }) { root ->
                eventually {
                    root.selectCss("code .token.keyword").count() shouldBe 4
                    root.selectCss("code .token.variable").textContent() shouldBe "@name"
                }
            }
        }

        "new code replaces the highlighted block" {
            val source = StreamSource(Shown("json", """{"first": 1}"""))

            TestBed.preact(view = { PrismHost(source) }) { root ->
                eventually { root.selectCss("code .token.property").textContent() shouldBe "\"first\"" }

                source(Shown("json", """{"second": 2}"""))

                eventually {
                    root.selectCss("code").textContent() shouldBe """{"second": 2}"""
                    root.selectCss("code .token.property").textContent() shouldBe "\"second\""
                }
            }
        }

        "code changed before the first highlight is the code highlighted" {
            val source = StreamSource(Shown("json", """{"early": 1}"""))

            TestBed.preact(view = { PrismHost(source) }) { root ->
                // Synchronously, while the theme and grammar still load
                source(Shown("json", """{"late": 2}"""))

                eventually { root.selectCss("code .token.property").textContent() shouldBe "\"late\"" }
                root.selectCss("code").textContent() shouldBe """{"late": 2}"""
            }
        }

        "markup in the code is text" {
            val payload = """{"q": "<img src=x onerror=alert(1)>"}"""
            val source = StreamSource(Shown("json", payload))

            TestBed.preact(view = { PrismHost(source) }) { root ->
                eventually { root.selectCss("code .token.string").count() shouldBe 1 }

                root.selectCss("img").count() shouldBe 0
                root.selectCss("code").textContent() shouldBe payload
            }
        }
    }
}
