package de.peekandpoke.ultra.semanticui

import kotlinx.html.Tag

@PublishedApi
internal fun <T : Tag> T.visitNoInline(block: T.() -> Unit) {
    consumer.onTagStart(this)
    try {
        this.block()
    } catch (err: Throwable) {
        consumer.onTagError(this, err)
    } finally {
        consumer.onTagEnd(this)
    }
}
