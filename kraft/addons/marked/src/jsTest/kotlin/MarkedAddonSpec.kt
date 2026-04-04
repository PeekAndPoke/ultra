package io.peekandpoke.kraft.addons.marked

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
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
import kotlinx.html.unsafe
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/** Polls until [check] passes or [timeout] is reached. */
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

// Component that renders "# Hello" markdown via the marked addon
@Suppress("TestFunctionName")
private fun Tag.HelloMarkdown() = comp { HelloMarkdownComponent(it) }

private class HelloMarkdownComponent(ctx: NoProps) : PureComponent(ctx) {
    private val markedAddon by subscribingTo(addons.marked)

    override fun VDom.render() {
        div(classes = "output") {
            val addon = markedAddon
            if (addon != null) {
                unsafe { +addon.markdown2html("# Hello") }
            } else {
                +"loading"
            }
        }
    }
}

// Component that renders XSS-containing markdown via the marked addon
@Suppress("TestFunctionName")
private fun Tag.XssMarkdown() = comp { XssMarkdownComponent(it) }

private class XssMarkdownComponent(ctx: NoProps) : PureComponent(ctx) {
    private val markedAddon by subscribingTo(addons.marked)

    override fun VDom.render() {
        div(classes = "output") {
            val addon = markedAddon
            if (addon != null) {
                unsafe { +addon.markdown2html("<script>alert('xss')</script>") }
            } else {
                +"loading"
            }
        }
    }
}

class MarkedAddonSpec : StringSpec() {

    init {
        "marked addon loads and provides MarkedAddon" {

            TestBed.preact(
                appSetup = {
                    addons {
                        marked()
                    }
                },
                view = {
                    HelloMarkdown()
                },
            ) { root ->
                eventually {
                    val html = root.selectCss(".output").textContent()
                    html shouldContain "Hello"
                }
            }
        }

        "markdown2html sanitizes XSS" {

            TestBed.preact(
                appSetup = {
                    addons {
                        marked()
                    }
                },
                view = {
                    XssMarkdown()
                },
            ) { root ->
                eventually {
                    val html = root.selectCss(".output").textContent()
                    html shouldNotContain "<script>"
                    html shouldNotContain "alert('xss')"
                }
            }
        }
    }
}
