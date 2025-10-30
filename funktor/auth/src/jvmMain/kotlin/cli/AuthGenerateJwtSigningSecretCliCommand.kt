package de.peekandpoke.funktor.auth.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import de.peekandpoke.funktor.auth.AuthRandom

class AuthGenerateJwtSigningSecretCliCommand(
    random: Lazy<AuthRandom>,
) : CliktCommand(name = "app:auth:generate-jwt-signing-secret") {

    private val random by random

    private val length by option(help = "Length of the generated secret").int().default(128)

    override fun help(context: Context): String {
        return "Generate a new JWT Signing Secret"
    }

    override fun run() {

        val key = random.getTokenAsBase64(length)

        println(key)
    }
}
