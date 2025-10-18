package io.peekandpoke.funktor.demo.server

import de.peekandpoke.funktor.core.AppKontainers
import de.peekandpoke.funktor.core.funktorApp

val app = funktorApp<FunktorDemoConfig>(
    kontainers = { config ->
        AppKontainers(
            createBlueprint(config)
        )
    },
)

fun main(args: Array<String>) = app.run(args)
