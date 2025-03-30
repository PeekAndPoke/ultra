package de.peekandpoke.kraft.utils

import de.peekandpoke.kraft.streams.StreamSource
import kotlinext.js.js
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import org.w3c.dom.HTMLScriptElement

object ScriptLoader {

    sealed interface Javascript {

        data class Default(
            override val src: String,
            override val type: String = "text/javascript",
            val integrity: String? = null,
            val crossOrigin: String? = "anonymous",
            val referrerPolicy: String? = "no-referrer",
        ) : Javascript

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

    sealed interface LoadedJavascript {
        data class Default(
            val script: Javascript.Default,
            override val tag: HTMLScriptElement,
        ) : LoadedJavascript

        data class Module<T>(
            val script: Javascript.Module<T>,
            override val tag: HTMLScriptElement,
            val module: T,
        ) : LoadedJavascript

        val tag: HTMLScriptElement
    }

    class Error(message: String, val details: Any? = null) : Throwable(message = message)

    private var callbackCounter = 0

    private val jsMap = mutableMapOf<Javascript, Pair<HTMLScriptElement, Deferred<LoadedJavascript>>>()

    private val jsSource = StreamSource<Set<Javascript>>(emptySet())

    val javascripts = jsSource.readonly

    fun hasLoaded(script: Javascript): Boolean {
        return jsMap.containsKey(script)
    }

    fun loadJavascript(src: String): Deferred<LoadedJavascript.Default> {
        return load(
            Javascript.Default(src = src)
        )
    }

    fun load(script: Javascript.Default): Deferred<LoadedJavascript.Default> {
        val deferred = jsMap.getOrPut(script) {
            mount(script)
        }

        jsSource(jsMap.keys)

        @Suppress("UNCHECKED_CAST")
        return deferred.second as Deferred<LoadedJavascript.Default>
    }

    fun <T : Any> load(script: Javascript.Module<T>): Deferred<LoadedJavascript.Module<T>> {
        val deferred = jsMap.getOrPut(script) {
            mount(script)
        }

        jsSource(jsMap.keys)

        @Suppress("UNCHECKED_CAST")
        return deferred.second as Deferred<LoadedJavascript.Module<T>>
    }

    fun unloadJavascript(src: String) {
        unload(
            Javascript.Default(src = src)
        )
    }

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
        win.___ScriptLoader___ = win.___ScriptLoader___ ?: js { }

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
