package de.peekandpoke.ktorfx.rest.docs.postman

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Suppress("unused")
@Serializable
data class PostmanDocs(
    val info: Info,
    val item: List<Item>,
    val auth: Auth = Auth.None(),
    val event: List<Event> = emptyList(),
    val protocolProfileBehavior: ProtocolProfileBehavior = ProtocolProfileBehavior(),
) {
    @Serializable
    data class Info(
        val name: String,
        val schema: String = "https://schema.getpostman.com/json/collection/v2.1.0/collection.json",
    )

    @Serializable
    sealed class Auth {

        companion object {
            fun jwt(token: String) = Jwt(bearer = listOf(Jwt.Entry(key = "token", value = token)))
        }

        @Serializable
        @SerialName("none")
        data class None(val type: String? = null) : Auth()

        @Serializable
        @SerialName("jwt")
        data class Jwt(val type: String = "bearer", val bearer: List<Entry>) : Auth() {
            @Serializable
            data class Entry(
                val key: String,
                val value: String,
                val type: String = "text",
            )
        }
    }

    @Serializable
    class Event

    @Serializable
    class ProtocolProfileBehavior

    @Serializable
    sealed class Item {

        @Serializable
        @SerialName("folder")
        data class Folder(
            val name: String,
            val description: String = "",
            val item: List<Item>? = emptyList(),
        ) : Item()

        @Serializable
        @SerialName("endpoint")
        data class Endpoint(
            val name: String,
            val request: Request? = null,
        ) : Item()
    }

    @Serializable
    data class Request(
        val method: String,
        val url: Url,
        val description: Description? = null,
        val header: List<Header> = emptyList(),
        val data: String? = null,
    )

    @Serializable
    data class Header(
        val key: String,
        val value: String,
        val type: String = "text",
    ) {
        companion object {
            object ContentType {
                val applicationJson = Header("Content-Type", "application/json")
            }
        }
    }

    @Serializable
    data class Url(
        val raw: String,
        val host: List<String>,
        val path: List<String>,
        val query: List<QueryParam>? = null,
    ) {
        companion object {
            fun of(host: String, path: String) = Url(
                raw = "$host/$path",
                host = listOf(host),
                path = path.split("/")
            )
        }

        fun withQuery(query: List<QueryParam>?) = copy(query = query)
    }

    @Serializable
    data class Description(
        val type: String,
        val content: String,
        val version: String? = null,
    ) {
        companion object {
            fun markdown(content: String) = Description(
                type = "text/markdown",
                content = content
            )
        }
    }

    @Serializable
    data class QueryParam(
        val key: String,
        val value: String = "",
        val description: String = "",
    )
}
