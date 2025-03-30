package de.peekandpoke.ktorfx.staticweb

import de.peekandpoke.ktorfx.staticweb.flashsession.ktorFxFlashSession
import de.peekandpoke.ktorfx.staticweb.resources.ktorFxResources
import de.peekandpoke.ktorfx.staticweb.templating.ktorFxTemplating
import de.peekandpoke.ultra.kontainer.KontainerBuilder
import de.peekandpoke.ultra.kontainer.module

fun KontainerBuilder.ktorFxStaticWeb() = module(KtorFX_StaticWeb)

/**
 * Templating kontainer module
 */
val KtorFX_StaticWeb = module {
    ktorFxFlashSession()
    ktorFxTemplating()
    ktorFxResources()
}
