package de.peekandpoke.ultra.html

import kotlinx.html.CommonAttributeGroupFacade
import kotlinx.html.FlowContent
import kotlinx.html.Tag
import kotlin.jvm.JvmName

/**
 * Helps the compiler to identify functions that operate on [FlowContent].
 */
typealias RenderFn = FlowContent.() -> Unit

/**
 * Helps the compiler to identify functions that operate on T.
 */
typealias RenderFunc<T> = T.() -> Unit

/**
 * Helps the compiler to identify a code block that is supposed to run on a semantic tag
 */
@JvmName("renderFn")
fun renderFn(block: RenderFn): RenderFn = block

/**
 * Helps the compiler to identify a code block that is supposed to run on a semantic tag
 */
@JvmName("renderFnT")
fun <T : Tag> renderFn(block: RenderFunc<T>): RenderFunc<T> = block

/**
 * Helps the compiler to identify a code block that is supposed to run on a [FlowContent]
 */
fun flowContent(block: FlowContent.() -> Unit) = block

/**
 * Markup Element key
 */
var CommonAttributeGroupFacade.key: String
    get() = attributes["key"] ?: ""
    set(value) {
        attributes["key"] = value
    }

/**
 * Gets the "debug-id" attribute
 */
fun CommonAttributeGroupFacade.debugId(): String? = attributes["debug-id"]

/**
 * Sets the "debug-id" attribute to [id]
 */
fun CommonAttributeGroupFacade.debugId(id: String) {
    attributes["debug-id"] = id
}

/**
 * Gets the "data-[id]" attribute
 */
fun CommonAttributeGroupFacade.data(id: String): String? = attributes["data-$id"]

/**
 * Sets the "data-[id]" attribute to [value]
 */
fun CommonAttributeGroupFacade.data(id: String, value: String) {
    attributes["data-$id"] = value
}
