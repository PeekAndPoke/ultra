package de.peekandpoke.funktor.auth.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class AuthProviderModel(
    val id: String,
    val type: String,
    val capabilities: Set<Capability>,
    val config: JsonObject? = null,
) {
    @Serializable
    enum class Capability {
        SignIn,
        SignUp,
    }

    companion object {
        const val TYPE_EMAIL_PASSWORD = "email-password"
        const val TYPE_GOOGLE = "google"
        const val TYPE_GITHUB = "github"
    }
}
