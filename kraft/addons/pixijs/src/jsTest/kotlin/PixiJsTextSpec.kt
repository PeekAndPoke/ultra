package io.peekandpoke.kraft.addons.pixijs

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
import io.peekandpoke.kraft.utils.jsObject
import io.peekandpoke.kraft.vdom.VDom
import kotlinx.coroutines.delay
import kotlinx.html.Tag
import kotlinx.html.div
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

private suspend fun eventually(
    timeout: Duration = 5.seconds,
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
private fun Tag.PixiJsTextConsumer() = comp { PixiJsTextConsumer(it) }

private class PixiJsTextConsumer(ctx: NoProps) : PureComponent(ctx) {
    private val pixi by subscribingTo(addons.pixiJs)
    private var result: String by value("waiting")

    init {
        lifecycle {
            onMount { check() }
            onUpdate { check() }
        }
    }

    private fun check() {
        if (result != "waiting") return
        val addon = pixi ?: return

        try {
            val text = addon.createText(jsObject { this.text = "Hello Pixi" })
            result = if (text.text == "Hello Pixi") "ok" else "mismatch:${text.text}"
        } catch (e: Throwable) {
            result = "error: ${e.message}"
        }
        triggerRedraw()
    }

    override fun VDom.render() {
        div(classes = "result") { +result }
    }
}

/** Top-up coverage: PixiJsAddon.createText builds a Text carrying the given string. */
class PixiJsTextSpec : StringSpec() {

    init {
        "createText builds a Text with the given string" {
            TestBed.preact(
                appSetup = { addons { pixiJs() } },
                view = { PixiJsTextConsumer() },
            ) { root ->
                eventually {
                    root.selectCss(".result").textContent() shouldBe "ok"
                }
            }
        }
    }
}
