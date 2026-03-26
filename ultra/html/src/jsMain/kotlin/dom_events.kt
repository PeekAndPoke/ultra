package io.peekandpoke.ultra.html

import kotlinx.html.CommonAttributeGroupFacade
import kotlinx.html.js.onBlurFunction
import kotlinx.html.js.onChangeFunction
import kotlinx.html.js.onClickFunction
import kotlinx.html.js.onContextMenuFunction
import kotlinx.html.js.onDragLeaveFunction
import kotlinx.html.js.onDragOverFunction
import kotlinx.html.js.onDropFunction
import kotlinx.html.js.onErrorFunction
import kotlinx.html.js.onFocusFunction
import kotlinx.html.js.onFocusInFunction
import kotlinx.html.js.onFocusOutFunction
import kotlinx.html.js.onInputFunction
import kotlinx.html.js.onKeyDownFunction
import kotlinx.html.js.onKeyUpFunction
import kotlinx.html.js.onMouseDownFunction
import kotlinx.html.js.onMouseEnterFunction
import kotlinx.html.js.onMouseLeaveFunction
import kotlinx.html.js.onMouseMoveFunction
import kotlinx.html.js.onMouseOutFunction
import kotlinx.html.js.onMouseOverFunction
import kotlinx.html.js.onMouseUpFunction
import kotlinx.html.js.onSelectFunction
import kotlinx.html.js.onSubmitFunction
import kotlinx.html.js.onWheelFunction
import org.w3c.dom.DragEvent
import org.w3c.dom.clipboard.ClipboardEvent
import org.w3c.dom.events.Event
import org.w3c.dom.events.FocusEvent
import org.w3c.dom.events.InputEvent
import org.w3c.dom.events.KeyboardEvent
import org.w3c.dom.events.MouseEvent
import org.w3c.dom.events.UIEvent
import org.w3c.dom.events.WheelEvent
import org.w3c.dom.pointerevents.PointerEvent

// =====================================================================================================================
// Animation events
// https://developer.mozilla.org/en-US/docs/Web/API/Element#animation_events
// =====================================================================================================================

/**
 * Adds an onAnimationEnd handler.
 *
 * Fired when a CSS animation completes.
 *
 * https://developer.mozilla.org/en-US/docs/Web/API/Element/animationend_event
 */
fun CommonAttributeGroupFacade.onAnimationEnd(handler: (Event) -> Unit) {
    consumer.onTagEvent(this, "onanimationend", handler.asDynamic())
}

/**
 * Adds an onAnimationStart handler.
 *
 * Fired when a CSS animation starts.
 *
 * https://developer.mozilla.org/en-US/docs/Web/API/Element/animationstart_event
 */
fun CommonAttributeGroupFacade.onAnimationStart(handler: (Event) -> Unit) {
    consumer.onTagEvent(this, "onanimationstart", handler.asDynamic())
}

/**
 * Adds an onAnimationIteration handler.
 *
 * Fired when a CSS animation completes one iteration.
 *
 * https://developer.mozilla.org/en-US/docs/Web/API/Element/animationiteration_event
 */
fun CommonAttributeGroupFacade.onAnimationIteration(handler: (Event) -> Unit) {
    consumer.onTagEvent(this, "onanimationiteration", handler.asDynamic())
}

/**
 * Adds an onTransitionEnd handler.
 *
 * Fired when a CSS transition completes.
 *
 * https://developer.mozilla.org/en-US/docs/Web/API/Element/transitionend_event
 */
fun CommonAttributeGroupFacade.onTransitionEnd(handler: (Event) -> Unit) {
    consumer.onTagEvent(this, "ontransitionend", handler.asDynamic())
}

// =====================================================================================================================
// Clipboard events
// https://developer.mozilla.org/en-US/docs/Web/API/Element#clipboard_events
// =====================================================================================================================

/**
 * Adds an onCopy handler.
 *
 * Fired when the user initiates a copy action.
 *
 * https://developer.mozilla.org/en-US/docs/Web/API/Element/copy_event
 */
fun CommonAttributeGroupFacade.onCopy(handler: (ClipboardEvent) -> Unit) {
    consumer.onTagEvent(this, "oncopy", handler.asDynamic())
}

