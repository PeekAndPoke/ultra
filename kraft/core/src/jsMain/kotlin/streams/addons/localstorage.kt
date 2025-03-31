package de.peekandpoke.kraft.streams.addons

import de.peekandpoke.kraft.streams.PersistentStreamSource
import de.peekandpoke.kraft.streams.StreamSource
import de.peekandpoke.kraft.streams.StreamStorage
import kotlinx.browser.window
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import org.w3c.dom.get
import org.w3c.dom.set

/**
 * Default json codec used for the local storage
 */
private val defaultJson: Json = Json {
    ignoreUnknownKeys = true
}

/**
 * Converts a [StreamSource] so that it will persist its values in the local storage under the given [key].
 *
 * Every published value is sent to the local storage.
 * When the stream is initialized the latest value will be loaded from the local storage.
 */
fun <T> StreamSource<T>.persistInLocalStorage(
    key: String,
    serializer: KSerializer<T>,
    codec: Json = defaultJson,
): StreamSource<T> {

    val storage = LocalStorageStreamStorage(
        toStr = {
            codec.encodeToString(serializer, it)
        },
        fromStr = {
            it?.let { nonNull -> codec.decodeFromString(serializer, nonNull) }
        },
        storageKey = key
    )

    return PersistentStreamSource(
        storage = storage,
        wrapped = this
    )
}

class LocalStorageStreamStorage<T>(
    private val toStr: (T) -> String,
    private val fromStr: (String?) -> T?,
    private val storageKey: String,
) : StreamStorage<T> {

    /**
     * We keep the value in memory, in case the local storage is not supported or the quota is exceeded
     */
    private var lastValue: T? = null

    override fun read(): T? {
        try {
            lastValue = fromStr(window.localStorage[storageKey])
        } catch (e: Throwable) {
            console.warn(e)
        }

        return lastValue
    }

    override fun write(value: T) {
        lastValue = value

        try {
            window.localStorage[storageKey] = toStr(value)
        } catch (e: Throwable) {
            console.warn(e)
        }
    }
}

