package io.peekandpoke.kraft.addons.signaturepad

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
import kotlinx.browser.document
import kotlinx.coroutines.delay
import kotlinx.html.Tag
import kotlinx.html.div
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLCanvasElement
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
private fun Tag.SigPadFacadeComponent() = comp { SigPadFacadeComponent(it) }

private class SigPadFacadeComponent(ctx: NoProps) : PureComponent(ctx) {
    private val sp by subscribingTo(addons.signaturePad)

    override fun VDom.render() {
        div(classes = "result") {
            val addon = sp
            if (addon == null) {
                +"loading"
            } else {
                val canvas = document.createElement("canvas") as HTMLCanvasElement
                canvas.width = 200
                canvas.height = 100

                val pad = addon.create(canvas)
                val empty = pad.isEmpty()   // no strokes → empty
                pad.clear()
                val afterClear = pad.isEmpty()

                // draw a 100x50 opaque rect, then trim — trim-canvas crops to the content bounds
                val c2d = canvas.getContext("2d") as CanvasRenderingContext2D
                c2d.asDynamic().fillStyle = "black"
                c2d.fillRect(40.0, 20.0, 100.0, 50.0)
                val trimmed = addon.trimCanvas(canvas)

                +"empty=$empty afterClear=$afterClear trim=${trimmed.width}x${trimmed.height}"
            }
        }
    }
}

/** Covers the SignaturePadAddon facade: create()/isEmpty()/clear() and trimCanvas() crop-to-bounds. */
class SignaturePadFacadeSpec : StringSpec() {

    init {
        "create() yields an empty pad; clear() and trimCanvas() work" {
            TestBed.preact(
                appSetup = { addons { signaturePad() } },
                view = { SigPadFacadeComponent() },
            ) { root ->
                eventually {
                    root.selectCss(".result").textContent() shouldBe "empty=true afterClear=true trim=100x50"
                }
            }
        }
    }
}