/**
 * Adds an onCut handler.
 *
 * Fired when the user initiates a cut action.
 *
 * https://developer.mozilla.org/en-US/docs/Web/API/Element/cut_event
 */
fun CommonAttributeGroupFacade.onCut(handler: (ClipboardEvent) -> Unit) {
    consumer.onTagEvent(this, "oncut", handler.asDynamic())
}

/**
 * Adds an onPaste handler.
 *
 * Fired when the user initiates a paste action.
 *
 * https://developer.mozilla.org/en-US/docs/Web/API/Element/paste_event
 */
fun CommonAttributeGroupFacade.onPaste(handler: (ClipboardEvent) -> Unit) {
    consumer.onTagEvent(this, "onpaste", handler.asDynamic())
}

// =====================================================================================================================
// Drag events
// https://developer.mozilla.org/en-US/docs/Web/API/HTML_Drag_and_Drop_API
// =====================================================================================================================

/**
 * Adds an onDrag handler.
 *
 * Fired continuously while an element is being dragged.
 *
 * https://developer.mozilla.org/en-US/docs/Web/API/HTMLElement/drag_event
 */
fun CommonAttributeGroupFacade.onDrag(handler: (DragEvent) -> Unit) {
    consumer.onTagEvent(this, "ondrag", handler.asDynamic())
}

/**
 * Adds an onDragStart handler.
 *
 * Fired when the user starts dragging an element.
 *
 * https://developer.mozilla.org/en-US/docs/Web/API/HTMLElement/dragstart_event
 */
fun CommonAttributeGroupFacade.onDragStart(handler: (DragEvent) -> Unit) {
    consumer.onTagEvent(this, "ondragstart", handler.asDynamic())
}

/**
 * Adds an onDragEnd handler.
 *
 * Fired when a drag operation ends (release or Escape).
 *
 * https://developer.mozilla.org/en-US/docs/Web/API/HTMLElement/dragend_event
 */
fun CommonAttributeGroupFacade.onDragEnd(handler: (DragEvent) -> Unit) {
    consumer.onTagEvent(this, "ondragend", handler.asDynamic())
}

/**
 * Adds an onDragEnter handler.
 *
 * Fired when a dragged element enters a valid drop target.
 *
 * https://developer.mozilla.org/en-US/docs/Web/API/HTMLElement/dragenter_event
 */
fun CommonAttributeGroupFacade.onDragEnter(handler: (DragEvent) -> Unit) {
    consumer.onTagEvent(this, "ondragenter", handler.asDynamic())
}

/**
 * Adds an onDragOver handler.
 *
 * Fired continuously while a dragged element is over a valid drop target.
 *
 * https://developer.mozilla.org/en-US/docs/Web/API/HTMLElement/dragover_event
 */
fun CommonAttributeGroupFacade.onDragOver(handler: (DragEvent) -> Unit) {
    onDragOverFunction = handler.asDynamic()
}

/**
 * Adds an onDragLeave handler.
 *
 * Fired when a dragged element leaves a valid drop target.
 *
 * https://developer.mozilla.org/en-US/docs/Web/API/HTMLElement/dragleave_event
 */
fun CommonAttributeGroupFacade.onDragLeave(handler: (DragEvent) -> Unit) {
    onDragLeaveFunction = handler.asDynamic()
}

/**
 * Adds an onDrop handler.
 *
 * Fired when an element is dropped on a valid drop target.
 *
 * https://developer.mozilla.org/en-US/docs/Web/API/HTMLElement/drop_event
 */
fun CommonAttributeGroupFacade.onDrop(handler: (DragEvent) -> Unit) {
    onDropFunction = handler.asDynamic()
}

// =====================================================================================================================
// Focus events
// https://developer.mozilla.org/en-US/docs/Web/API/Element#focus_events
// =====================================================================================================================

/**
 * Adds an onBlur handler.
 *
 * This event does NOT bubble while [onFocusOut] does.
 *
 * https://developer.mozilla.org/en-US/docs/Web/API/Element/blur_event
 */
fun CommonAttributeGroupFacade.onBlur(handler: (Event) -> Unit) {
    onBlurFunction = handler.asDynamic()
}

