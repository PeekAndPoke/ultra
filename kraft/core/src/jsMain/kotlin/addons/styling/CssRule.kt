package de.peekandpoke.kraft.addons.styling

open class CssRule(val name: String) {
    /** The formatted CSS selector, e.g., ".my-class_1a2b3c" */
    val selector: String get() = ".$name"

    override fun toString(): String = name

    operator fun invoke(): String = name
}
