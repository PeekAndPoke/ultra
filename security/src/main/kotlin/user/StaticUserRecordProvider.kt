package de.peekandpoke.ultra.security.user

class StaticUserRecordProvider(private val user: UserRecord) : UserRecordProvider {
    override fun invoke() = user
}
