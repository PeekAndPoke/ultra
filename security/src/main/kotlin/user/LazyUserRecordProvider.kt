package de.peekandpoke.ultra.security.user

class LazyUserRecordProvider(provider: () -> UserRecord) : UserRecordProvider {

    private val userRecord by lazy { provider() }

    override fun invoke() = userRecord
}