/**
 * Adds an onFocus handler.
 *
 * This event does NOT bubble while [onFocusIn] does.
 *
 * https://developer.mozilla.org/en-US/docs/Web/API/Element/focus_event
 */
fun CommonAttributeGroupFacade.onFocus(handler: (FocusEvent) -> Unit) {
    onFocusFunction = handler.asDynamic()
}

/**
 * Adds an onFocusIn handler.
 *
 * This event DOES bubble while [onFocus] does not.
 *
 * https://developer.mozilla.org/en-US/docs/Web/API/Element/focusin_event
 */
fun CommonAttributeGroupFacade.onFocusIn(handler: (FocusEvent) -> Unit) {
    onFocusInFunction = handler.asDynamic()
}

/**
 * Adds an onFocusOut handler.
 *
 * This event DOES bubble while [onBlur] does not.
 *
 * https://developer.mozilla.org/en-US/docs/Web/API/Element/focusout_event
 */
fun CommonAttributeGroupFacade.onFocusOut(handler: (FocusEvent) -> Unit) {
    onFocusOutFunction = handler.asDynamic()
}

// =====================================================================================================================
// Form events
// =====================================================================================================================

/**
 * Adds an onChange handler.
 *
 * Fired when the value of an input, select, or textarea has been changed and committed by the user.
 *
 * https://developer.mozilla.org/en-US/docs/Web/API/HTMLElement/change_event
 */
fun CommonAttributeGroupFacade.onChange(handler: (Event) -> Unit) {
    onChangeFunction = handler.asDynamic()
}

/**
 * Adds an onInput handler.
 *
 * Fired immediately when the value of an input, select, or textarea is changed.
 *
 * https://developer.mozilla.org/en-US/docs/Web/API/Element/input_event
 */
fun CommonAttributeGroupFacade.onInput(handler: (InputEvent) -> Unit) {
    onInputFunction = handler.asDynamic()
}

/**
 * Adds an onBeforeInput handler.
 *
 * Fired before the value of an input, select, or textarea is modified.
 *
 * https://developer.mozilla.org/en-US/docs/Web/API/Element/beforeinput_event
 */
fun CommonAttributeGroupFacade.onBeforeInput(handler: (InputEvent) -> Unit) {
    consumer.onTagEvent(this, "onbeforeinput", handler.asDynamic())
}

/**
 * Adds an onSelect handler.
 *
 * Fired when text inside an input or textarea is selected.
 *
 * https://developer.mozilla.org/en-US/docs/Web/API/HTMLInputElement/select_event
 */
fun CommonAttributeGroupFacade.onSelect(handler: (Event) -> Unit) {
    onSelectFunction = handler.asDynamic()
}

/**
 * Adds an onSubmit handler.
 *
 * Automatically calls [Event.preventDefault] to prevent full-page form submission.
 *
 * https://developer.mozilla.org/en-US/docs/Web/API/HTMLFormElement/submit_event
 */
fun CommonAttributeGroupFacade.onSubmit(handler: (Event) -> Unit) {
    onSubmitFunction = {
        it.preventDefault()
        handler(it.asDynamic())
    }
}

// =====================================================================================================================
// Keyboard events
// https://developer.mozilla.org/en-US/docs/Web/API/Element#keyboard_events
// =====================================================================================================================

/**
 * Adds an onKeyDown handler.
 *
 * Fired when a key is pressed.
 *
 * https://developer.mozilla.org/en-US/docs/Web/API/Element/keydown_event
 */
fun CommonAttributeGroupFacade.onKeyDown(handler: (KeyboardEvent) -> Unit) {
    onKeyDownFunction = handler.asDynamic()
}


/**
 * Adds an onKeyUp handler.
 *
 * Fired when a key is released.
 *
 * https://developer.mozilla.org/en-US/docs/Web/API/Element/keyup_event
 */
fun CommonAttributeGroupFacade.onKeyUp(handler: (KeyboardEvent) -> Unit) {
    onKeyUpFunction = handler.asDynamic()
}

// =====================================================================================================================
// Mouse events
// https://developer.mozilla.org/en-US/docs/Web/API/Element#mouse_events
// =====================================================================================================================

/**
 * Adds an onClick handler.
 *
 * https://developer.mozilla.org/en-US/docs/Web/API/Element/click_event
 */
