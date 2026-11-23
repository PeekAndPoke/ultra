package io.peekandpoke.kraft.addons.signaturepad

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.kraft.addons.registry.addons
import io.peekandpoke.kraft.components.Component
import io.peekandpoke.kraft.components.Ctx
import io.peekandpoke.kraft.components.comp
import io.peekandpoke.kraft.testing.TestBed
import io.peekandpoke.kraft.testing.eventually
import io.peekandpoke.kraft.vdom.VDom
import io.peekandpoke.ultra.streams.StreamSource
import kotlinx.browser.window
import kotlinx.coroutines.delay
import kotlinx.html.Tag
import kotlinx.html.div
import kotlinx.html.style
import org.w3c.dom.events.Event
import kotlin.time.Duration.Companion.seconds

@Suppress("TestFunctionName")
private fun Tag.ToggledPad(shown: StreamSource<Boolean>, onChange: (SignaturePad) -> Unit) =
    comp(ToggledPad.Props(shown, onChange)) { ToggledPad(it) }

/** A signature pad while [Props.shown] holds true. */
private class ToggledPad(ctx: Ctx<Props>) : Component<ToggledPad.Props>(ctx) {
    data class Props(val shown: StreamSource<Boolean>, val onChange: (SignaturePad) -> Unit)

    private val shown by subscribingTo(props.shown)

    override fun VDom.render() {
        div {
            style = "width: 200px; height: 100px;"
            if (shown) SignaturePad(onChange = props.onChange)
        }
    }
}

/** The pad's window listener goes with the pad (ULTRA-00350). */
class SignaturePadLifecycleSpec : StringSpec({

    "after unmount a window resize no longer reaches the pad or its onChange" {
        val shown = StreamSource(true)
        var changes = 0

        TestBed.preact(appSetup = { addons { signaturePad() } }, view = { ToggledPad(shown) { changes++ } }) {
            // Mounted: a resize redraws the pad and reports it
            eventually(timeout = 5.seconds) {
                window.dispatchEvent(Event("resize"))
                (changes > 0) shouldBe true
            }

            shown(false)
            delay(50)
            val before = changes

            window.dispatchEvent(Event("resize"))
            changes shouldBe before
        }
    }
})
