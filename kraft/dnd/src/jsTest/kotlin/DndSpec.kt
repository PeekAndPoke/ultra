package io.peekandpoke.kraft.dnd

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.kraft.components.Component
import io.peekandpoke.kraft.components.Ctx
import io.peekandpoke.kraft.components.comp
import io.peekandpoke.kraft.dnd.DndManager.Companion.dnd
import io.peekandpoke.kraft.testing.KQuery
import io.peekandpoke.kraft.testing.TestBed
import io.peekandpoke.kraft.testing.eventually
import io.peekandpoke.kraft.testing.selectCss
import io.peekandpoke.kraft.vdom.VDom
import io.peekandpoke.ultra.html.onClick
import io.peekandpoke.ultra.html.onPointerDown
import io.peekandpoke.ultra.streams.StreamSource
import kotlinx.browser.document
import kotlinx.coroutines.delay
import kotlinx.html.FlowContent
import kotlinx.html.Tag
import kotlinx.html.div
import kotlinx.html.style
import org.w3c.dom.Element
import org.w3c.dom.HTMLElement
import org.w3c.dom.events.EventTarget
import org.w3c.dom.events.KeyboardEvent
import org.w3c.dom.events.MouseEvent
import org.w3c.dom.pointerevents.PointerEvent

// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Fixture: a source at (0, 0) 40x40, zone "a" at (100, 0) 100x100, zone "b" at (100, 150) 100x100 that takes nothing,
// zone "outer" at (300, 0) 200x200 holding a plain child (300..350) and zone "inner" at (400, 50) 50x50.
// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

private data class Look(val name: String)

private class Probe {
    val events = mutableListOf<String>()
    var clicks = 0
    var lastPointerType: String? = null
    var lastSource: Any? = null
}

private fun box(left: Int, top: Int, width: Int, height: Int): String =
    "position: fixed; left: ${left}px; top: ${top}px; width: ${width}px; height: ${height}px; touch-action: none;"

@Suppress("TestFunctionName")
private fun Tag.Source(probe: Probe) = comp(Source.Props(probe)) { Source(it) }

private class Source(ctx: Ctx<Props>) : Component<Source.Props>(ctx) {
    data class Props(val probe: Probe)

    override fun VDom.render() {
        div(classes = "source") {
            style = box(0, 0, 40, 40)

            onClick { props.probe.clicks++ }

            onPointerDown { event ->
                dnd.startDrag(
                    event = event,
                    payload = Look("red"),
                    source = "bench",
                    preview = { div(classes = "preview") { +"red" } },
                )
                    .onDropped { zone -> props.probe.events.add("dropped:${(zone.element() as HTMLElement).className}") }
                    .onCancelled { props.probe.events.add("cancelled") }
            }
        }
    }
}

@Suppress("TestFunctionName")
private fun Tag.Zone(name: String, probe: Probe, takes: Boolean, style: String, inner: FlowContent.() -> Unit = {}) =
    comp(Zone.Props(name, probe, takes, style, inner)) { Zone(it) }

private class Zone(ctx: Ctx<Props>) : Component<Zone.Props>(ctx) {
    data class Props(
        val name: String,
        val probe: Probe,
        val takes: Boolean,
        val style: String,
        val inner: FlowContent.() -> Unit,
    )

    init {
        dropZone<Look>(accepts = { props.takes }) {
            val log: (String) -> Unit = { props.probe.events.add("${props.name}:$it") }
            onDragStart { log("start") }
            onDragStop { log("stop") }
            onEnter { log("enter") }
            onLeave { log("leave") }
            onDrop { drag ->
                props.probe.lastPointerType = drag.pointerType
                props.probe.lastSource = drag.source
                log("drop")
            }
        }
    }

    override fun VDom.render() {
        div(classes = "zone-${props.name}") {
            style = props.style
            props.inner(this)
        }
    }
}

@Suppress("TestFunctionName")
private fun Tag.Board(probe: Probe, showA: StreamSource<Boolean>) = comp(Board.Props(probe, showA)) { Board(it) }

private class Board(ctx: Ctx<Props>) : Component<Board.Props>(ctx) {
    data class Props(val probe: Probe, val showA: StreamSource<Boolean>)

    private val showA by subscribingTo(props.showA)

    override fun VDom.render() {
        Source(props.probe)

        if (showA) Zone("a", props.probe, takes = true, style = box(100, 0, 100, 100))

        Zone("b", props.probe, takes = false, style = box(100, 150, 100, 100))

        Zone("outer", props.probe, takes = true, style = box(300, 0, 200, 200)) {
            div(classes = "outer-child") { style = box(300, 0, 50, 50) }
            Zone("inner", props.probe, takes = true, style = box(400, 50, 50, 50))
        }
    }
}

