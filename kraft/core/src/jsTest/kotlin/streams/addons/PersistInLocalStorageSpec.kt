package de.peekandpoke.kraft.streams.addons

import de.peekandpoke.kraft.streams.StreamSource
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer

class PersistInLocalStorageSpec : StringSpec() {

    @Serializable
    data class PersistedObject(
        val test: String,
    )

    init {

        "Persisting and reloading a simple value from local storage must work" {

            val key = "int-key"

            // We initialize the stream with 100
            val original = StreamSource(100).persistInLocalStorage(key, Int.serializer())
            // Then we send 200, which should be stored in the local storage
            original(200)

            // Then we create another persistent stream on the same key
            val copy = StreamSource(100).persistInLocalStorage(key, Int.serializer())
            // And the value should be the last value that was sent to the stream
            copy.subscribeToStream {
                it shouldBe 200
            }

            copy.removeAllSubscriptions()
        }

        "Persisting and reloading an object from local storage must work" {

            val key = "object-key"

            // We initialize the stream with 100
            val original = StreamSource(
                PersistedObject("first")
            ).persistInLocalStorage(key, PersistedObject.serializer())

            // Then we send another value
            val persisted = PersistedObject("second")
            original(persisted)

            // Then we create another persistent stream on the same key
            val copy = StreamSource(
                PersistedObject("")
            ).persistInLocalStorage(key, PersistedObject.serializer())

            // And the value should be the last value that was sent to the stream
            copy.subscribeToStream {
                it shouldBe persisted
            }

            copy.removeAllSubscriptions()
        }
    }
}
