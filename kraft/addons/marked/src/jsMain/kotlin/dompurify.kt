package de.peekandpoke.kraft.addons.marked

import js.objects.Object

@Suppress("ClassName")
@JsModule("dompurify")
@JsNonModule
external object dompurify {
    fun sanitize(dirty: String): String

    fun sanitize(dirty: String, config: Object?): String
}
