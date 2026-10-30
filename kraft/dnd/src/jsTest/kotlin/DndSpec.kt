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
import kotlinx.browser.window
import kotlinx.coroutines.delay
import kotlinx.html.FlowContent
import kotlinx.html.Tag
import kotlinx.html.div
import kotlinx.html.style
import org.w3c.dom.Element
import org.w3c.dom.HTMLElement
import org.w3c.dom.events.Event
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
private fun Tag.Zone(
    name: String,
    probe: Probe,
    takes: Boolean,
    style: String,
    failDrop: Boolean = false,
    cancelOnStart: Boolean = false,
    inner: FlowContent.() -> Unit = {},
) = comp(Zone.Props(name, probe, takes, style, failDrop, cancelOnStart, inner)) { Zone(it) }

private class Zone(ctx: Ctx<Props>) : Component<Zone.Props>(ctx) {
    data class Props(
        val name: String,
        val probe: Probe,
        val takes: Boolean,
        val style: String,
        val failDrop: Boolean,
        val cancelOnStart: Boolean,
        val inner: FlowContent.() -> Unit,
    )

    init {
        dropZone<Look>(accepts = { props.takes }) {
            val log: (String) -> Unit = { props.probe.events.add("${props.name}:$it") }
            onDragStart {
                log("start")
                if (props.cancelOnStart) dnd.cancel()
            }
            onDragStop { log("stop") }
            onEnter { log("enter") }
            onLeave { log("leave") }
            onDrop { drag ->
                props.probe.lastPointerType = drag.pointerType
                props.probe.lastSource = drag.source
                log("drop")
                if (props.failDrop) error("a drop handler fails")
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
private fun Tag.Board(probe: Probe, showA: StreamSource<Boolean>, showInner: StreamSource<Boolean>) =
    comp(Board.Props(probe, showA, showInner)) { Board(it) }

private class Board(ctx: Ctx<Props>) : Component<Board.Props>(ctx) {
    data class Props(val probe: Probe, val showA: StreamSource<Boolean>, val showInner: StreamSource<Boolean>)

    private val showA by subscribingTo(props.showA)
    private val showInner by subscribingTo(props.showInner)

    override fun VDom.render() {
        Source(props.probe)

        if (showA) Zone("a", props.probe, takes = true, style = box(100, 0, 100, 100))

        Zone("b", props.probe, takes = false, style = box(100, 150, 100, 100))

        Zone("outer", props.probe, takes = true, style = box(300, 0, 200, 200)) {
            div(classes = "outer-child") { style = box(300, 0, 50, 50) }
            if (showInner) Zone("inner", props.probe, takes = true, style = box(400, 50, 50, 50))
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

@Suppress("TestFunctionName")
private fun Tag.LateZone(probe: Probe, accepting: () -> Boolean) = comp(LateZone.Props(probe, accepting)) { LateZone(it) }

/** A zone at (100, 0) whose `accepts` reads changing state: it refuses at the start and accepts later. */
private class LateZone(ctx: Ctx<Props>) : Component<LateZone.Props>(ctx) {
    data class Props(val probe: Probe, val accepting: () -> Boolean)

    init {
        dropZone<Look>(accepts = { props.accepting() }) {
            onEnter { props.probe.events.add("late:enter") }
            onDrop { props.probe.events.add("late:drop") }
            onDragStop { props.probe.events.add("late:stop") }
        }
    }

    override fun VDom.render() {
        div(classes = "late") { style = box(100, 0, 100, 100) }
    }
}

@Suppress("TestFunctionName")
private fun Tag.NumberZone(probe: Probe) = comp(NumberZone.Props(probe)) { NumberZone(it) }

/** A zone at (100, 0) for numbers: a Look must pass it by. */
private class NumberZone(ctx: Ctx<Props>) : Component<NumberZone.Props>(ctx) {
    data class Props(val probe: Probe)

    init {
        dropZone<Int> {
            onDragStart { props.probe.events.add("number:start") }
            onEnter { props.probe.events.add("number:enter") }
            onDrop { props.probe.events.add("number:drop") }
        }
    }

    override fun VDom.render() {
        div(classes = "numbers") { style = box(100, 0, 100, 100) }
    }
}

// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Klang's equip grid: two slots at (0, 300) and (100, 300), each a source and a zone; a bench zone at (300, 300)
// 150x50 holding its looks as sources, 40 px apart from (300, 300).
// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

private class Equip(slots: List<String?>, bench: List<String>) {
    val slots = StreamSource(slots)
    val bench = StreamSource(bench)
}

/** Where a drag of the grid came from: a slot, or the bench. */
private sealed interface From {
    data class Slot(val index: Int) : From
    data object Bench : From
}

@Suppress("TestFunctionName")
private fun Tag.SlotBox(index: Int, equip: Equip) = comp(SlotBox.Props(index, equip)) { SlotBox(it) }

private class SlotBox(ctx: Ctx<Props>) : Component<SlotBox.Props>(ctx) {
    data class Props(val index: Int, val equip: Equip)

    private val slots by subscribingTo(props.equip.slots)

    init {
        dropZone<String> {
            onDrop { drag ->
                val equip = props.equip
                val next = equip.slots().toMutableList()

                when (val from = drag.source) {
                    // Slot to slot swaps (onto itself: nothing changes)
                    is From.Slot -> next[from.index] = next[props.index].also { next[props.index] = next[from.index] }
                    // Bench to slot equips; the look in the slot goes to the bench
                    else -> {
                        val old = next[props.index]
                        next[props.index] = drag.payload
                        equip.bench(equip.bench() - drag.payload + listOfNotNull(old))
                    }
                }

                equip.slots(next)
            }
        }
    }

    override fun VDom.render() {
        val look = slots[props.index]

        div(classes = "slot-${props.index}") {
            style = box(props.index * 100, 300, 50, 50)

            if (look != null) {
                onPointerDown { event -> dnd.startDrag(event = event, payload = look, source = From.Slot(props.index)) }
                +look
            }
        }
    }
}

@Suppress("TestFunctionName")
private fun Tag.BenchBox(equip: Equip) = comp(BenchBox.Props(equip)) { BenchBox(it) }

private class BenchBox(ctx: Ctx<Props>) : Component<BenchBox.Props>(ctx) {
    data class Props(val equip: Equip)

    private val bench by subscribingTo(props.equip.bench)

    init {
        dropZone<String> {
            // Slot to bench moves the look off the slot
            onDrop { drag ->
                val from = drag.source as? From.Slot ?: return@onDrop
                props.equip.slots(props.equip.slots().toMutableList().also { it[from.index] = null })
                props.equip.bench(props.equip.bench() + drag.payload)
            }
        }
    }

    override fun VDom.render() {
        div(classes = "bench") {
            style = box(300, 300, 150, 50)

            bench.forEachIndexed { i, look ->
                div(classes = "bench-item") {
                    style = box(300 + i * 40, 300, 30, 30)
                    onPointerDown { event -> dnd.startDrag(event = event, payload = look, source = From.Bench) }
                    +look
                }
            }
        }
    }
}

// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Pointer helpers
// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/** A pointer event as a browser sends it: the primary button, held from down to up unless [buttons] says otherwise. */
private fun pointer(
    type: String,
    target: EventTarget,
    x: Int,
    y: Int,
    id: Int = 1,
    pointerType: String = "mouse",
    button: Int = 0,
    buttons: Int = if (type == "pointerup" || type == "pointercancel") 0 else 1,
) {
    val init: dynamic = js("{}")
    init.bubbles = true
    init.cancelable = true
    init.clientX = x
    init.clientY = y
    init.pointerId = id
    init.pointerType = pointerType
    init.isPrimary = true
    init.button = button
    init.buttons = buttons

    target.dispatchEvent(PointerEvent(type, init))
}

private fun at(x: Int, y: Int): Element = document.elementFromPoint(x.toDouble(), y.toDouble()) ?: document.body!!

private suspend fun KQuery<Element>.down(pointerType: String = "mouse", id: Int = 1) =
    pointer("pointerdown", selectCss(".source")[0], 10, 10, id = id, pointerType = pointerType)

private fun move(x: Int, y: Int, id: Int = 1, pointerType: String = "mouse") =
    pointer("pointermove", at(x, y), x, y, id = id, pointerType = pointerType)

private fun up(x: Int, y: Int, id: Int = 1, pointerType: String = "mouse") =
    pointer("pointerup", at(x, y), x, y, id = id, pointerType = pointerType)

private fun escape() {
    val init: dynamic = js("{}")
    init.key = "Escape"
    init.bubbles = true
    init.cancelable = true
    document.body!!.dispatchEvent(KeyboardEvent("keydown", init))
}

private fun click(element: Element) = element.dispatchEvent(MouseEvent("click", js("({bubbles: true})")))

private suspend fun withBoard(
    showInner: StreamSource<Boolean> = StreamSource(true),
    test: suspend KQuery<Element>.(Probe, StreamSource<Boolean>) -> Unit,
) {
    val probe = Probe()
    val showA = StreamSource(true)

    TestBed.preact(
        appSetup = { dnd() },
        view = { Board(probe, showA, showInner) },
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
            escape()

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

    "the drop goes to the zone under the pointer at the release, not at the last move" {
        withBoard { probe, _ ->
            down()
            move(150, 50)
            // The layout moves under a still pointer (a wheel scroll, a re-render): zone a is no longer there
            (selectCss(".zone-a")[0] as HTMLElement).style.display = "none"
            up(150, 50)

            probe.events.contains("a:drop") shouldBe false
            probe.events.filter { it.startsWith("a:") || it == "cancelled" } shouldBe
                listOf("a:start", "a:enter", "a:leave", "cancelled", "a:stop")
        }
    }

    "only the primary button drags: a right press starts nothing" {
        withBoard { probe, _ ->
            pointer("pointerdown", selectCss(".source")[0], 10, 10, button = 2, buttons = 2)
            move(150, 50)
            up(150, 50)

            probe.events shouldBe emptyList()
        }
    }

    "a mouse that moves with no button held lost its release: the drag ends as cancelled" {
        withBoard { probe, _ ->
            down()
            move(150, 50)
            // The release went to a context menu or another window
            pointer("pointermove", at(160, 60), 160, 60, buttons = 0)
            up(160, 60)

            probe.events.filter { it.startsWith("a:") || it == "cancelled" } shouldBe
                listOf("a:start", "a:enter", "a:leave", "cancelled", "a:stop")
        }
    }

    "the browser's own drag is held off while a press is held, and only then" {
        withBoard { _, _ ->
            suspend fun nativeDrag(): Boolean {
                val event = Event("dragstart", js("({bubbles: true, cancelable: true})"))
                selectCss(".source")[0].dispatchEvent(event)
                return event.defaultPrevented
            }

            down()
            nativeDrag() shouldBe true
            move(150, 50)
            nativeDrag() shouldBe true
            up(150, 50)

            nativeDrag() shouldBe false
        }
    }

    "Klang's equip grid: slot to slot swaps, slot to bench moves, bench to slot equips; mouse, touch and pen" {
        listOf("mouse", "touch", "pen").forEach { type ->
            val equip = Equip(slots = listOf("red", "blue"), bench = listOf("green"))

            TestBed.preact(
                appSetup = { dnd() },
                view = { SlotBox(0, equip); SlotBox(1, equip); BenchBox(equip) },
            ) {
                suspend fun drag(fromX: Int, fromY: Int, toX: Int, toY: Int) {
                    pointer("pointerdown", at(fromX, fromY), fromX, fromY, pointerType = type)
                    pointer("pointermove", at(toX, toY), toX, toY, pointerType = type)
                    pointer("pointerup", at(toX, toY), toX, toY, pointerType = type)
                    // Redrawn from the new state before the next drag hits the elements
                    delay(20)
                }

                // Slot 0 onto slot 1: the two swap
                drag(10, 310, 120, 320)
                equip.slots() shouldBe listOf("blue", "red")

                // Slot 1 onto the bench (its background): the look moves off the slot
                drag(110, 310, 430, 330)
                equip.slots() shouldBe listOf("blue", null)
                equip.bench() shouldBe listOf("green", "red")

                // A bench look onto slot 1: equipped, and off the bench
                drag(310, 310, 120, 320)
                equip.slots() shouldBe listOf("blue", "green")
                equip.bench() shouldBe listOf("red")

                // Slot 0 onto itself (the source lies inside its own zone): nothing changes
                drag(10, 310, 30, 330)
                equip.slots() shouldBe listOf("blue", "green")
            }
        }
    }

    "a zone for another payload type hears nothing of the drag" {
        val probe = Probe()

        TestBed.preact(appSetup = { dnd() }, view = { Source(probe); NumberZone(probe) }) { root ->
            root.down()
            move(150, 50)
            up(150, 50)
        }

        probe.events shouldBe listOf("cancelled")
    }

    "a handler that throws is logged: the source still learns the drop, and every zone hears the stop" {
        val probe = Probe()

        TestBed.preact(
            appSetup = { dnd() },
            view = {
                Source(probe)
                Zone("a", probe, takes = true, style = box(100, 0, 100, 100), failDrop = true)
                Zone("c", probe, takes = true, style = box(100, 300, 50, 50))
            },
        ) { root ->
            root.down()
            move(150, 50)
            up(150, 50)
        }

        probe.events.contains("dropped:zone-a") shouldBe true
        probe.events.filter { it.endsWith(":stop") }.toSet() shouldBe setOf("a:stop", "c:stop")
    }

    "a handler that cancels the drag leaves no preview and no current drag" {
        val probe = Probe()

        TestBed.preact(
            appSetup = { dnd() },
            view = {
                Source(probe)
                Zone("a", probe, takes = true, style = box(100, 0, 100, 100), cancelOnStart = true)
            },
        ) { root ->
            root.down()
            move(150, 50)
            move(160, 60)
            delay(20)

            (document.querySelector(".kraft-dnd-stage") == null) shouldBe true
            (app.appAttributes[DndManager.key]!!.current() == null) shouldBe true
            up(160, 60)
        }

        probe.events shouldBe listOf("a:start", "cancelled", "a:stop")
    }

    "after Esc the pointer is still down: the click after its release is swallowed too" {
        withBoard { probe, _ ->
            down()
            move(150, 50)
            escape()
            up(10, 10)
            click(selectCss(".source")[0])

            probe.clicks shouldBe 0
        }
    }

    "a press below the threshold is a click, and the click reaches the element" {
        withBoard { probe, _ ->
            down()
            move(12, 12)
            up(12, 12)
            click(selectCss(".source")[0])

            probe.clicks shouldBe 1
        }
    }

    "the innermost zone wins also when the outer one registered first" {
        val showInner = StreamSource(false)

        withBoard(showInner = showInner) { probe, _ ->
            // Mounted after the outer zone, the inner one registers after it
            showInner(true)
            eventually { selectCss(".zone-inner").size shouldBe 1 }

            down()
            move(420, 70)
            up(420, 70)

            probe.events.contains("inner:drop") shouldBe true
            probe.events.contains("outer:drop") shouldBe false
        }
    }

    "every window listener a drag adds is removed when it ends" {
        val win = window.asDynamic()
        val add = win.addEventListener
        val remove = win.removeEventListener
        val counts = js("({})")
        val counting = js(
            "(function (orig, counts, sign) { return function (type, h, o) { " +
                "counts[type] = (counts[type] || 0) + sign; return orig.call(this, type, h, o); }; })"
        )

        withBoard { _, _ ->
            win.addEventListener = counting(add, counts, 1)
            win.removeEventListener = counting(remove, counts, -1)

            try {
                down()
                move(150, 50)
                up(150, 50)
                // The click swallower goes after its task
                delay(10)
            } finally {
                win.addEventListener = add
                win.removeEventListener = remove
            }
        }

        listOf("pointermove", "pointerup", "pointercancel", "keydown", "dragstart", "click")
            .associateWith { (counts[it] as Int?) ?: 0 } shouldBe
            mapOf("pointermove" to 0, "pointerup" to 0, "pointercancel" to 0, "keydown" to 0, "dragstart" to 0, "click" to 0)
    }

    "a zone that refused at the start is not entered or dropped on later: it would never hear the stop" {
        val probe = Probe()
        var accepting = false

        TestBed.preact(appSetup = { dnd() }, view = { Source(probe); LateZone(probe) { accepting } }) { root ->
            root.down()
            move(50, 50)
            accepting = true
            move(150, 50)
            up(150, 50)
        }

        probe.events shouldBe listOf("cancelled")
    }
})
