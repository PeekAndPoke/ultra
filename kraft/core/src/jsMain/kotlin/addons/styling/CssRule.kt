package io.peekandpoke.kraft.addons.styling

/**
 * Represents a CSS class rule with a mangled [name].
 */
open class CssRule(val name: String) {
    /** The formatted CSS selector, e.g., ".my-class_1a2b3c" */
    val selector: String get() = ".$name"

    /** Returns the CSS class name. */
    override fun toString(): String = name

    /** Returns the CSS class name, allowing the rule to be used as a function. */
    operator fun invoke(): String = name
}
