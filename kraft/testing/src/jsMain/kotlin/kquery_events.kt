package io.peekandpoke.kraft.testing

import org.w3c.dom.Element
import org.w3c.dom.HTMLElement
import org.w3c.dom.events.Event
import org.w3c.dom.events.KeyboardEvent
import org.w3c.dom.events.MouseEvent

// =====================================================================================================================
// Low-level dispatch helpers
// =====================================================================================================================

private fun eventInit(bubbles: Boolean = false, cancelable: Boolean = false): dynamic {
    val o = js("{}")
    o.bubbles = bubbles
    o.cancelable = cancelable
    return o
}

/** Dispatches a generic [Event] on all matched elements. */
fun <E : Element> KQuery<E>.dispatch(type: String, bubbles: Boolean = false, cancelable: Boolean = false): KQuery<E> =
    apply { forEach { it.dispatchEvent(Event(type, eventInit(bubbles, cancelable))) } }

/** Dispatches a [MouseEvent] on all matched elements. */
fun <E : Element> KQuery<E>.dispatchMouse(type: String, bubbles: Boolean = false): KQuery<E> =
    apply { forEach { it.dispatchEvent(MouseEvent(type, eventInit(bubbles))) } }

/** Dispatches a [KeyboardEvent] on all matched elements. */
fun <E : Element> KQuery<E>.dispatchKeyboard(
    type: String,
    key: String = "",
    code: String = "",
    bubbles: Boolean = true,
): KQuery<E> = apply {
    val init: dynamic = eventInit(bubbles)
    init.key = key
    init.code = code
    forEach { it.dispatchEvent(KeyboardEvent(type, init)) }
}

// =====================================================================================================================
// Click
// =====================================================================================================================

/** Simulates a click on all HTML elements in this query. Returns this query for chaining. */
fun <E : Element> KQuery<E>.click(): KQuery<E> = apply {
    filterIsInstance<HTMLElement>()
        .forEach { it.click() }
}

// =====================================================================================================================
// Mouse events
// =====================================================================================================================

/** Dispatches a `dblclick` event on all matched elements. */
fun <E : Element> KQuery<E>.dblClick(): KQuery<E> = dispatchMouse("dblclick", bubbles = true)

/** Dispatches a `mousedown` event on all matched elements. */
fun <E : Element> KQuery<E>.mouseDown(): KQuery<E> = dispatchMouse("mousedown", bubbles = true)

/** Dispatches a `mouseup` event on all matched elements. */
fun <E : Element> KQuery<E>.mouseUp(): KQuery<E> = dispatchMouse("mouseup", bubbles = true)

/** Dispatches a `mouseenter` event on all matched elements. Does not bubble. */
fun <E : Element> KQuery<E>.mouseEnter(): KQuery<E> = dispatchMouse("mouseenter")

/** Dispatches a `mouseleave` event on all matched elements. Does not bubble. */
fun <E : Element> KQuery<E>.mouseLeave(): KQuery<E> = dispatchMouse("mouseleave")

/** Dispatches a `mousemove` event on all matched elements. */
fun <E : Element> KQuery<E>.mouseMove(): KQuery<E> = dispatchMouse("mousemove", bubbles = true)

/** Dispatches a `mouseover` event on all matched elements. Bubbles. */
fun <E : Element> KQuery<E>.mouseOver(): KQuery<E> = dispatchMouse("mouseover", bubbles = true)

/** Dispatches a `mouseout` event on all matched elements. Bubbles. */
fun <E : Element> KQuery<E>.mouseOut(): KQuery<E> = dispatchMouse("mouseout", bubbles = true)

/** Dispatches a `contextmenu` event on all matched elements. */
fun <E : Element> KQuery<E>.contextMenu(): KQuery<E> = dispatchMouse("contextmenu", bubbles = true)

/** Dispatches a `wheel` event on all matched elements. */
fun <E : Element> KQuery<E>.wheel(): KQuery<E> = dispatch("wheel", bubbles = true)

// =====================================================================================================================
// Keyboard events
// =====================================================================================================================

/** Dispatches a `keydown` event on all matched elements. */
fun <E : Element> KQuery<E>.keyDown(key: String = "", code: String = ""): KQuery<E> =
    dispatchKeyboard("keydown", key = key, code = code)

/** Dispatches a `keyup` event on all matched elements. */
fun <E : Element> KQuery<E>.keyUp(key: String = "", code: String = ""): KQuery<E> =
    dispatchKeyboard("keyup", key = key, code = code)

// =====================================================================================================================
// Focus events
// =====================================================================================================================

/** Calls [HTMLElement.focus] on all matched elements. */
fun <E : Element> KQuery<E>.focus(): KQuery<E> = apply {
    filterIsInstance<HTMLElement>().forEach { it.focus() }
}

/** Calls [HTMLElement.blur] on all matched elements. */
fun <E : Element> KQuery<E>.blur(): KQuery<E> = apply {
    filterIsInstance<HTMLElement>().forEach { it.blur() }
}

// =====================================================================================================================
// Form / Input events
// =====================================================================================================================

/** Dispatches an `input` event on all matched elements. */
fun <E : Element> KQuery<E>.input(): KQuery<E> = dispatch("input", bubbles = true)

/** Dispatches a `change` event on all matched elements. */
fun <E : Element> KQuery<E>.change(): KQuery<E> = dispatch("change", bubbles = true)

/** Dispatches a `select` event on all matched elements. */
fun <E : Element> KQuery<E>.select(): KQuery<E> = dispatch("select", bubbles = true)

