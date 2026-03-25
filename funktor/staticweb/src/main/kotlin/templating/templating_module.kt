package io.peekandpoke.funktor.staticweb.templating

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.html.*
import io.ktor.server.routing.*
import io.peekandpoke.funktor.core.kontainer
import io.peekandpoke.ultra.kontainer.KontainerBuilder
import io.peekandpoke.ultra.kontainer.module
import kotlinx.html.HTML
import kotlin.reflect.KClass

internal fun KontainerBuilder.funktorTemplating() = module(Funktor_Templating)

/**
 * Templating kontainer module
 */
internal val Funktor_Templating = module {
    /**
     * Override this one with an application specific implementation of [TemplateTools]
     */
    prototype(TemplateTools::class, TemplateToolsImpl::class)
}

@JvmName("respond_specific")
suspend inline fun <reified T : SimpleTemplate> RoutingContext.respond(
    status: HttpStatusCode = HttpStatusCode.OK,
    noinline body: T.() -> Unit,
) {
    respond(status, T::class, body)
}

/**
 * Responds with the given template [T]
 */
suspend fun <T : SimpleTemplate> RoutingContext.respond(
    status: HttpStatusCode = HttpStatusCode.OK,
    template: KClass<T>,
    body: T.() -> Unit,
) {
    call.respondHtmlTemplate(kontainer.get(template), status, body)
}

/**
 * Responds to a client with an HTML response built based on a specified template.
 * You can learn more from [HTML DSL](https://ktor.io/docs/html-dsl.html).
 */
private suspend fun <TTemplate : Template<HTML>> ApplicationCall.respondTemplate(
    template: TTemplate,
    status: HttpStatusCode = HttpStatusCode.OK,
    body: suspend TTemplate.() -> Unit,
) {
    template.body()
    respondHtml(status) { with(template) { apply() } }
}
