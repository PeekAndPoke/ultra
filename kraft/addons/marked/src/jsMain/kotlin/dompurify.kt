package io.peekandpoke.kraft.addons.marked

import js.objects.Object

@Suppress("ClassName")
@JsModule("dompurify")
@JsNonModule
/** External binding for DOMPurify, an XSS sanitizer for HTML. */
external object dompurify {
    /** Sanitizes [dirty] HTML, removing potentially dangerous content. */
    fun sanitize(dirty: String): String

    /** Sanitizes [dirty] HTML with the given DOMPurify [config]. */
    fun sanitize(dirty: String, config: Object?): String
}
