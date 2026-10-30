package io.peekandpoke.kraft.semanticui.modals

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.kraft.modals.DefaultModalsManager
import io.peekandpoke.kraft.modals.ModalsManager
import io.peekandpoke.kraft.routing.Static
import io.peekandpoke.kraft.testing.TestBed
import io.peekandpoke.kraft.testing.awaitCss
import io.peekandpoke.kraft.testing.awaitUri
import io.peekandpoke.kraft.testing.eventually
import kotlinx.browser.window
import kotlinx.coroutines.delay
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

    // Path routing: its navigation fires no popstate, which under hash routing closes the modal through its back trap
    "path routing: a navigation to another page dismisses an open modal; a query-only move does not" {
        val m = DefaultModalsManager()
        val answers = mutableListOf<OkCancelModal.Result?>()
        val originalHref = window.location.href

        try {
            TestBed.preact(
                appSetup = {
                    setAttribute(ModalsManager.key, m)
                    routing {
                        usePathStrategy()
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
                            content = { +"content" },
                            okText = { +"Yes" },
                            cancelText = { +"No" },
                        ),
                        onResult = { answers += it },
                    )
                }
                root.awaitCss(".dimmer")

                router.replaceUri("/a?tab=2")
                router.awaitUri("/a?tab=2")
                delay(50)
                m().size shouldBe 1

                router.navToUri("/b")
                router.awaitUri("/b")
                eventually { m().size shouldBe 0 }
                answers shouldBe listOf(null)
            }
        } finally {
            window.history.replaceState(null, "", originalHref)
        }
    }

    "hash routing: a modal shown together with a navigation to another page stays open on that page" {
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

            // One handler: go to /b and open a modal there ("Saved", "Session expired")
            router.navToUri("/b")
            m.show { h ->
                OkCancelModal(
                    handle = h,
                    transition = FadingModal.Transition(fadeInTimeMs = 0, fadeOutTimeMs = 0),
                    view = OkCancelModal.View(
                        appearance = { mini },
                        header = { +"Header" },
                        content = { +"content" },
                        okText = { +"Yes" },
                        cancelText = { +"No" },
                    ),
                    onResult = {},
                )
            }
            router.awaitUri("/b")
            delay(50)

            m().size shouldBe 1
        }
    }

    "path routing: a route link inside a modal goes through the router (no page load), and the modal closes" {
        val m = DefaultModalsManager()
        val originalHref = window.location.href

        try {
            TestBed.preact(
                appSetup = {
                    setAttribute(ModalsManager.key, m)
                    routing {
                        usePathStrategy()
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
                            content = { a(href = "/b", classes = "go") { +"go" } },
                            okText = { +"Yes" },
                            cancelText = { +"No" },
                        ),
                        onResult = {},
                    )
                }

                val click = MouseEvent("click", js("({bubbles: true, cancelable: true, button: 0})"))
                root.awaitCss(".go")[0].dispatchEvent(click)

                click.defaultPrevented shouldBe true
                router.awaitUri("/b")
                eventually { m().size shouldBe 0 }
            }
        } finally {
            window.history.replaceState(null, "", originalHref)
        }
    }
})
