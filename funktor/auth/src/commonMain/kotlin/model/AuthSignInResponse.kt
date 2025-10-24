package de.peekandpoke.funktor.auth.model

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

@Serializable
data class AuthSignInResponse(
    val token: Token,
    val realm: AuthRealmModel,
    val user: JsonObject,
) {
    @Serializable
    data class Token(
        val token: String,
        val permissionsNs: String,
        val userNs: String,
    )

    fun <T> getTypedUser(serializer: DeserializationStrategy<T>): T {
        return Json.decodeFromJsonElement(serializer, user)
    }
}
