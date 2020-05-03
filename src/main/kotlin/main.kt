import de.peekandpoke.ultra.security.jwt.SimpleJwtGenerator
import de.peekandpoke.ultra.security.jwt.expiresIn
import io.jsonwebtoken.SignatureAlgorithm
import kotlin.time.ExperimentalTime
import kotlin.time.days

@ExperimentalTime
fun main() {

//    println(Keys.secretKeyFor(SignatureAlgorithm.HS512).encoded.toBase64())

    val signingKey = "ELoNVHHdwkZmRWUnumXmDqbXfQJTcNmJJk6a8K2BLRIPlakSg9vtYCXkz8z+vmCecgQxHODHhP2PJB7Epa745g=="

    val gen = SimpleJwtGenerator(
        signingAlgorithm = SignatureAlgorithm.HS512,
        signingKey = signingKey
    )

    val token = gen.createToken {
        setIssuer("https://www.jointhebase.co")
        setAudience("api.jointhebase.co")
        expiresIn(1.days)
        claim("name", "Karsten J. Gerber")
        claim("email", "karsten@jointhebase.co")
        claim(
            "https://api.jointhebase.co/auth",
            mapOf(
                "roles" to listOf("SUPER_USER", "CMS")
            )
        )
    }

    println(token)

    val parsed = gen.parseToken(token) {
        requireAudience("api.jointhebase.co")
        requireIssuer("https://www.jointhebase.co")
    }

    println("PARSED")
    println(parsed)

    println("PARSED.BODY")
    println(parsed.body)

    println("PARSED.BODY['name']")
    println(parsed.body["name"])
}

