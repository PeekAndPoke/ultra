package io.peekandpoke.funktor

import io.kotest.matchers.shouldNotBe
import io.peekandpoke.funktor.rest.JwtSigningKeyOnAppStarting

/**
 * Every app with a JWT logs at boot which key signs (ULTRA-00178): `funktorRest { jwt(...) }` registers the hook.
 * What the line says is pinned by `JwtBootValidationSpec`.
 */
class JwtSigningKeyBootLogSpec : FunktorApiSpec() {

    init {
        "jwt() registers the boot log in a real app" {
            apiApp {
                kontainer.getOrNull(JwtSigningKeyOnAppStarting::class) shouldNotBe null
            }
        }
    }
}