fun CommonAttributeGroupFacade.onClick(handler: (PointerEvent) -> Unit) {
    onClickFunction = handler.asDynamic()
}

/**
 * Adds an onClick handler that calls [Event.preventDefault] and [Event.stopPropagation] before invoking [handler].
 *
 * https://developer.mozilla.org/en-US/docs/Web/API/Element/click_event
 */
fun CommonAttributeGroupFacade.onClickStoppingEvent(handler: (PointerEvent) -> Unit) = onClick { e ->
    e.preventDefault()
    e.stopPropagation()
    handler(e.asDynamic())
}

/**
 * Adds both an onClick and an onAuxClick handler to the same [handler].
 *
 * Covers left-click, middle-click, and other auxiliary button clicks.
 *
 * https://developer.mozilla.org/en-US/docs/Web/API/Element/click_event
 * https://developer.mozilla.org/en-US/docs/Web/API/Element/auxclick_event
 */
fun CommonAttributeGroupFacade.onClickOrAuxClick(handler: (PointerEvent) -> Unit) {
    onClick(handler)
    onAuxClick(handler)
}

/**
 * Adds an onAuxClick handler.
 *
 * Fired when a non-primary button (middle-click, right-click, etc.) is clicked.
 *
 * https://developer.mozilla.org/en-US/docs/Web/API/Element/auxclick_event
 */
fun CommonAttributeGroupFacade.onAuxClick(handler: (PointerEvent) -> Unit) {
    consumer.onTagEvent(this, "onauxclick", handler.asDynamic())
}

/**
 * Adds an onContextMenu handler.
 *
 * Fired when the user attempts to open a context menu (typically right-click).
 *
 * https://developer.mozilla.org/en-US/docs/Web/API/Element/contextmenu_event
 */
fun CommonAttributeGroupFacade.onContextMenu(handler: (PointerEvent) -> Unit) {
    onContextMenuFunction = handler.asDynamic()
}

/**
 * Adds an onContextMenu handler that calls [Event.preventDefault] and [Event.stopPropagation].
 *
 * Prevents the browser's default context menu from appearing.
 *
 * https://developer.mozilla.org/en-US/docs/Web/API/Element/contextmenu_event
 */
fun CommonAttributeGroupFacade.onContextMenuStoppingEvent(handler: (UIEvent) -> Unit) = onContextMenu { e ->
    e.preventDefault()
    e.stopPropagation()
    handler(e.asDynamic())
}

/**
 * Adds an onDblClick handler.
 *
 * Fired when a pointing device button is double-clicked.
 *
 * https://developer.mozilla.org/en-US/docs/Web/API/Element/dblclick_event
 */
fun CommonAttributeGroupFacade.onDblClick(handler: (MouseEvent) -> Unit) {
    consumer.onTagEvent(this, "ondblclick", handler.asDynamic())
}

/**
 * Adds an onMouseDown handler.
 *
 * Fired when a pointing device button is pressed on an element.
 *
 * https://developer.mozilla.org/en-US/docs/Web/API/Element/mousedown_event
 */
fun CommonAttributeGroupFacade.onMouseDown(handler: (MouseEvent) -> Unit) {
    onMouseDownFunction = handler.asDynamic()
}

/**
 * Adds an onMouseEnter handler.
 *
 * Fired when the pointer enters the element. Does NOT bubble — see [onMouseOver] for the bubbling variant.
 *
 * https://developer.mozilla.org/en-US/docs/Web/API/Element/mouseenter_event
 */
fun CommonAttributeGroupFacade.onMouseEnter(handler: (MouseEvent) -> Unit) {
    onMouseEnterFunction = handler.asDynamic()
}

/**
 * Adds an onMouseLeave handler.
 *
 * Fired when the pointer leaves the element. Does NOT bubble — see [onMouseOut] for the bubbling variant.
 *
 * https://developer.mozilla.org/en-US/docs/Web/API/Element/mouseleave_event
 */
fun CommonAttributeGroupFacade.onMouseLeave(handler: (MouseEvent) -> Unit) {
    onMouseLeaveFunction = handler.asDynamic()
}

