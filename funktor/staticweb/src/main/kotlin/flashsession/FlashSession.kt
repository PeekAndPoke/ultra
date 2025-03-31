package de.peekandpoke.funktor.staticweb.flashsession

import io.ktor.server.sessions.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

interface FlashSession {

    @Serializable
    data class Data(val entries: List<Entry> = listOf()) {
        @Serializable
        data class Entry(val message: String, val type: String)
    }

    fun pull(): List<Data.Entry>

    fun add(message: String, type: String)

    companion object {

        fun of(session: CurrentSession): FlashSession = SimpleFlashSession(session)

        fun register(config: SessionsConfig) {

            config.cookie<Data>("flash-messages") {

                cookie.path = "/"

                serializer = object : SessionSerializer<Data> {

                    override fun deserialize(text: String): Data =
                        try {
                            Json.decodeFromString(Data.serializer(), text)
                        } catch (_: Exception) {
                            Data()
                        }

                    override fun serialize(session: Data): String =
                        Json.encodeToString(Data.serializer(), session)
                }
            }
        }
    }
}

class NullFlashSession internal constructor() : FlashSession {

    override fun pull(): List<FlashSession.Data.Entry> = listOf()

    override fun add(message: String, type: String) = Unit
}

class SimpleFlashSession internal constructor(private val session: CurrentSession) : FlashSession {

    override fun pull(): List<FlashSession.Data.Entry> {

        val current = read()

        session.set(FlashSession.Data())

        return current.entries
    }

    override fun add(message: String, type: String) {

        val current = read()

        session.set(
            current.copy(
                entries = current.entries.plus(
                    FlashSession.Data.Entry(message, type)
                )
            )
        )
    }

    private fun read() = session.get<FlashSession.Data>() ?: FlashSession.Data()
}
