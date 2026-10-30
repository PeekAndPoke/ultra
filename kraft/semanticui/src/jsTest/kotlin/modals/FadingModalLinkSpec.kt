package io.peekandpoke.kraft.semanticui.modals

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.kraft.modals.DefaultModalsManager
import io.peekandpoke.kraft.modals.ModalsManager
import io.peekandpoke.kraft.routing.Static
import io.peekandpoke.kraft.testing.TestBed
import io.peekandpoke.kraft.testing.awaitCss
import io.peekandpoke.kraft.testing.awaitUri
import kotlinx.browser.window
import kotlinx.html.a
import kotlinx.html.div
import org.w3c.dom.events.Event
import org.w3c.dom.events.MouseEvent

/** A FadingModal and the router (ULTRA-00277): a route link inside a modal goes through the router. */
class FadingModalLinkSpec : StringSpec({

    // A hash left by a row must not reach the next spec
    afterTest { window.history.replaceState(null, "", window.location.pathname + window.location.search) }

    "a route link inside a modal is taken by the router; other clicks inside still stop at the modal" {
        val m = DefaultModalsManager()

        TestBed.preact(
            appSetup = {
                setAttribute(ModalsManager.key, m)
                routing {
                    mount(Static("/a")) {}
                    mount(Static("/b")) {}
                }
            },
            view = { },
        ) { root ->
            router.navToUri("/a")
            router.awaitUri("/a")

            m.show { h ->
                OkCancelModal(
                    handle = h,
                    transition = FadingModal.Transition(fadeInTimeMs = 0, fadeOutTimeMs = 0),
                    view = OkCancelModal.View(
                        appearance = { mini },
                        header = { +"Header" },
                        content = {
                            a(href = "#/b", classes = "go") { +"go" }
                            div(classes = "plain") { +"plain" }
                        },
                        okText = { +"Yes" },
                        cancelText = { +"No" },
                    ),
                    onResult = {},
                )
            }

            var windowClicks = 0
            val counter: (Event) -> Unit = { windowClicks++ }
            window.addEventListener("click", counter)

            try {
                val plain = root.awaitCss(".plain")[0]
                plain.dispatchEvent(MouseEvent("click", js("({bubbles: true, cancelable: true, button: 0})")))
                windowClicks shouldBe 0

                val link = root.awaitCss(".go")[0]
                val click = MouseEvent("click", js("({bubbles: true, cancelable: true, button: 0})"))
                link.dispatchEvent(click)

                // The router's window listener took it (prevented the browser's own follow) and navigated
                click.defaultPrevented shouldBe true
                router.awaitUri("/b")
            } finally {
                window.removeEventListener("click", counter)
            }
        }
    }
})
