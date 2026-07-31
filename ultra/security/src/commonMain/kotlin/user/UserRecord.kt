package io.peekandpoke.ultra.security.user

import io.peekandpoke.ultra.common.slumber.Slumber
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Core identity record for a request, dispatched on the auth method that produced it. */
@Serializable
sealed interface UserRecord {
    @Slumber.Field
    val userId: UserId

    @Slumber.Field
    val clientIp: String?

    @Slumber.Field
    val email: EmailAddress? get() = null

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
        override val userId: UserId get() = ANONYMOUS_ID
    }

    /** Internal system actor (background jobs, internal calls). */
    @Serializable
    @SerialName("system")
    data class System(
        override val clientIp: String? = null,
    ) : UserRecord {
        override val userId: UserId get() = SYSTEM_ID
    }

    /** End-user authenticated via a JWT (or equivalent session). */
    @Serializable
    @SerialName("logged-in")
    data class LoggedIn(
        override val userId: UserId,
        override val clientIp: String? = null,
        override val email: EmailAddress? = null,
        override val desc: String? = null,
        override val type: String? = null,
    ) : UserRecord

    /** Caller authenticated via an API key. */
    @Serializable
    @SerialName("api-key")
    data class ApiKey(
        override val userId: UserId,
        override val clientIp: String? = null,
        val keyId: String,
        val keyName: String? = null,
        override val email: EmailAddress? = null,
        override val desc: String? = null,
        override val type: String? = null,
    ) : UserRecord

    companion object {
        /** User id for anonymous users. */
        val ANONYMOUS_ID: UserId = UserId("anonymous")

        /** User id for system users. */
        val SYSTEM_ID: UserId = UserId("system")

        /** Singleton anonymous user record. */
        val anonymous: UserRecord = Anonymous()

        /** Creates a system user record with the given [ip] address. */
        fun system(ip: String?): UserRecord = System(clientIp = ip)
    }
}
