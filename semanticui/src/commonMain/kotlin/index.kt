package de.peekandpoke.ultra.semanticui

import kotlinx.html.FlowContent

@SemanticUiDslMarker
val FlowContent.ui: SemanticTag
    get() = SemanticTag(this, mutableListOf("ui"))

@SemanticUiDslMarker
val FlowContent.noui: SemanticTag
    get() = SemanticTag(this, mutableListOf(""))

@SemanticIconMarker
val FlowContent.icon: SemanticIcon
    get() = SemanticIcon(this)

@SemanticIconMarker
val FlowContent.emoji: SemanticEmoji
    get() = SemanticEmoji(this)

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
