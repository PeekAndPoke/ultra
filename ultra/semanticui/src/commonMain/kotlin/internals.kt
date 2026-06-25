package io.peekandpoke.ultra.semanticui

import kotlinx.html.Tag
import kotlinx.html.visitTag

@PublishedApi
internal fun <T : Tag> T.visitNoInline(block: T.() -> Unit) {
    visitTag(block)
}
