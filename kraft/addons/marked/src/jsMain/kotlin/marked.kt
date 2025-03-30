package de.peekandpoke.kraft.addons.marked

// Type-Definitions for marked-js

@Suppress("ClassName")
@JsModule("marked")
@JsNonModule
external object marked {
    /**
     * Configure marked https://marked.js.org/using_advanced#options
     */
    fun use(settings: dynamic)

    /**
     * Parses the [content] as markdown and returns html as string
     */
    fun parse(content: String): String
}
