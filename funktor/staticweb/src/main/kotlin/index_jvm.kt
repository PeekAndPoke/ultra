package io.peekandpoke.funktor.staticweb

import io.peekandpoke.funktor.staticweb.flashsession.funktorFlashSession
import io.peekandpoke.funktor.staticweb.resources.funktorResources
import io.peekandpoke.funktor.staticweb.templating.funktorTemplating
import io.peekandpoke.ultra.kontainer.KontainerBuilder
import io.peekandpoke.ultra.kontainer.module

fun KontainerBuilder.funktorStaticWeb() = module(Funktor_StaticWeb)

/**
 * Templating kontainer module
 */
val Funktor_StaticWeb = module {
    funktorFlashSession()
    funktorTemplating()
    funktorResources()
}