/**
 * Adds an onMouseMove handler.
 *
 * Fired when the pointer moves while over the element.
 *
 * https://developer.mozilla.org/en-US/docs/Web/API/Element/mousemove_event
 */
fun CommonAttributeGroupFacade.onMouseMove(handler: (MouseEvent) -> Unit) {
    onMouseMoveFunction = handler.asDynamic()
}

/**
 * Adds an onMouseOver handler.
 *
 * Fired when the pointer enters the element or one of its children. DOES bubble — see [onMouseEnter] for the
 * non-bubbling variant.
 *
 * https://developer.mozilla.org/en-US/docs/Web/API/Element/mouseover_event
 */
fun CommonAttributeGroupFacade.onMouseOver(handler: (MouseEvent) -> Unit) {
    onMouseOverFunction = handler.asDynamic()
}

/**
 * Adds an onMouseOut handler.
 *
 * Fired when the pointer leaves the element or one of its children. DOES bubble — see [onMouseLeave] for the
 * non-bubbling variant.
 *
 * https://developer.mozilla.org/en-US/docs/Web/API/Element/mouseout_event
 */
fun CommonAttributeGroupFacade.onMouseOut(handler: (MouseEvent) -> Unit) {
    onMouseOutFunction = handler.asDynamic()
}

/**
 * Adds an onMouseUp handler.
 *
 * Fired when a pointing device button is released over an element.
 *
 * https://developer.mozilla.org/en-US/docs/Web/API/Element/mouseup_event
 */
fun CommonAttributeGroupFacade.onMouseUp(handler: (MouseEvent) -> Unit) {
    onMouseUpFunction = handler.asDynamic()
}

/**
 * Adds an onWheel handler.
 *
 * Fired when the mouse wheel or similar device is rotated.
 *
 * https://developer.mozilla.org/en-US/docs/Web/API/Element/wheel_event
 */
fun CommonAttributeGroupFacade.onWheel(handler: (WheelEvent) -> Unit) {
    onWheelFunction = handler.asDynamic()
}

// =====================================================================================================================
// Pointer events
// https://developer.mozilla.org/en-US/docs/Web/API/Element#pointer_events
// =====================================================================================================================

/**
 * Adds an onPointerDown handler.
 *
 * Fired when a pointer becomes active (mouse button pressed, touch contact, pen contact).
 *
 * https://developer.mozilla.org/en-US/docs/Web/API/Element/pointerdown_event
 */
fun CommonAttributeGroupFacade.onPointerDown(handler: (PointerEvent) -> Unit) {
    consumer.onTagEvent(this, "onpointerdown", handler.asDynamic())
}

/**
 * Adds an onPointerUp handler.
 *
 * Fired when a pointer is no longer active.
 *
 * https://developer.mozilla.org/en-US/docs/Web/API/Element/pointerup_event
 */
fun CommonAttributeGroupFacade.onPointerUp(handler: (PointerEvent) -> Unit) {
    consumer.onTagEvent(this, "onpointerup", handler.asDynamic())
}

/**
 * Adds an onPointerMove handler.
 *
 * Fired when a pointer changes coordinates.
 *
 * https://developer.mozilla.org/en-US/docs/Web/API/Element/pointermove_event
 */
fun CommonAttributeGroupFacade.onPointerMove(handler: (PointerEvent) -> Unit) {
    consumer.onTagEvent(this, "onpointermove", handler.asDynamic())
}

/**
 * Adds an onPointerEnter handler.
 *
 * Fired when a pointer enters the element's hit-test area. Does NOT bubble.
 *
 * https://developer.mozilla.org/en-US/docs/Web/API/Element/pointerenter_event
 */
fun CommonAttributeGroupFacade.onPointerEnter(handler: (PointerEvent) -> Unit) {
    consumer.onTagEvent(this, "onpointerenter", handler.asDynamic())
}

/**
 * Adds an onPointerLeave handler.
 *
 * Fired when a pointer leaves the element's hit-test area. Does NOT bubble.
 *
 * https://developer.mozilla.org/en-US/docs/Web/API/Element/pointerleave_event
 */
fun CommonAttributeGroupFacade.onPointerLeave(handler: (PointerEvent) -> Unit) {
    consumer.onTagEvent(this, "onpointerleave", handler.asDynamic())
}

