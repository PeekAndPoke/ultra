package io.peekandpoke.funktor.demo.server

import ch.qos.logback.classic.Level
import de.peekandpoke.funktor.cluster.workers.launchWorkers
import de.peekandpoke.funktor.core.lifecycle.lifeCycle
import de.peekandpoke.funktor.insights.gui.InsightsGui
import de.peekandpoke.funktor.logging.karango.addKarangoAppender
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sse.*
import io.ktor.server.webjars.*
import io.peekandpoke.funktor.demo.server.api.ApiApp
import kotlinx.serialization.json.Json

@Suppress("unused")
fun Application.module() = app.module(this) { app, config, init ->
    // Add log appender that writes to the database
    addKarangoAppender(config = config.arangodb, minLevel = Level.INFO)

    if (!config.ktor.isTest) {
        lifeCycle(init) {
            // TODO: start and stop the workers by using LifeCycleHooks as well
            launchWorkers { init.clone() }
        }
    }

    install(Webjars) {
        path = "vendor"
    }

    install(SSE)

    // Install CORS feature
    install(CORS) {
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Options)

        allowHeader(HttpHeaders.Accept)
        allowHeader(HttpHeaders.AcceptEncoding)
        allowHeader(HttpHeaders.AcceptLanguage)
        allowHeader(HttpHeaders.AccessControlRequestHeaders)
        allowHeader(HttpHeaders.AccessControlRequestMethod)
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.CacheControl)
        allowHeader("Last-Event-ID")

        allowCredentials = true

        // Dev adminapp:
        allowHost("localhost:36588", schemes = listOf("http", "https"))
        allowHost("127.0.0.1:36588", schemes = listOf("http", "https"))
        // Dev webapp:
        allowHost("localhost:36589", schemes = listOf("http", "https"))
        allowHost("127.0.0.1:36589", schemes = listOf("http", "https"))
        // Dev domains
        allowHost("admin.funktor-demo.localhost:36587", schemes = listOf("http", "https"))
        allowHost("www.funktor-demo.localhost:36587", schemes = listOf("http", "https"))
    }

    install(io.ktor.server.plugins.contentnegotiation.ContentNegotiation) {
        json(
            Json {
                prettyPrint = true
                isLenient = true
            }
        )
    }

    routing {
        ////  API  /////////////////////////////////////////////////////////////////////////////////////////////////////

        host("api.*".toRegex()) {
            // Install Kontainer into ApplicationCall
            installApiKontainer(app, config.api.insights)
            // Mount the app
            init.get(ApiApp::class).apply { mountApiAppModule() }
        }

        //// ADMIN /////////////////////////////////////////////////////////////////////////////////////////////////////

        host("admin.*".toRegex()) {
            // Install Kontainer into ApplicationCall
            installWwwKontainer(app, config.api.insights)

            staticResources(remotePath = "assets", basePackage = "assets")

            // mount the insights gui when present
            init.use(InsightsGui::class) {
                mount()
            }

            get("/") {
                call.respondText("Hello, world!")
            }

            get("/ping") {
                call.respondText("pong")
            }
        }
    }
}
