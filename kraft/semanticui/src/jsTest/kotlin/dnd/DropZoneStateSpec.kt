package io.peekandpoke.kraft.semanticui.dnd

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.kraft.components.NoProps
import io.peekandpoke.kraft.components.PureComponent
import io.peekandpoke.kraft.components.comp
import io.peekandpoke.kraft.dnd.DndManager.Companion.dnd
import io.peekandpoke.kraft.dnd.dnd
import io.peekandpoke.kraft.dnd.dropZone
import io.peekandpoke.kraft.testing.TestBed
import io.peekandpoke.kraft.testing.eventually
import io.peekandpoke.kraft.vdom.VDom
import io.peekandpoke.ultra.html.onPointerDown
import kotlinx.browser.document
import kotlinx.html.Tag
import kotlinx.html.div
import kotlinx.html.style
import org.w3c.dom.events.EventTarget
import org.w3c.dom.pointerevents.PointerEvent

private fun box(left: Int, top: Int): String =
    "position: fixed; left: ${left}px; top: ${top}px; width: 100px; height: 100px; touch-action: none;"

private fun pointer(type: String, target: EventTarget, x: Int, y: Int) {
    val init: dynamic = js("{}")
    init.bubbles = true
    init.cancelable = true
    init.clientX = x
    init.clientY = y
    init.pointerId = 1
    init.pointerType = "mouse"
    init.isPrimary = true
    init.button = 0
    init.buttons = if (type == "pointerup") 0 else 1
    target.dispatchEvent(PointerEvent(type, init))
}

private fun at(x: Int, y: Int) = document.elementFromPoint(x.toDouble(), y.toDouble()) ?: document.body!!

@Suppress("TestFunctionName")
private fun Tag.Number() = comp { Number(it) }

private class Number(ctx: NoProps) : PureComponent(ctx) {
    override fun VDom.render() {
        div(classes = "number") {
            style = box(0, 0)
            onPointerDown { event -> dnd.startDrag(event = event, payload = 7) }
        }
    }
}

@Suppress("TestFunctionName")
private fun Tag.Counter() = comp { Counter(it) }

private class Counter(ctx: NoProps) : PureComponent(ctx) {
    private val zone = DropZoneState()

    init {
        dropZone<Int> { track(zone) { triggerRedraw() } }
    }

    override fun VDom.render() {
        div(classes = "counter ${zone.semanticClasses.joinToString(" ")}") { style = box(200, 0) }
    }
}

/** `DropZoneState` and `track` (ULTRA-00265): the zone's Fomantic classes follow the drag. */
class DropZoneStateSpec : StringSpec({

    "secondary while the zone can take the drag, green while it is over it, none after" {
        TestBed.preact(appSetup = { dnd() }, view = { Number(); Counter() }) {
            fun classes() = document.querySelector(".counter")!!.className.split(" ").filter { it.isNotBlank() }.toSet()

            classes() shouldBe setOf("counter")

            pointer("pointerdown", document.querySelector(".number")!!, 10, 10)
            pointer("pointermove", at(50, 50), 50, 50)
            eventually { classes() shouldBe setOf("counter", "secondary") }

            pointer("pointermove", at(250, 50), 250, 50)
            eventually { classes() shouldBe setOf("counter", "green") }

            pointer("pointerup", at(250, 50), 250, 50)
            eventually { classes() shouldBe setOf("counter") }
        }
    }
})
