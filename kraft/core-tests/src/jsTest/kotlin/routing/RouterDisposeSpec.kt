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
import io.peekandpoke.kraft.utils.ResponsiveController
import io.peekandpoke.kraft.utils.WindowController
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.delay
import kotlinx.html.div
import org.w3c.dom.HTMLAnchorElement
import org.w3c.dom.events.Event
import org.w3c.dom.events.MouseEvent
import kotlin.time.Duration

class RouterDisposeSpec : StringSpec({

    // A hash left by a row, also a failing one, must not reach the next spec
    afterTest { window.history.replaceState(null, "", window.location.pathname + window.location.search) }

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

        // Enabled again, only a removed listener stays silent: disable() alone would also pass the check below
        appRouter.enable()
        window.location.hash = "/b"
        // hashchange is dispatched as a task of its own
        delay(50)

        appRouter.current().uri shouldBe "/a"

    }

    "Router.dispose() removes the path strategy's popstate and click listeners" {
        val originalHref = window.location.href

        val pathRouter = router {
            usePathStrategy()
            mount(Static("/app/one")) {}
            mount(Static("/app/two")) {}
            catchAll {}
        }

        val anchor = (document.createElement("a") as HTMLAnchorElement).apply { href = "/app/two" }
        var prevented: Boolean? = null
        val recorder: (Event) -> Unit = { event ->
            prevented = event.defaultPrevented
            event.preventDefault()
        }

        try {
            // Listening before dispose: a popstate to another route is followed
            window.history.pushState(null, "", "/app/one")
            window.dispatchEvent(Event("popstate"))
            pathRouter.current().uri shouldBe "/app/one"

            pathRouter.dispose()
            // Enabled again, only a removed listener stays silent: disable() alone would also pass these checks
            pathRouter.enable()

            window.history.pushState(null, "", "/app/two")
            window.dispatchEvent(Event("popstate"))
            pathRouter.current().uri shouldBe "/app/one"

            // A plain click on a relative link is no longer intercepted
            document.body!!.appendChild(anchor)
            window.addEventListener("click", recorder)

            val options: dynamic = js("{}")
            options.bubbles = true
            options.cancelable = true
            options.button = 0
            anchor.dispatchEvent(MouseEvent("click", options))

            prevented shouldBe false
        } finally {
            window.removeEventListener("click", recorder)
            anchor.remove()
            pathRouter.dispose()
            window.history.replaceState(null, "", originalHref)
        }
    }

    "an app that sets its own ResponsiveController creates no default one (and no listener for it)" {
        val custom = ResponsiveController()
        // Created before counting: the shared WindowController adds a resize listener of its own once
        WindowController.default.hasFocus()

        val win = window.asDynamic()
        val add = win.addEventListener
        var resizeListeners = 0
        win.addEventListener = { type: String, handler: dynamic, options: dynamic ->
            if (type == "resize") resizeListeners++
            add.call(window, type, handler, options)
        }

        try {
            TestBed.preact(appSetup = { responsive(custom) }, view = { div { } }) {
                (app.appAttributes[ResponsiveController.key] === custom) shouldBe true
            }
        } finally {
            win.addEventListener = add
            custom.dispose()
        }

        resizeListeners shouldBe 0
    }

    "KraftApp.dispose() removes the ResponsiveController's resize listener" {
        lateinit var responsive: ResponsiveController

        TestBed.preact(view = { div { } }) {
            responsive = app.appAttributes[ResponsiveController.key]!!
        }

        var emissions = 0
        val unsubscribe = responsive.subscribeToStream { emissions++ }
        emissions = 0

        window.dispatchEvent(Event("resize"))
        unsubscribe()

        emissions shouldBe 0
    }
})
