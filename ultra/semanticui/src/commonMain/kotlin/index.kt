package de.peekandpoke.ultra.semanticui

import kotlinx.html.FlowContent
import kotlinx.html.Tag

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