@Suppress("TestFunctionName")
private fun Tag.StackedZone(probe: Probe) = comp(StackedZone.Props(probe)) { StackedZone(it) }

/** A zone at (100, 0) whose handlers are registered twice, as a helper and the component would. */
private class StackedZone(ctx: Ctx<Props>) : Component<StackedZone.Props>(ctx) {
    data class Props(val probe: Probe)

    init {
        dropZone<Look> {
            onEnter { props.probe.events.add("helper:enter") }
            onEnter { props.probe.events.add("own:enter") }
            onDrop { props.probe.events.add("helper:drop") }
            onDrop { props.probe.events.add("own:drop") }
        }
    }

    override fun VDom.render() {
        div(classes = "stacked") { style = box(100, 0, 100, 100) }
    }
}

// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Pointer helpers
// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

private fun pointer(type: String, target: EventTarget, x: Int, y: Int, id: Int = 1, pointerType: String = "mouse") {
    val init: dynamic = js("{}")
    init.bubbles = true
    init.cancelable = true
    init.clientX = x
    init.clientY = y
    init.pointerId = id
    init.pointerType = pointerType
    init.isPrimary = true

    target.dispatchEvent(PointerEvent(type, init))
}

private fun at(x: Int, y: Int): Element = document.elementFromPoint(x.toDouble(), y.toDouble()) ?: document.body!!

private suspend fun KQuery<Element>.down(pointerType: String = "mouse", id: Int = 1) =
    pointer("pointerdown", selectCss(".source")[0], 10, 10, id = id, pointerType = pointerType)

private fun move(x: Int, y: Int, id: Int = 1, pointerType: String = "mouse") =
    pointer("pointermove", at(x, y), x, y, id = id, pointerType = pointerType)

private fun up(x: Int, y: Int, id: Int = 1) = pointer("pointerup", at(x, y), x, y, id = id)

private suspend fun withBoard(test: suspend KQuery<Element>.(Probe, StreamSource<Boolean>) -> Unit) {
    val probe = Probe()
    val showA = StreamSource(true)

    TestBed.preact(
        appSetup = { dnd() },
        view = { Board(probe, showA) },
    ) { root ->
        root.test(probe, showA)
    }
}

