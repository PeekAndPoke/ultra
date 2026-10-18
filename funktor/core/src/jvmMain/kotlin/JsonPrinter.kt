package io.peekandpoke.funktor.core

import io.peekandpoke.ultra.slumber.Codec
import io.peekandpoke.ultra.slumber.JsonUtil.toJsonElement
import io.peekandpoke.ultra.slumber.slumber
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

/**
 * Pretty-prints arbitrary objects as JSON.
 *
 * Slumber turns the object into a plain tree; kotlinx renders that tree as text. There is no second
 * serializer involved — the Jackson `ObjectMapper` this used to carry only ever did the rendering step,
 * which [toJsonElement] already covers.
 *
 * **The Jackson fallback was also a hole.** It read
 * `writeValueAsString(try { codec.slumber(obj) } catch { obj })` — so whenever Slumber could not
 * describe a value, the RAW object went to Jackson, which reflects over anything. That path bypassed
 * every Slumber codec, `Redacted` included, and `AppConfigCliCommand` prints the whole `AppConfig`
 * through here. Now a value Slumber cannot describe reports an error instead of being dumped.
 */
object JsonPrinter {

    private val json = Json { prettyPrint = true }

    private val codec = Codec.default

    fun prettyPrint(obj: Any?): String = try {
        json.encodeToString(JsonElement.serializer(), codec.slumber(obj).toJsonElement())
    } catch (e: Throwable) {
        // The CLASS, never the value. This branch runs precisely when slumbering failed — i.e. when the
        // redacting codecs did NOT run — so interpolating `$obj` here would dump in the clear whatever
        // the successful path exists to protect. Reachable from `app:config` and the admin APIs.
        "Could not pretty print an instance of: ${obj?.let { it::class.qualifiedName } ?: "null"} \n" +
                e.stackTraceToString()
    }
}
