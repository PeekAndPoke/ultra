package io.peekandpoke.kraft.semanticui.menu

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.kraft.routing.Static
import io.peekandpoke.kraft.testing.TestBed
import io.peekandpoke.kraft.testing.awaitCss
import io.peekandpoke.kraft.testing.awaitUri
import io.peekandpoke.kraft.testing.eventually
import io.peekandpoke.kraft.testing.selectCss
import kotlinx.browser.window
import kotlinx.html.a
import kotlinx.html.div
import org.w3c.dom.events.MouseEvent

/** A DropdownMenu and the router (ULTRA-00277): a route link in the menu goes through the router. */
class DropdownMenuLinkSpec : StringSpec({

    // A hash left by a row must not reach the next spec
    afterTest { window.history.replaceState(null, "", window.location.pathname + window.location.search) }

    "a route link in a dropdown menu is taken by the router" {
        TestBed.preact(
            appSetup = {
                routing {
                    mount(Static("/a")) {}
                    mount(Static("/b")) {}
                }
            },
            view = {
                // Inside a Fomantic menu, as a dropdown usually is: its own `.menu` is the one that counts
                div(classes = "ui menu") {
                    DropdownMenu {
                        text {
                            item { render { a(href = "#/b", classes = "go") { +"go" } } }
                        }
                    }
                }
            },
        ) { root ->
            router.navToUri("/a")
            router.awaitUri("/a")

            // Opened by a click on the trigger, as a user does
            root.awaitCss(".dropdown.item")[0].dispatchEvent(MouseEvent("click", js("({bubbles: true, cancelable: true})")))
            eventually { root.selectCss(".menu.visible").size shouldBe 1 }

            val click = MouseEvent("click", js("({bubbles: true, cancelable: true, button: 0})"))
            root.awaitCss(".go")[0].dispatchEvent(click)

            // The router's window listener took it (prevented the browser's own follow) and navigated
            click.defaultPrevented shouldBe true
            router.awaitUri("/b")

            // And the menu closed, as for any choice in it: the trigger around it must not open it again
            eventually { root.selectCss(".menu.visible").size shouldBe 0 }
        }
    }
})