/**
 * Adds an onPointerOver handler.
 *
 * Fired when a pointer enters the element's hit-test area. DOES bubble.
 *
 * https://developer.mozilla.org/en-US/docs/Web/API/Element/pointerover_event
 */
fun CommonAttributeGroupFacade.onPointerOver(handler: (PointerEvent) -> Unit) {
    consumer.onTagEvent(this, "onpointerover", handler.asDynamic())
}

/**
 * Adds an onPointerOut handler.
 *
 * Fired when a pointer leaves the element's hit-test area. DOES bubble.
 *
 * https://developer.mozilla.org/en-US/docs/Web/API/Element/pointerout_event
 */
fun CommonAttributeGroupFacade.onPointerOut(handler: (PointerEvent) -> Unit) {
    consumer.onTagEvent(this, "onpointerout", handler.asDynamic())
}

/**
 * Adds an onPointerCancel handler.
 *
 * Fired when the browser determines there are unlikely to be any more pointer events.
 *
 * https://developer.mozilla.org/en-US/docs/Web/API/Element/pointercancel_event
 */
fun CommonAttributeGroupFacade.onPointerCancel(handler: (PointerEvent) -> Unit) {
    consumer.onTagEvent(this, "onpointercancel", handler.asDynamic())
}

// =====================================================================================================================
// Touch events
// https://developer.mozilla.org/en-US/docs/Web/API/Element#touch_events
// =====================================================================================================================

/**
 * Adds an onTouchStart handler.
 *
 * Fired when one or more touch points are placed on the touch surface.
 *
 * https://developer.mozilla.org/en-US/docs/Web/API/Element/touchstart_event
 */
fun CommonAttributeGroupFacade.onTouchStart(handler: (Event) -> Unit) {
    consumer.onTagEvent(this, "ontouchstart", handler.asDynamic())
}

/**
 * Adds an onTouchMove handler.
 *
 * Fired when one or more touch points are moved along the touch surface.
 *
 * https://developer.mozilla.org/en-US/docs/Web/API/Element/touchmove_event
 */
fun CommonAttributeGroupFacade.onTouchMove(handler: (Event) -> Unit) {
    consumer.onTagEvent(this, "ontouchmove", handler.asDynamic())
}

/**
 * Adds an onTouchEnd handler.
 *
 * Fired when one or more touch points are removed from the touch surface.
 *
 * https://developer.mozilla.org/en-US/docs/Web/API/Element/touchend_event
 */
fun CommonAttributeGroupFacade.onTouchEnd(handler: (Event) -> Unit) {
    consumer.onTagEvent(this, "ontouchend", handler.asDynamic())
}

/**
 * Adds an onTouchCancel handler.
 *
 * Fired when one or more touch points are disrupted (e.g. too many touch points).
 *
 * https://developer.mozilla.org/en-US/docs/Web/API/Element/touchcancel_event
 */
fun CommonAttributeGroupFacade.onTouchCancel(handler: (Event) -> Unit) {
    consumer.onTagEvent(this, "ontouchcancel", handler.asDynamic())
}

// =====================================================================================================================
// Scroll & Load events
// =====================================================================================================================

/**
 * Adds an onScroll handler.
 *
 * Fired when the element is scrolled.
 *
 * https://developer.mozilla.org/en-US/docs/Web/API/Element/scroll_event
 */
fun CommonAttributeGroupFacade.onScroll(handler: (Event) -> Unit) {
    consumer.onTagEvent(this, "onscroll", handler.asDynamic())
}

/**
 * Adds an onLoad handler.
 *
 * Fired when a resource (e.g. image, script, iframe) has finished loading.
 *
 * https://developer.mozilla.org/en-US/docs/Web/API/HTMLElement/load_event
 */
fun CommonAttributeGroupFacade.onLoad(handler: (Event) -> Unit) {
    consumer.onTagEvent(this, "onload", handler.asDynamic())
}

/**
 * Adds an onError handler.
 *
 * Fired when a resource fails to load.
 *
 * https://developer.mozilla.org/en-US/docs/Web/API/HTMLElement/error_event
 */
fun CommonAttributeGroupFacade.onError(handler: (Event) -> Unit) {
    onErrorFunction = handler.asDynamic()
}
