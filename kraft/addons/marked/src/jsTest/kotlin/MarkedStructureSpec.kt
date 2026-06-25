package io.peekandpoke.kraft.addons.marked

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.kraft.addons.registry.AddonRegistry.Companion.addons
import io.peekandpoke.kraft.addons.registry.addons
import io.peekandpoke.kraft.components.NoProps
import io.peekandpoke.kraft.components.PureComponent
import io.peekandpoke.kraft.components.comp
import io.peekandpoke.kraft.testing.TestBed
import io.peekandpoke.kraft.testing.selectCss
import io.peekandpoke.kraft.testing.textContent
import io.peekandpoke.kraft.vdom.VDom
import kotlinx.coroutines.delay
import kotlinx.html.Tag
import kotlinx.html.div
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

private suspend fun eventually(
    timeout: Duration = 2.seconds,
    poll: Duration = 25.milliseconds,
    check: suspend () -> Unit,
) {
    val deadline = kotlinx.datetime.Clock.System.now().plus(timeout)
    while (true) {
        try {
            check()
            return
        } catch (e: Throwable) {
            if (kotlinx.datetime.Clock.System.now() > deadline) throw e
            delay(poll)
        }
    }
}

@Suppress("TestFunctionName")
private fun Tag.MarkedStructureComponent() = comp { MarkedStructureComponent(it) }

private class MarkedStructureComponent(ctx: NoProps) : PureComponent(ctx) {
    private val marked by subscribingTo(addons.marked)

    override fun VDom.render() {
        div(classes = "result") {
            val m = marked
            if (m == null) {
                +"loading"
            } else {
                val heading = m.markdown2html("# Hello")
                val bold = m.markdown2html("**bold**")
                val list = m.markdown2html("- a\n- b")
                val link = m.markdown2html("[t](http://example.com)")

                val ok = heading.contains("<h1") && heading.contains("Hello") &&
                        bold.contains("<strong") &&
                        list.contains("<ul") && list.contains("<li") &&
                        link.contains("href=\"http://example.com")

                +(if (ok) "structure-ok" else "FAIL h=[$heading] b=[$bold] l=[$list] link=[$link]")
            }
        }
    }
}

/** Deepens marked coverage: asserts markdown renders to the expected (sanitized) HTML structure. */
class MarkedStructureSpec : StringSpec() {

    init {
        "markdown2html renders headings, bold, lists and links" {
            TestBed.preact(
                appSetup = { addons { marked() } },
                view = { MarkedStructureComponent() },
            ) { root ->
                eventually {
                    root.selectCss(".result").textContent() shouldBe "structure-ok"
                }
            }
        }
    }
}
