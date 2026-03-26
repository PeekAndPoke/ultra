package io.peekandpoke.kraft.testing

import org.w3c.dom.Element
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLSelectElement
import org.w3c.dom.HTMLTextAreaElement
import org.w3c.dom.asList
import org.w3c.dom.events.Event

// =====================================================================================================================
// Text content
// =====================================================================================================================

/** Returns the concatenated text content of all elements, joined by [glue]. */
fun <E : Element> KQuery<E>.textContent(glue: String = ""): String {
    return mapNotNull { it.textContent }
        .joinToString(glue) { it }
}

/** Returns true if any element's text content contains the given [text]. */
fun <E : Element> KQuery<E>.containsText(text: String): Boolean {
    return any { it.textContent?.contains(text) ?: false }
}

// =====================================================================================================================
// Attributes
// =====================================================================================================================

/** Returns the value of the [name] attribute for each matched element (null when absent). */
fun <E : Element> KQuery<E>.attr(name: String): List<String?> =
    map { it.getAttribute(name) }

/** Returns true if every matched element has the [name] attribute. */
fun <E : Element> KQuery<E>.allHaveAttr(name: String): Boolean =
    isNotEmpty() && all { it.hasAttribute(name) }

/** Returns true if any matched element has the [name] attribute. */
fun <E : Element> KQuery<E>.anyHasAttr(name: String): Boolean =
    any { it.hasAttribute(name) }

// =====================================================================================================================
// CSS classes
// =====================================================================================================================

/** Returns the set of CSS classes for each matched element. */
fun <E : Element> KQuery<E>.classes(): List<Set<String>> =
    map { it.classList.asList().toSet() }

/** Returns true if every matched element has the given CSS [className]. */
fun <E : Element> KQuery<E>.allHaveClass(className: String): Boolean =
    isNotEmpty() && all { it.classList.contains(className) }

/** Returns true if any matched element has the given CSS [className]. */
fun <E : Element> KQuery<E>.anyHasClass(className: String): Boolean =
    any { it.classList.contains(className) }

// =====================================================================================================================
// Form values
// =====================================================================================================================

/** Returns the form value for each matched input, textarea, or select element. */
fun <E : Element> KQuery<E>.values(): List<String?> = map { el ->
    when (el) {
        is HTMLInputElement -> el.value
        is HTMLTextAreaElement -> el.value
        is HTMLSelectElement -> el.value
        else -> null
    }
}

/**
 * Sets the value on all matched input, textarea, or select elements.
 *
 * Does NOT fire input/change events — use [typeText] for that.
 */
fun <E : Element> KQuery<E>.setValue(value: String): KQuery<E> = apply {
    forEach { el ->
        when (el) {
            is HTMLInputElement -> el.value = value
            is HTMLTextAreaElement -> el.value = value
            is HTMLSelectElement -> el.value = value
        }
    }
}

/**
 * Simulates typing into all matched input/textarea elements.
 *
 * Sets the value, then dispatches `input` and `change` events (bubbling) so that
 * Kraft components pick up the change.
 */
fun <E : Element> KQuery<E>.typeText(text: String): KQuery<E> = apply {
    val init: dynamic = js("{}")
    init.bubbles = true

    forEach { el ->
        when (el) {
            is HTMLInputElement -> {
                el.value = text
                el.dispatchEvent(Event("input", init))
                el.dispatchEvent(Event("change", init))
            }

            is HTMLTextAreaElement -> {
                el.value = text
                el.dispatchEvent(Event("input", init))
                el.dispatchEvent(Event("change", init))
            }
        }
    }
}

// =====================================================================================================================
// Checked state (checkboxes and radio buttons)
// =====================================================================================================================

/** Returns the checked state for each matched input element. Non-input elements return false. */
fun <E : Element> KQuery<E>.checkedStates(): List<Boolean> =
    map { (it as? HTMLInputElement)?.checked ?: false }

/** Returns true if every matched input element is checked. */
fun <E : Element> KQuery<E>.allChecked(): Boolean =
    isNotEmpty() && all { (it as? HTMLInputElement)?.checked ?: false }

/** Returns true if any matched input element is checked. */
fun <E : Element> KQuery<E>.anyChecked(): Boolean =
    any { (it as? HTMLInputElement)?.checked ?: false }

