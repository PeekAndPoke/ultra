package io.peekandpoke.ultra.security.user

import io.peekandpoke.ultra.slumber.Slumber
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Core identity record for a request, dispatched on the auth method that produced it. */
@Serializable
sealed interface UserRecord {
    @Slumber.Field
    val userId: String

    @Slumber.Field
    val clientIp: String?

    @Slumber.Field
    val email: String? get() = null

    @Slumber.Field
    val desc: String? get() = null

    @Slumber.Field
    val type: String? get() = null

    fun isAnonymous(): Boolean = userId == ANONYMOUS_ID
    fun isSystem(): Boolean = userId == SYSTEM_ID

    /** Anonymous, unauthenticated caller. */
    @Serializable
    @SerialName("anonymous")
    data class Anonymous(
        override val clientIp: String? = null,
    ) : UserRecord {
        override val userId: String get() = ANONYMOUS_ID
    }

    /** Internal system actor (background jobs, internal calls). */
    @Serializable
    @SerialName("system")
    data class System(
        override val clientIp: String? = null,
    ) : UserRecord {
        override val userId: String get() = SYSTEM_ID
    }

    /** End-user authenticated via a JWT (or equivalent session). */
    @Serializable
    @SerialName("logged-in")
    data class LoggedIn(
        override val userId: String,
        override val clientIp: String? = null,
        override val email: String? = null,
        override val desc: String? = null,
        override val type: String? = null,
    ) : UserRecord

    /** Caller authenticated via an API key. */
    @Serializable
    @SerialName("api-key")
    data class ApiKey(
        override val userId: String,
        override val clientIp: String? = null,
        val keyId: String,
        val keyName: String? = null,
        override val email: String? = null,
        override val desc: String? = null,
        override val type: String? = null,
    ) : UserRecord

    companion object {
        /** User id for anonymous users. */
        const val ANONYMOUS_ID = "anonymous"

        /** User id for system users. */
        const val SYSTEM_ID = "system"

        /** Singleton anonymous user record. */
        val anonymous: UserRecord = Anonymous()

        /** Creates a system user record with the given [ip] address. */
        fun system(ip: String?): UserRecord = System(clientIp = ip)
    }
}
