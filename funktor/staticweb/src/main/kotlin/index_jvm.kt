package de.peekandpoke.funktor.staticweb

import de.peekandpoke.funktor.staticweb.flashsession.funktorFlashSession
import de.peekandpoke.funktor.staticweb.resources.funktorResources
import de.peekandpoke.funktor.staticweb.templating.funktorTemplating
import de.peekandpoke.ultra.kontainer.KontainerBuilder
import de.peekandpoke.ultra.kontainer.module

fun KontainerBuilder.funktorStaticWeb() = module(Funktor_StaticWeb)

/**
 * Templating kontainer module
 */
val Funktor_StaticWeb = module {
    funktorFlashSession()
    funktorTemplating()
    funktorResources()
}
