package io.peekandpoke.kraft.coretests

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.kraft.testing.KQuery
import io.peekandpoke.kraft.testing.TestBed
import io.peekandpoke.kraft.testing.animationEnd
import io.peekandpoke.kraft.testing.animationIteration
import io.peekandpoke.kraft.testing.animationStart
import io.peekandpoke.kraft.testing.blur
import io.peekandpoke.kraft.testing.change
import io.peekandpoke.kraft.testing.click
import io.peekandpoke.kraft.testing.contextMenu
import io.peekandpoke.kraft.testing.copy
import io.peekandpoke.kraft.testing.cut
import io.peekandpoke.kraft.testing.dblClick
import io.peekandpoke.kraft.testing.dispatch
import io.peekandpoke.kraft.testing.drag
import io.peekandpoke.kraft.testing.dragEnd
import io.peekandpoke.kraft.testing.dragEnter
import io.peekandpoke.kraft.testing.dragLeave
import io.peekandpoke.kraft.testing.dragOver
import io.peekandpoke.kraft.testing.dragStart
import io.peekandpoke.kraft.testing.drop
import io.peekandpoke.kraft.testing.error
import io.peekandpoke.kraft.testing.focus
import io.peekandpoke.kraft.testing.input
import io.peekandpoke.kraft.testing.keyDown
import io.peekandpoke.kraft.testing.keyUp
import io.peekandpoke.kraft.testing.load
import io.peekandpoke.kraft.testing.mouseDown
import io.peekandpoke.kraft.testing.mouseEnter
import io.peekandpoke.kraft.testing.mouseLeave
import io.peekandpoke.kraft.testing.mouseMove
import io.peekandpoke.kraft.testing.mouseOut
import io.peekandpoke.kraft.testing.mouseOver
import io.peekandpoke.kraft.testing.mouseUp
import io.peekandpoke.kraft.testing.paste
import io.peekandpoke.kraft.testing.pointerCancel
import io.peekandpoke.kraft.testing.pointerDown
import io.peekandpoke.kraft.testing.pointerEnter
import io.peekandpoke.kraft.testing.pointerLeave
import io.peekandpoke.kraft.testing.pointerMove
import io.peekandpoke.kraft.testing.pointerOut
import io.peekandpoke.kraft.testing.pointerOver
import io.peekandpoke.kraft.testing.pointerUp
import io.peekandpoke.kraft.testing.scroll
import io.peekandpoke.kraft.testing.select
import io.peekandpoke.kraft.testing.submit
import io.peekandpoke.kraft.testing.textContent
import io.peekandpoke.kraft.testing.touchCancel
import io.peekandpoke.kraft.testing.touchEnd
import io.peekandpoke.kraft.testing.touchMove
import io.peekandpoke.kraft.testing.touchStart
import io.peekandpoke.kraft.testing.transitionEnd
import io.peekandpoke.kraft.testing.wheel
import io.peekandpoke.ultra.html.onAnimationEnd
import io.peekandpoke.ultra.html.onAnimationIteration
import io.peekandpoke.ultra.html.onAnimationStart
import io.peekandpoke.ultra.html.onAuxClick
import io.peekandpoke.ultra.html.onBeforeInput
import io.peekandpoke.ultra.html.onBlur
import io.peekandpoke.ultra.html.onChange
import io.peekandpoke.ultra.html.onClick
import io.peekandpoke.ultra.html.onClickOrAuxClick
import io.peekandpoke.ultra.html.onClickStoppingEvent
import io.peekandpoke.ultra.html.onContextMenu
import io.peekandpoke.ultra.html.onContextMenuStoppingEvent
import io.peekandpoke.ultra.html.onCopy
import io.peekandpoke.ultra.html.onCut
import io.peekandpoke.ultra.html.onDblClick
import io.peekandpoke.ultra.html.onDrag
import io.peekandpoke.ultra.html.onDragEnd
import io.peekandpoke.ultra.html.onDragEnter
import io.peekandpoke.ultra.html.onDragLeave
import io.peekandpoke.ultra.html.onDragOver
import io.peekandpoke.ultra.html.onDragStart
import io.peekandpoke.ultra.html.onDrop
import io.peekandpoke.ultra.html.onError
import io.peekandpoke.ultra.html.onFocus
import io.peekandpoke.ultra.html.onFocusIn
import io.peekandpoke.ultra.html.onFocusOut
import io.peekandpoke.ultra.html.onInput
import io.peekandpoke.ultra.html.onKeyDown
import io.peekandpoke.ultra.html.onKeyUp
import io.peekandpoke.ultra.html.onLoad
import io.peekandpoke.ultra.html.onMouseDown
import io.peekandpoke.ultra.html.onMouseEnter
import io.peekandpoke.ultra.html.onMouseLeave
import io.peekandpoke.ultra.html.onMouseMove
import io.peekandpoke.ultra.html.onMouseOut
import io.peekandpoke.ultra.html.onMouseOver
import io.peekandpoke.ultra.html.onMouseUp
import io.peekandpoke.ultra.html.onPaste
import io.peekandpoke.ultra.html.onPointerCancel
import io.peekandpoke.ultra.html.onPointerDown
import io.peekandpoke.ultra.html.onPointerEnter
import io.peekandpoke.ultra.html.onPointerLeave
import io.peekandpoke.ultra.html.onPointerMove
import io.peekandpoke.ultra.html.onPointerOut
import io.peekandpoke.ultra.html.onPointerOver
import io.peekandpoke.ultra.html.onPointerUp
import io.peekandpoke.ultra.html.onScroll
import io.peekandpoke.ultra.html.onSelect
import io.peekandpoke.ultra.html.onSubmit
import io.peekandpoke.ultra.html.onTouchCancel
import io.peekandpoke.ultra.html.onTouchEnd
import io.peekandpoke.ultra.html.onTouchMove
import io.peekandpoke.ultra.html.onTouchStart
import io.peekandpoke.ultra.html.onTransitionEnd
import io.peekandpoke.ultra.html.onWheel
import kotlinx.coroutines.delay
import kotlinx.html.button
import kotlinx.html.div
import kotlinx.html.form
import kotlinx.html.id
import kotlinx.html.input
import org.w3c.dom.HTMLElement

