package de.peekandpoke.kraft.semanticui

import kotlinx.html.FlowContent
import kotlinx.html.Tag
import kotlin.jvm.JvmName

@SemanticUiDslMarker
val Tag.ui: SemanticTag
    get() = SemanticTag(this, mutableListOf("ui"))

@SemanticUiDslMarker
val Tag.noui: SemanticTag
    get() = SemanticTag(this, mutableListOf(""))

@SemanticIconMarker
val FlowContent.icon: SemanticIcon
    get() = SemanticIcon(this)

@SemanticIconMarker
val FlowContent.emoji: SemanticEmoji
    get() = SemanticEmoji(this)

@SemanticIconMarker
val FlowContent.flag: SemanticFlag
    get() = SemanticFlag(this)

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
 * Shorthand type
 */
typealias SemanticFn = SemanticTag.() -> SemanticTag

/**
 * Helps the compiler to identify a code block that is supposed to run on a semantic tag
 */
fun semantic(block: SemanticFn): SemanticFn = block

/**
 * Shorthand type
 */
typealias SemanticIconFn = SemanticIcon.() -> SemanticIcon

/**
 * Helps the compiler to identify a code block that is supposed to run on a semantic tag
 */
fun semanticIcon(block: SemanticIconFn): SemanticIconFn = block

/**
 * Shorthand type
 */
typealias SemanticEmojiFn = SemanticEmoji.() -> SemanticEmoji

/**
 * Helps the compiler to identify a code block that is supposed to run on a semantic tag
 */
fun semanticEmoji(block: SemanticEmojiFn): SemanticEmojiFn = block
