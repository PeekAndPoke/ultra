import de.peekandpoke.ultra.security.password.PBKDF2PasswordHasher

fun main() {

    val hasher = PBKDF2PasswordHasher()

    val password = "secret"

    val hashed = hasher.hash(password)

    println(password)
    println(hashed)

    println("===================================")

    println(hasher.check("secret", hashed))

}