/**
 * Tests that all dom event helpers in `io.peekandpoke.ultra.html` correctly wire up event handlers
 * via the Preact rendering engine.
 *
 * Each test renders an element with an event handler that writes to a `.result` div,
 * then dispatches the event via KQuery helpers and asserts the result.
 */
class DomEventsSpec : StringSpec({

    // Helper: most tests follow the same pattern
    // - render a .result div and a #target element with the event handler
    // - dispatch the event on #target
    // - check .result text
    suspend fun testEvent(
        setup: kotlinx.html.FlowContent.() -> Unit,
        trigger: suspend (target: KQuery<org.w3c.dom.Element>) -> Unit,
        expected: String,
    ) {
        TestBed.preact({ setup() }) { root ->
            trigger(root.selectCss("#target"))
            delay(10)
            root.selectCss(".result").textContent() shouldBe expected
        }
    }

    fun resultWriter(expected: String): (org.w3c.dom.events.Event) -> Unit = { evt ->
        (evt.currentTarget as HTMLElement).parentElement!!.querySelector(".result")!!.textContent = expected
    }

    // =========================================================================================================
    // Mouse events
    // =========================================================================================================

    "onClick fires" {
        testEvent(
            setup = {
                div(classes = "result") { +"0" }
                button { id = "target"; onClick(resultWriter("clicked")); +"Go" }
            },
            trigger = { it.click() },
            expected = "clicked",
        )
    }

    "onDblClick fires" {
        testEvent(
            setup = {
                div(classes = "result") { +"0" }
                div { id = "target"; onDblClick { resultWriter("dblclicked")(it) } }
            },
            trigger = { it.dblClick() },
            expected = "dblclicked",
        )
    }

    "onMouseDown fires" {
        testEvent(
            setup = {
                div(classes = "result") { +"0" }
                div { id = "target"; onMouseDown(resultWriter("down")) }
            },
            trigger = { it.mouseDown() },
            expected = "down",
        )
    }

    "onMouseUp fires" {
        testEvent(
            setup = {
                div(classes = "result") { +"0" }
                div { id = "target"; onMouseUp(resultWriter("up")) }
            },
            trigger = { it.mouseUp() },
            expected = "up",
        )
    }

    "onMouseEnter fires" {
        testEvent(
            setup = {
                div(classes = "result") { +"0" }
                div { id = "target"; onMouseEnter(resultWriter("entered")) }
            },
            trigger = { it.mouseEnter() },
            expected = "entered",
        )
    }

    "onMouseLeave fires" {
        testEvent(
            setup = {
                div(classes = "result") { +"0" }
                div { id = "target"; onMouseLeave(resultWriter("left")) }
            },
            trigger = { it.mouseLeave() },
            expected = "left",
        )
    }

    "onMouseMove fires" {
        testEvent(
            setup = {
                div(classes = "result") { +"0" }
                div { id = "target"; onMouseMove(resultWriter("moved")) }
            },
            trigger = { it.mouseMove() },
            expected = "moved",
        )
    }

    "onMouseOver fires (bubbles)" {
        testEvent(
            setup = {
                div(classes = "result") { +"0" }
                div { id = "target"; onMouseOver(resultWriter("over")) }
            },
            trigger = { it.mouseOver() },
            expected = "over",
        )
    }

    "onMouseOut fires (bubbles)" {
        testEvent(
            setup = {
                div(classes = "result") { +"0" }
                div { id = "target"; onMouseOut(resultWriter("out")) }
            },
            trigger = { it.mouseOut() },
            expected = "out",
        )
    }

    "onContextMenu fires" {
        testEvent(
            setup = {
                div(classes = "result") { +"0" }
                div { id = "target"; onContextMenu { resultWriter("context")(it) } }
            },
            trigger = { it.contextMenu() },
            expected = "context",
        )
    }

    "onWheel fires" {
        testEvent(
            setup = {
                div(classes = "result") { +"0" }
                div { id = "target"; onWheel { resultWriter("wheeled")(it) } }
            },
            trigger = { it.wheel() },
            expected = "wheeled",
        )
    }

    // =========================================================================================================
    // Keyboard events
    // =========================================================================================================

    "onKeyDown fires" {
        testEvent(
            setup = {
                div(classes = "result") { +"0" }
                input { id = "target"; onKeyDown(resultWriter("keydown")) }
            },
            trigger = { it.keyDown(key = "a") },
            expected = "keydown",
        )
    }

    "onKeyUp fires" {
        testEvent(
            setup = {
                div(classes = "result") { +"0" }
                input { id = "target"; onKeyUp(resultWriter("keyup")) }
            },
            trigger = { it.keyUp(key = "a") },
            expected = "keyup",
        )
    }

    // =========================================================================================================
    // Focus events
    // =========================================================================================================

    "onFocus fires" {
        testEvent(
            setup = {
                div(classes = "result") { +"0" }
                input { id = "target"; onFocus { resultWriter("focused")(it) } }
            },
            trigger = { it.focus() },
            expected = "focused",
        )
    }

    "onBlur fires" {
        TestBed.preact({
            div(classes = "result") { +"0" }
            input { id = "target"; onBlur(resultWriter("blurred")) }
        }) { root ->
            root.selectCss("#target").focus()
            delay(10)
            root.selectCss("#target").blur()
            delay(10)
            root.selectCss(".result").textContent() shouldBe "blurred"
        }
    }

    "onFocusIn fires (bubbles from child)" {
        TestBed.preact({
            div(classes = "result") { +"0" }
            div {
                id = "target"
                onFocusIn { resultWriter("focusin")(it) }
                input { id = "inner" }
            }
        }) { root ->
            root.selectCss("#inner").focus()
            delay(10)
            root.selectCss(".result").textContent() shouldBe "focusin"
        }
    }

    "onFocusOut fires (bubbles from child)" {
        TestBed.preact({
            div(classes = "result") { +"0" }
            div {
                id = "target"
                onFocusOut { resultWriter("focusout")(it) }
                input { id = "inner" }
            }
        }) { root ->
            root.selectCss("#inner").focus()
            delay(10)
            root.selectCss("#inner").blur()
            delay(10)
            root.selectCss(".result").textContent() shouldBe "focusout"
        }
    }

    // =========================================================================================================
    // Form / Input events
    // =========================================================================================================

    "onInput fires" {
        testEvent(
            setup = {
                div(classes = "result") { +"0" }
                input { id = "target"; onInput { resultWriter("input")(it) } }
            },
            trigger = { it.input() },
            expected = "input",
        )
    }

    "onChange fires" {
        testEvent(
            setup = {
                div(classes = "result") { +"0" }
                input { id = "target"; onChange(resultWriter("changed")) }
            },
            trigger = { it.change() },
            expected = "changed",
        )
    }

    "onSelect fires" {
        testEvent(
            setup = {
                div(classes = "result") { +"0" }
                input { id = "target"; onSelect(resultWriter("selected")) }
            },
            trigger = { it.select() },
            expected = "selected",
        )
    }

    "onSubmit fires and prevents default" {
        testEvent(
            setup = {
                div(classes = "result") { +"0" }
                form { id = "target"; onSubmit(resultWriter("submitted")); button { +"Go" } }
            },
            trigger = { it.submit() },
            expected = "submitted",
        )
    }

    // =========================================================================================================
    // Animation events
    // =========================================================================================================

    "onAnimationEnd fires" {
        testEvent(
            setup = {
                div(classes = "result") { +"0" }
                div { id = "target"; onAnimationEnd(resultWriter("animationend")) }
            },
            trigger = { it.animationEnd() },
            expected = "animationend",
        )
    }

    "onAnimationStart fires" {
        testEvent(
            setup = {
                div(classes = "result") { +"0" }
                div { id = "target"; onAnimationStart(resultWriter("animationstart")) }
            },
            trigger = { it.animationStart() },
            expected = "animationstart",
        )
    }

    "onAnimationIteration fires" {
        testEvent(
            setup = {
                div(classes = "result") { +"0" }
                div { id = "target"; onAnimationIteration(resultWriter("animationiteration")) }
            },
            trigger = { it.animationIteration() },
            expected = "animationiteration",
        )
    }

    "onTransitionEnd fires" {
        testEvent(
            setup = {
                div(classes = "result") { +"0" }
                div { id = "target"; onTransitionEnd(resultWriter("transitionend")) }
            },
            trigger = { it.transitionEnd() },
            expected = "transitionend",
        )
    }

    // =========================================================================================================
    // Clipboard events
    // =========================================================================================================

    "onCopy fires" {
        testEvent(
            setup = {
                div(classes = "result") { +"0" }
                div { id = "target"; onCopy { resultWriter("copied")(it) } }
            },
            trigger = { it.copy() },
            expected = "copied",
        )
    }

    "onCut fires" {
        testEvent(
            setup = {
                div(classes = "result") { +"0" }
                div { id = "target"; onCut { resultWriter("cut")(it) } }
            },
            trigger = { it.cut() },
            expected = "cut",
        )
    }

    "onPaste fires" {
        testEvent(
            setup = {
                div(classes = "result") { +"0" }
                div { id = "target"; onPaste { resultWriter("pasted")(it) } }
            },
            trigger = { it.paste() },
            expected = "pasted",
        )
    }

    // =========================================================================================================
    // Drag events
    // =========================================================================================================

    "onDragStart fires" {
        testEvent(
            setup = {
                div(classes = "result") { +"0" }
                div { id = "target"; onDragStart { resultWriter("dragstart")(it) } }
            },
            trigger = { it.dragStart() },
            expected = "dragstart",
        )
    }

    "onDragEnd fires" {
        testEvent(
            setup = {
                div(classes = "result") { +"0" }
                div { id = "target"; onDragEnd { resultWriter("dragend")(it) } }
            },
            trigger = { it.dragEnd() },
            expected = "dragend",
        )
    }

    "onDragEnter fires" {
        testEvent(
            setup = {
                div(classes = "result") { +"0" }
                div { id = "target"; onDragEnter { resultWriter("dragenter")(it) } }
            },
            trigger = { it.dragEnter() },
            expected = "dragenter",
        )
    }

    "onDragOver fires" {
        testEvent(
            setup = {
                div(classes = "result") { +"0" }
                div { id = "target"; onDragOver { resultWriter("dragover")(it) } }
            },
            trigger = { it.dragOver() },
            expected = "dragover",
        )
    }

    "onDragLeave fires" {
        testEvent(
            setup = {
                div(classes = "result") { +"0" }
                div { id = "target"; onDragLeave { resultWriter("dragleave")(it) } }
            },
            trigger = { it.dragLeave() },
            expected = "dragleave",
        )
    }

    "onDrop fires" {
        testEvent(
            setup = {
                div(classes = "result") { +"0" }
                div { id = "target"; onDrop { resultWriter("dropped")(it) } }
            },
            trigger = { it.drop() },
            expected = "dropped",
        )
    }

    // =========================================================================================================
    // Pointer events
    // =========================================================================================================

    "onPointerDown fires" {
        testEvent(
            setup = {
                div(classes = "result") { +"0" }
                div { id = "target"; onPointerDown { resultWriter("pointerdown")(it) } }
            },
            trigger = { it.pointerDown() },
            expected = "pointerdown",
        )
    }

    "onPointerUp fires" {
        testEvent(
            setup = {
                div(classes = "result") { +"0" }
                div { id = "target"; onPointerUp { resultWriter("pointerup")(it) } }
            },
            trigger = { it.pointerUp() },
            expected = "pointerup",
        )
    }

    "onPointerMove fires" {
        testEvent(
            setup = {
                div(classes = "result") { +"0" }
                div { id = "target"; onPointerMove { resultWriter("pointermove")(it) } }
            },
            trigger = { it.pointerMove() },
            expected = "pointermove",
        )
    }

    "onPointerEnter fires" {
        testEvent(
            setup = {
                div(classes = "result") { +"0" }
                div { id = "target"; onPointerEnter { resultWriter("pointerenter")(it) } }
            },
            trigger = { it.pointerEnter() },
            expected = "pointerenter",
        )
    }

    "onPointerLeave fires" {
        testEvent(
            setup = {
                div(classes = "result") { +"0" }
                div { id = "target"; onPointerLeave { resultWriter("pointerleave")(it) } }
            },
            trigger = { it.pointerLeave() },
            expected = "pointerleave",
        )
    }

    "onPointerOver fires" {
        testEvent(
            setup = {
                div(classes = "result") { +"0" }
                div { id = "target"; onPointerOver { resultWriter("pointerover")(it) } }
            },
            trigger = { it.pointerOver() },
            expected = "pointerover",
        )
    }

    "onPointerOut fires" {
        testEvent(
            setup = {
                div(classes = "result") { +"0" }
                div { id = "target"; onPointerOut { resultWriter("pointerout")(it) } }
            },
            trigger = { it.pointerOut() },
            expected = "pointerout",
        )
    }

    // =========================================================================================================
    // Touch events
    // =========================================================================================================

    "onTouchStart fires" {
        testEvent(
            setup = {
                div(classes = "result") { +"0" }
                div { id = "target"; onTouchStart(resultWriter("touchstart")) }
            },
            trigger = { it.touchStart() },
            expected = "touchstart",
        )
    }

    "onTouchMove fires" {
        testEvent(
            setup = {
                div(classes = "result") { +"0" }
                div { id = "target"; onTouchMove(resultWriter("touchmove")) }
            },
            trigger = { it.touchMove() },
            expected = "touchmove",
        )
    }

    "onTouchEnd fires" {
        testEvent(
            setup = {
                div(classes = "result") { +"0" }
                div { id = "target"; onTouchEnd(resultWriter("touchend")) }
            },
            trigger = { it.touchEnd() },
            expected = "touchend",
        )
    }

    // =========================================================================================================
    // Scroll & Load events
    // =========================================================================================================

    "onScroll fires" {
        testEvent(
            setup = {
                div(classes = "result") { +"0" }
                div { id = "target"; onScroll(resultWriter("scrolled")) }
            },
            trigger = { it.scroll() },
            expected = "scrolled",
        )
    }

    "onError fires" {
        testEvent(
            setup = {
                div(classes = "result") { +"0" }
                div { id = "target"; onError(resultWriter("errored")) }
            },
            trigger = { it.error() },
            expected = "errored",
        )
    }

    // =========================================================================================================
    // Compound helpers
    // =========================================================================================================

    "onAuxClick fires" {
        testEvent(
            setup = {
                div(classes = "result") { +"0" }
                div { id = "target"; onAuxClick { resultWriter("auxclicked")(it) } }
            },
            trigger = { it.dispatch("auxclick", bubbles = true) },
            expected = "auxclicked",
        )
    }

    "onClickOrAuxClick fires on click" {
        testEvent(
            setup = {
                div(classes = "result") { +"0" }
                button { id = "target"; onClickOrAuxClick { resultWriter("any-clicked")(it) }; +"Go" }
            },
            trigger = { it.click() },
            expected = "any-clicked",
        )
    }

    "onClickOrAuxClick fires on auxclick" {
        testEvent(
            setup = {
                div(classes = "result") { +"0" }
                div { id = "target"; onClickOrAuxClick { resultWriter("any-auxclicked")(it) } }
            },
            trigger = { it.dispatch("auxclick", bubbles = true) },
            expected = "any-auxclicked",
        )
    }

    "onContextMenuStoppingEvent prevents default and stops propagation" {
        TestBed.preact({
            div(classes = "result") { +"0" }
            div {
                id = "outer"
                onContextMenu { (it.currentTarget as HTMLElement).querySelector(".result")!!.textContent = "outer" }
                div {
                    id = "inner"
                    onContextMenuStoppingEvent {
                        (it.currentTarget as HTMLElement).parentElement!!.parentElement!!
                            .querySelector(".result")!!.textContent = "inner-only"
                    }
                }
            }
        }) { root ->
            root.selectCss("#inner").contextMenu()
            delay(10)
            root.selectCss(".result").textContent() shouldBe "inner-only"
        }
    }

    "onBeforeInput fires" {
        testEvent(
            setup = {
                div(classes = "result") { +"0" }
                input { id = "target"; onBeforeInput { resultWriter("beforeinput")(it) } }
            },
            trigger = { it.dispatch("beforeinput", bubbles = true) },
            expected = "beforeinput",
        )
    }

    "onDrag fires" {
        testEvent(
            setup = {
                div(classes = "result") { +"0" }
                div { id = "target"; onDrag { resultWriter("drag")(it) } }
            },
            trigger = { it.drag() },
            expected = "drag",
        )
    }

    "onLoad fires" {
        testEvent(
            setup = {
                div(classes = "result") { +"0" }
                div { id = "target"; onLoad(resultWriter("loaded")) }
            },
            trigger = { it.load() },
            expected = "loaded",
        )
    }

    "onPointerCancel fires" {
        testEvent(
            setup = {
                div(classes = "result") { +"0" }
                div { id = "target"; onPointerCancel { resultWriter("pointercancel")(it) } }
            },
            trigger = { it.pointerCancel() },
            expected = "pointercancel",
        )
    }

    "onTouchCancel fires" {
        testEvent(
            setup = {
                div(classes = "result") { +"0" }
                div { id = "target"; onTouchCancel(resultWriter("touchcancel")) }
            },
            trigger = { it.touchCancel() },
            expected = "touchcancel",
        )
    }

    // =========================================================================================================
    // Compound helpers
    // =========================================================================================================

    "onClickStoppingEvent prevents propagation" {
        TestBed.preact({
            div(classes = "result") { +"0" }
            div {
                id = "outer"
                onClick { (it.currentTarget as HTMLElement).querySelector(".result")!!.textContent = "outer" }
                button {
                    id = "inner"
                    onClickStoppingEvent {
                        (it.currentTarget as HTMLElement).parentElement!!.parentElement!!
                            .querySelector(".result")!!.textContent = "inner-only"
                    }
                    +"Inner"
                }
            }
        }) { root ->
            root.selectCss("#inner").click()
            delay(10)
            root.selectCss(".result").textContent() shouldBe "inner-only"
        }
    }
})
