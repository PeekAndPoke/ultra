package de.peekandpoke.ultra.security.user

class AnonymousUserRecordProvider : UserRecordProvider {
    override fun invoke() = UserRecord("anonymous", "unknown")
}
