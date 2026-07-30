package io.peekandpoke.funktor.insights.gui

import io.ktor.http.*
import io.ktor.server.html.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.peekandpoke.funktor.core.broker.TypedRouteRenderer
import io.peekandpoke.funktor.core.broker.get
import io.peekandpoke.funktor.core.config.AppConfig
import io.peekandpoke.funktor.core.kontainer
import io.peekandpoke.funktor.core.model.AppInfo
import io.peekandpoke.funktor.insights.InsightsDataLoader

class InsightsGui(
    private val routes: InsightsGuiRoutes,
    private val insights: InsightsDataLoader,
) {

    fun Route.mount() {

        get(routes.bar) { file ->

            val guiData = insights.loadGuiData(file.path)

            if (guiData != null) {
                call.respondBytes(ContentType.Text.Html, HttpStatusCode.OK) {
                    InsightsBarTemplate(
                        data = file,
                        routes = routes,
                        guiData = guiData,
                        routeRenderer = kontainer.get(TypedRouteRenderer::class),
                        appConfig = kontainer.get(AppConfig::class),
                        appInfo = kontainer.get(AppInfo::class),
                    ).render().toString().toByteArray()
                }
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }

        get(routes.details) { file ->

            val guiData = insights.loadGuiData(file.path)

            if (guiData != null) {
                val template = kontainer.get(InsightsGuiTemplate::class)

                call.respondHtmlTemplate(template, HttpStatusCode.OK) {
                    render(guiData)
                }
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }
    }
}
