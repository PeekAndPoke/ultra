package io.peekandpoke.kraft.coretests.routing

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.peekandpoke.kraft.routing.Router
import io.peekandpoke.kraft.routing.Static
import io.peekandpoke.kraft.routing.router
import io.peekandpoke.kraft.testing.TestBed
import io.peekandpoke.kraft.testing.awaitUri
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.delay
import kotlinx.html.div
import org.w3c.dom.HTMLAnchorElement
import org.w3c.dom.events.Event
import org.w3c.dom.events.MouseEvent
import kotlin.time.Duration

class RouterDisposeSpec : StringSpec({

    "the test reaches the app's own router, and awaitUri waits for a route" {
        TestBed.preact(
            appSetup = {
                routing {
                    mount(Static("/a")) {}
                    mount(Static("/b")) {}
                }
            },
            view = { div { } },
        ) {
            router.navToUri("/b")
            router.awaitUri("/b")

            router.current().uri shouldBe "/b"
        }

        window.history.replaceState(null, "", window.location.pathname + window.location.search)
    }

    "awaitUri fails with where the router is" {
        TestBed.preact(
            appSetup = { routing { mount(Static("/a")) {} } },
            view = { div { } },
        ) {
            router.navToUri("/a")
            router.awaitUri("/a")

            val error = shouldThrow<AssertionError> { router.awaitUri("/x", timeout = Duration.ZERO) }

            error.message shouldContain "router at '/a', expected '/x'"
        }

        window.history.replaceState(null, "", window.location.pathname + window.location.search)
    }

    "after a TestBed render the app's router no longer answers hashchange" {
        lateinit var appRouter: Router

        TestBed.preact(
            appSetup = {
                routing {
                    mount(Static("/a")) {}
                    mount(Static("/b")) {}
                }
            },
            view = { div { } },
        ) {
            appRouter = router
            router.navToUri("/a")
            router.awaitUri("/a")
        }

        window.location.hash = "/b"
        // hashchange is dispatched as a task of its own
        delay(50)

        appRouter.current().uri shouldBe "/a"

        window.history.replaceState(null, "", window.location.pathname + window.location.search)
    }

    "Router.dispose() removes the path strategy's popstate and click listeners" {
        val originalHref = window.location.href

        val pathRouter = router {
            usePathStrategy()
            mount(Static("/app/one")) {}
            mount(Static("/app/two")) {}
            catchAll {}
        }

        // Listening before dispose: a popstate to another route is followed
        window.history.pushState(null, "", "/app/one")
        window.dispatchEvent(Event("popstate"))
        pathRouter.current().uri shouldBe "/app/one"

        pathRouter.dispose()

        window.history.pushState(null, "", "/app/two")
        window.dispatchEvent(Event("popstate"))
        pathRouter.current().uri shouldBe "/app/one"

        // A plain click on a relative link is no longer intercepted
        val anchor = (document.createElement("a") as HTMLAnchorElement).apply { href = "/app/two" }
        document.body!!.appendChild(anchor)

        var prevented: Boolean? = null
        val recorder: (Event) -> Unit = { event ->
            prevented = event.defaultPrevented
            event.preventDefault()
        }
        window.addEventListener("click", recorder)

        val options: dynamic = js("{}")
        options.bubbles = true
        options.cancelable = true
        options.button = 0
        anchor.dispatchEvent(MouseEvent("click", options))

        window.removeEventListener("click", recorder)
        anchor.remove()
        window.history.replaceState(null, "", originalHref)

        prevented shouldBe false
    }
})
