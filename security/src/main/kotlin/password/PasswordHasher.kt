package de.peekandpoke.ultra.security.password


interface PasswordHasher {
    fun hash(password: String): String

    fun check(plaintext: String?, hash: String?): Boolean
}

