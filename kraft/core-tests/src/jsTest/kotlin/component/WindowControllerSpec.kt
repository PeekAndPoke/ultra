package io.peekandpoke.kraft.coretests.component

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.peekandpoke.kraft.components.NoProps
import io.peekandpoke.kraft.components.PureComponent
import io.peekandpoke.kraft.components.comp
import io.peekandpoke.kraft.testing.TestBed
import io.peekandpoke.kraft.testing.TestWindowController
import io.peekandpoke.kraft.testing.eventually
import io.peekandpoke.kraft.testing.selectCss
import io.peekandpoke.kraft.testing.textContent
import io.peekandpoke.kraft.utils.WindowController
import io.peekandpoke.kraft.utils.windowCtrl
import io.peekandpoke.kraft.vdom.VDom
import kotlinx.browser.document
import kotlinx.html.Tag
import kotlinx.html.div
import org.w3c.dom.events.Event

@Suppress("TestFunctionName")
private fun Tag.FocusShowcase() = comp { FocusShowcase(it) }

private class FocusShowcase(ctx: NoProps) : PureComponent(ctx) {
    private val hasFocus by subscribingTo(windowCtrl.hasFocus)
    private val isVisible by subscribingTo(windowCtrl.isVisible)

    override fun VDom.render() {
        div(classes = "focus") { +(if (hasFocus) "working" else "resting") }
        div(classes = "visible") { +(if (isVisible) "shown" else "hidden") }
    }
}

class WindowControllerSpec : StringSpec({

    "every app shares the one default controller" {
        val seen = mutableListOf<WindowController>()

        repeat(3) {
            TestBed.preact(view = { div { } }) {
                seen += app.appAttributes[WindowController.key]!!
            }
        }

        seen.forEach { it shouldBeSameInstanceAs WindowController.default }
    }

    "the default is readable outside any component" {
        WindowController.default.hasFocus() shouldBe document.hasFocus()
    }

    "the default follows visibilitychange into isVisible" {
        val doc = document.asDynamic()
        val controller = WindowController.default
        val defineProperty = js("Object.defineProperty")

        try {
            defineProperty(document, "visibilityState", js("({ configurable: true, get: function() { return 'hidden'; } })"))
            document.dispatchEvent(Event("visibilitychange"))
            controller.isVisible() shouldBe false

            defineProperty(document, "visibilityState", js("({ configurable: true, get: function() { return 'visible'; } })"))
            document.dispatchEvent(Event("visibilitychange"))
            controller.isVisible() shouldBe true
        } finally {
            // Back to the prototype's getter
            js("delete doc.visibilityState")
        }
    }

    "a TestWindowController drives the components by hand" {
        val ctrl = TestWindowController()

        TestBed.preact(
            appSetup = { windowCtrl(ctrl) },
            view = { FocusShowcase() },
        ) { root ->
            root.selectCss(".focus").textContent() shouldBe "working"
            root.selectCss(".visible").textContent() shouldBe "shown"

            ctrl.blur()
            ctrl.visibility(false)

            eventually {
                root.selectCss(".focus").textContent() shouldBe "resting"
                root.selectCss(".visible").textContent() shouldBe "hidden"
            }
        }
    }
})