/** `kraft:dnd` (ULTRA-00265): the manager, its stage and drop zones registered by components. */
class DndSpec : StringSpec({

    "a press that moves less than the threshold is a click: no zone hears anything" {
        withBoard { probe, _ ->
            down()
            move(12, 12)
            up(12, 12)

            probe.events shouldBe emptyList()
        }
    }

    "a drag onto a zone: start, enter, drop, stop in order; the source learns the zone" {
        withBoard { probe, _ ->
            down()
            move(50, 50)
            move(150, 50)
            up(150, 50)

            // Every accepting zone hears the start first and the stop last, in no promised order among them
            probe.events.take(3).toSet() shouldBe setOf("a:start", "outer:start", "inner:start")
            probe.events.subList(3, 6) shouldBe listOf("a:enter", "a:drop", "dropped:zone-a")
            probe.events.drop(6).toSet() shouldBe setOf("a:stop", "outer:stop", "inner:stop")
            probe.events.size shouldBe 9
            probe.lastSource shouldBe "bench"
        }
    }

    "a zone that does not accept hears nothing and is not dropped on" {
        withBoard { probe, _ ->
            down()
            move(150, 200)
            up(150, 200)

            probe.events.none { it.startsWith("b:") } shouldBe true
            probe.events.contains("cancelled") shouldBe true
        }
    }

    "the innermost zone wins; a plain child of a zone gives no leave and enter" {
        withBoard { probe, _ ->
            down()
            move(320, 20)
            move(330, 30)
            move(420, 70)
            up(420, 70)

            val moves = probe.events.filter { it.endsWith(":enter") || it.endsWith(":leave") }
            moves shouldBe listOf("outer:enter", "outer:leave", "inner:enter")
            probe.events.contains("inner:drop") shouldBe true
        }
    }

    "leaving a zone and releasing outside cancels: leave, cancelled, stop" {
        withBoard { probe, _ ->
            down()
            move(150, 50)
            move(250, 300)
            up(250, 300)

            probe.events.filter { it.startsWith("a:") || it == "cancelled" } shouldBe
                listOf("a:start", "a:enter", "a:leave", "cancelled", "a:stop")
        }
    }

    "pointercancel and Esc cancel over a zone: leave, cancelled, stop" {
        withBoard { probe, _ ->
            down()
            move(150, 50)
            pointer("pointercancel", at(150, 50), 150, 50)

            probe.events.filter { it.startsWith("a:") || it == "cancelled" } shouldBe
                listOf("a:start", "a:enter", "a:leave", "cancelled", "a:stop")
        }

        withBoard { probe, _ ->
            down()
            move(150, 50)

            val init: dynamic = js("{}")
            init.key = "Escape"
            init.bubbles = true
            init.cancelable = true
            document.body!!.dispatchEvent(KeyboardEvent("keydown", init))

            probe.events.filter { it.startsWith("a:") || it == "cancelled" } shouldBe
                listOf("a:start", "a:enter", "a:leave", "cancelled", "a:stop")
        }
    }

    "after a drag no listener is left: another move or release changes nothing" {
        withBoard { probe, _ ->
            down()
            move(150, 50)
            up(150, 50)
            val after = probe.events.toList()

            move(160, 60)
            up(160, 60)

            probe.events shouldBe after
        }
    }

    "a zone unmounted during the drag hears no more, and the drop misses it" {
        withBoard { probe, showA ->
            down()
            move(150, 50)
            showA(false)
            eventually { selectCss(".zone-a").size shouldBe 0 }

            move(160, 60)
            up(160, 60)

            probe.events.filter { it.startsWith("a:") } shouldBe listOf("a:start", "a:enter")
            probe.events.contains("cancelled") shouldBe true
        }
    }

    "touch and pen drag like the mouse" {
        listOf("touch", "pen").forEach { type ->
            withBoard { probe, _ ->
                down(pointerType = type)
                move(150, 50, pointerType = type)
                up(150, 50)

                probe.events.contains("a:drop") shouldBe true
                probe.lastPointerType shouldBe type
            }
        }
    }

    "a press still pending when the app goes ends with it: no listener outlives the app" {
        val probe = Probe()

        withBoard { p, _ ->
            down()
            // The app is torn down with the pointer still down
            p.events.clear()
        }

        // The old press must not start on this pointer's moves: a started drag would take the page's text selection
        withBoard { p, _ ->
            move(150, 50)
            (document.documentElement!!.asDynamic().style.getPropertyValue("user-select") as String) shouldBe ""
            up(150, 50)
            p.events shouldBe emptyList()
        }

        probe.events shouldBe emptyList()
    }

    "another pointer does not move the drag" {
        withBoard { probe, _ ->
            down(id = 1)
            move(150, 50, id = 2)
            up(150, 50, id = 2)

            probe.events shouldBe emptyList()
        }
    }

    "the preview follows the pointer on the stage, above the modals, and goes with the drag" {
        withBoard { _, _ ->
            down()
            move(150, 60)

            val stage = eventually { document.querySelector(".kraft-dnd-stage") as HTMLElement? ?: error("no stage") }
            // At the pointer, shifted by the grabbed share (10 of 40 px: a quarter) of the preview's own size
            stage.style.left shouldBe "150px"
            stage.style.top shouldBe "60px"
            stage.style.transform shouldBe "translate(-25%, -25%)"
            stage.style.zIndex shouldBe "20000"
            stage.textContent shouldBe "red"

            up(150, 60)
            eventually { (document.querySelector(".kraft-dnd-stage") == null) shouldBe true }
        }
    }

    "handlers stack: every one added runs, in the order added" {
        val probe = Probe()

        TestBed.preact(appSetup = { dnd() }, view = { Source(probe); StackedZone(probe) }) { root ->
            root.down()
            move(150, 50)
            up(150, 50)
        }

        probe.events.filter { !it.startsWith("dropped") } shouldBe listOf("helper:enter", "own:enter", "helper:drop", "own:drop")
    }

    "no text selection on the page while a drag runs, and back after it" {
        withBoard { _, _ ->
            val style = document.documentElement!!.asDynamic().style

            fun userSelect() = style.getPropertyValue("user-select") as String
            val seen = mutableListOf<String>()

            seen.add(userSelect())
            down()
            move(12, 12)
            seen.add(userSelect())
            move(150, 50)
            seen.add(userSelect())
            up(150, 50)
            seen.add(userSelect())

            seen shouldBe listOf("", "", "none", "")
        }
    }

    "the click after a drag is swallowed; a plain click is not" {
        withBoard { probe, _ ->
            down()
            move(150, 50)
            up(150, 50)
            selectCss(".source")[0].dispatchEvent(MouseEvent("click", js("({bubbles: true})")))
            probe.clicks shouldBe 0

            delay(10)
            selectCss(".source")[0].dispatchEvent(MouseEvent("click", js("({bubbles: true})")))
            probe.clicks shouldBe 1
        }
    }
})
