package io.peekandpoke.kraft.utils

import io.peekandpoke.ultra.streams.StreamSource
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import org.w3c.dom.HTMLScriptElement

/**
 * Global loader for dynamically injecting JavaScript and ES module scripts into the document head.
 *
 * Tracks loaded scripts and prevents duplicate loading for the same source.
 */
object ScriptLoader {

    /** Descriptor for a JavaScript resource to load. */
    sealed interface Javascript {

        /** A standard JavaScript file loaded via a `<script>` tag. */
        data class Default(
            override val src: String,
            override val type: String = "text/javascript",
            val integrity: String? = null,
            val crossOrigin: String? = "anonymous",
            val referrerPolicy: String? = "no-referrer",
        ) : Javascript

        /** An ES module loaded via a `<script type="module">` tag, with the module exports available as [T]. */
        data class Module<T>(
            override val src: String,
            val integrity: String? = null,
            val crossOrigin: String? = "anonymous",
            val referrerPolicy: String? = "no-referrer",
        ) : Javascript {
            override val type get() = "module"
        }

        val src: String
        val type: String
    }

    /** Result of a successfully loaded JavaScript resource. */
    sealed interface LoadedJavascript {
        /** A loaded standard JavaScript file. */
        data class Default(
            val script: Javascript.Default,
            override val tag: HTMLScriptElement,
        ) : LoadedJavascript

        /** A loaded ES module, with its exports available via [module]. */
        data class Module<T>(
            val script: Javascript.Module<T>,
            override val tag: HTMLScriptElement,
            val module: T,
        ) : LoadedJavascript

        /** The `<script>` element that was injected into the DOM. */
        val tag: HTMLScriptElement
    }

    /** Error thrown when a script fails to load. */
    class Error(message: String, val details: Any? = null) : Throwable(message = message)

    private var callbackCounter = 0

    private val jsMap = mutableMapOf<Javascript, Pair<HTMLScriptElement, Deferred<LoadedJavascript>>>()

    private val jsSource = StreamSource<Set<Javascript>>(emptySet())

    /** A stream of all currently loaded JavaScript descriptors. */
    val javascripts = jsSource.readonly

    /** Returns true if the given [script] has already been loaded. */
    fun hasLoaded(script: Javascript): Boolean {
        return jsMap.containsKey(script)
    }

    /** Loads a JavaScript file from the given [src] URL. */
    fun loadJavascript(src: String): Deferred<LoadedJavascript.Default> {
        return load(
            Javascript.Default(src = src)
        )
    }

    /** Loads a standard JavaScript [script], returning a [Deferred] that completes when it is ready. */
    fun load(script: Javascript.Default): Deferred<LoadedJavascript.Default> {
        val deferred = jsMap.getOrPut(script) {
            mount(script)
        }

        jsSource(jsMap.keys)

        @Suppress("UNCHECKED_CAST")
        return deferred.second as Deferred<LoadedJavascript.Default>
    }

    /** Loads an ES module [script], returning a [Deferred] that completes with the module exports. */
    fun <T : Any> load(script: Javascript.Module<T>): Deferred<LoadedJavascript.Module<T>> {
        val deferred = jsMap.getOrPut(script) {
            mount(script)
        }

        jsSource(jsMap.keys)

        @Suppress("UNCHECKED_CAST")
        return deferred.second as Deferred<LoadedJavascript.Module<T>>
    }

    /** Unloads a JavaScript file by its [src] URL, removing the script tag from the DOM. */
    fun unloadJavascript(src: String) {
        unload(
            Javascript.Default(src = src)
        )
    }

    /** Unloads the given [javascript] resource, removing its script tag from the DOM. */
    fun unload(javascript: Javascript) {
        jsMap.remove(javascript)?.let {
            it.first.remove()
            jsSource(jsMap.keys)
        }
    }

    private fun mount(script: Javascript.Default): Pair<HTMLScriptElement, Deferred<LoadedJavascript.Default>> {

        val deferred = CompletableDeferred<LoadedJavascript.Default>()

        val tag = (document.createElement("script") as HTMLScriptElement)
        tag.src = script.src
        tag.type = script.type
        script.integrity?.let { tag.asDynamic()["integrity"] = it }
        script.crossOrigin?.let { tag.crossOrigin = it }
        script.referrerPolicy?.let { tag.asDynamic().referrerPolicy = it }

        tag.onload = { _ ->
            deferred.complete(
                LoadedJavascript.Default(script = script, tag = tag)
            )
        }

        tag.onerror = { event, url, line, col, errorObj ->
            val details = mapOf(
                "event" to event,
                "url" to url,
                "line" to line,
                "col" to col,
                "errorObj" to errorObj,
            )

            deferred.completeExceptionally(
                Error("Error loading script ${script.src}", details.js)
            )

            launch {
                unload(script)
            }
        }

        document.head?.appendChild(tag)

        return tag to deferred
    }

    private fun <T : Any> mount(
        script: Javascript.Module<T>,
    ): Pair<HTMLScriptElement, Deferred<LoadedJavascript.Module<T>>> {

        val deferred = CompletableDeferred<LoadedJavascript.Module<T>>()

        val tag = (document.createElement("script") as HTMLScriptElement)
        tag.type = script.type
        script.integrity?.let { tag.asDynamic()["integrity"] = it }
        script.crossOrigin?.let { tag.crossOrigin = it }
        script.referrerPolicy?.let { tag.asDynamic().referrerPolicy = it }

        val win = window.asDynamic()
        win.___ScriptLoader___ = win.___ScriptLoader___ ?: js("{}")

        val cbName = "cb_" + (callbackCounter++).toString()
        win.___ScriptLoader___[cbName] = { result: dynamic ->
            deferred.complete(
                LoadedJavascript.Module(
                    script = script,
                    tag = tag,
                    module = result,
                )
            )
        }

        tag.innerHTML = """
            var cb = window.___ScriptLoader___.$cbName
            import * as content from '${script.src}';
            cb(content, null);
        """.trimIndent()

        tag.onerror = { event, url, line, col, errorObj ->
            val details = mapOf(
                "event" to event,
                "url" to url,
                "line" to line,
                "col" to col,
                "errorObj" to errorObj,
            )

            deferred.completeExceptionally(
                Error("Error loading script ${script.src}", details.js)
            )

            launch {
                unload(script)
            }
        }

        document.head?.appendChild(tag)

        return tag to deferred
    }
}