/** Sets checked = true on all matched input elements and fires a `change` event. */
fun <E : Element> KQuery<E>.check(): KQuery<E> = setChecked(true)

/** Sets checked = false on all matched input elements and fires a `change` event. */
fun <E : Element> KQuery<E>.uncheck(): KQuery<E> = setChecked(false)

private fun <E : Element> KQuery<E>.setChecked(checked: Boolean): KQuery<E> = apply {
    val init: dynamic = js("{}")
    init.bubbles = true

    filterIsInstance<HTMLInputElement>().forEach { el ->
        el.checked = checked
        el.dispatchEvent(Event("change", init))
    }
}

// =====================================================================================================================
// Enabled / Disabled
// =====================================================================================================================

/** Returns the disabled state for each matched element. */
fun <E : Element> KQuery<E>.disabledStates(): List<Boolean> = map { el ->
    when (el) {
        is HTMLInputElement -> el.disabled
        is HTMLTextAreaElement -> el.disabled
        is HTMLSelectElement -> el.disabled
        else -> el.hasAttribute("disabled")
    }
}

/** Returns true if every matched element is disabled. */
fun <E : Element> KQuery<E>.allDisabled(): Boolean =
    isNotEmpty() && disabledStates().all { it }

/** Returns true if any matched element is disabled. */
fun <E : Element> KQuery<E>.anyDisabled(): Boolean =
    disabledStates().any { it }

/** Returns true if every matched element is enabled (not disabled). */
fun <E : Element> KQuery<E>.allEnabled(): Boolean =
    isNotEmpty() && disabledStates().none { it }

/** Returns true if any matched element is enabled. */
fun <E : Element> KQuery<E>.anyEnabled(): Boolean =
    disabledStates().any { !it }

// =====================================================================================================================
// Visibility
// =====================================================================================================================

/**
 * Returns the visibility state for each matched element.
 *
 * An element is considered visible if it has a non-null `offsetParent`, or non-zero offset dimensions.
 */
fun <E : Element> KQuery<E>.visibilityStates(): List<Boolean> = map { el ->
    (el as? HTMLElement)?.let {
        it.offsetParent != null || it.offsetWidth > 0 || it.offsetHeight > 0
    } ?: false
}

/** Returns true if every matched element is visible. */
fun <E : Element> KQuery<E>.allVisible(): Boolean =
    isNotEmpty() && visibilityStates().all { it }

/** Returns true if any matched element is visible. */
fun <E : Element> KQuery<E>.anyVisible(): Boolean =
    visibilityStates().any { it }

// =====================================================================================================================
// HTML content
// =====================================================================================================================

/** Returns the innerHTML for each matched element. */
fun <E : Element> KQuery<E>.innerHTML(): List<String> =
    map { it.innerHTML }

/** Returns the outerHTML for each matched element. */
fun <E : Element> KQuery<E>.outerHTML(): List<String> =
    map { it.outerHTML }

// =====================================================================================================================
// DOM traversal
// =====================================================================================================================

/** Returns a KQuery wrapping the parent elements of all matched elements (deduplicated). */
fun <E : Element> KQuery<E>.parents(): KQuery<Element> =
    KQuery(mapNotNull { it.parentElement }.distinct())

/** Returns a KQuery wrapping all direct child elements of all matched elements. */
fun <E : Element> KQuery<E>.children(): KQuery<Element> =
    KQuery(flatMap { it.children.asList() })

/** Returns a KQuery wrapping only the first matched element, or empty. */
fun <E : Element> KQuery<E>.first(): KQuery<E> =
    KQuery(listOfNotNull(firstOrNull()))

/** Returns a KQuery wrapping only the last matched element, or empty. */
fun <E : Element> KQuery<E>.last(): KQuery<E> =
    KQuery(listOfNotNull(lastOrNull()))

/** Returns a KQuery wrapping the element at the given [index], or empty. */
fun <E : Element> KQuery<E>.nth(index: Int): KQuery<E> =
    KQuery(listOfNotNull(getOrNull(index)))

/** Filters matched elements by a predicate, returning a new KQuery. */
fun <E : Element> KQuery<E>.filterElements(predicate: (E) -> Boolean): KQuery<E> =
    KQuery(filter(predicate))