/** Dispatches a `submit` event on all matched elements. */
fun <E : Element> KQuery<E>.submit(): KQuery<E> = dispatch("submit", bubbles = true, cancelable = true)

// =====================================================================================================================
// Drag events
// =====================================================================================================================

/** Dispatches a `dragstart` event on all matched elements. */
fun <E : Element> KQuery<E>.dragStart(): KQuery<E> = dispatch("dragstart", bubbles = true)

/** Dispatches a `drag` event on all matched elements. */
fun <E : Element> KQuery<E>.drag(): KQuery<E> = dispatch("drag", bubbles = true)

/** Dispatches a `dragend` event on all matched elements. */
fun <E : Element> KQuery<E>.dragEnd(): KQuery<E> = dispatch("dragend", bubbles = true)

/** Dispatches a `dragenter` event on all matched elements. */
fun <E : Element> KQuery<E>.dragEnter(): KQuery<E> = dispatch("dragenter", bubbles = true)

/** Dispatches a `dragover` event on all matched elements. */
fun <E : Element> KQuery<E>.dragOver(): KQuery<E> = dispatch("dragover", bubbles = true)

/** Dispatches a `dragleave` event on all matched elements. */
fun <E : Element> KQuery<E>.dragLeave(): KQuery<E> = dispatch("dragleave", bubbles = true)

/** Dispatches a `drop` event on all matched elements. */
fun <E : Element> KQuery<E>.drop(): KQuery<E> = dispatch("drop", bubbles = true)

// =====================================================================================================================
// Pointer events
// =====================================================================================================================

/** Dispatches a `pointerdown` event on all matched elements. */
fun <E : Element> KQuery<E>.pointerDown(): KQuery<E> = dispatch("pointerdown", bubbles = true)

/** Dispatches a `pointerup` event on all matched elements. */
fun <E : Element> KQuery<E>.pointerUp(): KQuery<E> = dispatch("pointerup", bubbles = true)

/** Dispatches a `pointermove` event on all matched elements. */
fun <E : Element> KQuery<E>.pointerMove(): KQuery<E> = dispatch("pointermove", bubbles = true)

/** Dispatches a `pointerenter` event on all matched elements. Does not bubble. */
fun <E : Element> KQuery<E>.pointerEnter(): KQuery<E> = dispatch("pointerenter")

/** Dispatches a `pointerleave` event on all matched elements. Does not bubble. */
fun <E : Element> KQuery<E>.pointerLeave(): KQuery<E> = dispatch("pointerleave")

/** Dispatches a `pointerover` event on all matched elements. Bubbles. */
fun <E : Element> KQuery<E>.pointerOver(): KQuery<E> = dispatch("pointerover", bubbles = true)

/** Dispatches a `pointerout` event on all matched elements. Bubbles. */
fun <E : Element> KQuery<E>.pointerOut(): KQuery<E> = dispatch("pointerout", bubbles = true)

/** Dispatches a `pointercancel` event on all matched elements. */
fun <E : Element> KQuery<E>.pointerCancel(): KQuery<E> = dispatch("pointercancel", bubbles = true)

// =====================================================================================================================
// Touch events
// =====================================================================================================================

/** Dispatches a `touchstart` event on all matched elements. */
fun <E : Element> KQuery<E>.touchStart(): KQuery<E> = dispatch("touchstart", bubbles = true)

/** Dispatches a `touchmove` event on all matched elements. */
fun <E : Element> KQuery<E>.touchMove(): KQuery<E> = dispatch("touchmove", bubbles = true)

/** Dispatches a `touchend` event on all matched elements. */
fun <E : Element> KQuery<E>.touchEnd(): KQuery<E> = dispatch("touchend", bubbles = true)

/** Dispatches a `touchcancel` event on all matched elements. */
fun <E : Element> KQuery<E>.touchCancel(): KQuery<E> = dispatch("touchcancel", bubbles = true)

// =====================================================================================================================
// Animation & Transition events
// =====================================================================================================================

/** Dispatches an `animationstart` event on all matched elements. */
fun <E : Element> KQuery<E>.animationStart(): KQuery<E> = dispatch("animationstart")

/** Dispatches an `animationend` event on all matched elements. */
fun <E : Element> KQuery<E>.animationEnd(): KQuery<E> = dispatch("animationend")

/** Dispatches an `animationiteration` event on all matched elements. */
fun <E : Element> KQuery<E>.animationIteration(): KQuery<E> = dispatch("animationiteration")

/** Dispatches a `transitionend` event on all matched elements. */
fun <E : Element> KQuery<E>.transitionEnd(): KQuery<E> = dispatch("transitionend")

// =====================================================================================================================
// Clipboard events
// =====================================================================================================================

/** Dispatches a `copy` event on all matched elements. */
fun <E : Element> KQuery<E>.copy(): KQuery<E> = dispatch("copy", bubbles = true)

/** Dispatches a `cut` event on all matched elements. */
fun <E : Element> KQuery<E>.cut(): KQuery<E> = dispatch("cut", bubbles = true)

/** Dispatches a `paste` event on all matched elements. */
fun <E : Element> KQuery<E>.paste(): KQuery<E> = dispatch("paste", bubbles = true)

// =====================================================================================================================
// Scroll & Load events
// =====================================================================================================================

/** Dispatches a `scroll` event on all matched elements. */
fun <E : Element> KQuery<E>.scroll(): KQuery<E> = dispatch("scroll")

/** Dispatches a `load` event on all matched elements. */
fun <E : Element> KQuery<E>.load(): KQuery<E> = dispatch("load")

/** Dispatches an `error` event on all matched elements. */
fun <E : Element> KQuery<E>.error(): KQuery<E> = dispatch("error")
