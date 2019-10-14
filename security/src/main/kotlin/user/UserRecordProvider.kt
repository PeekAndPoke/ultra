package de.peekandpoke.ultra.security.user

interface UserRecordProvider {
    operator fun invoke(): UserRecord
}
