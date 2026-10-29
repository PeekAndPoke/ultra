package io.peekandpoke.kraft.coretests.routing

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.kraft.components.NoProps
import io.peekandpoke.kraft.components.PureComponent
import io.peekandpoke.kraft.components.comp
import io.peekandpoke.kraft.routing.BackNavigationTrap
import io.peekandpoke.kraft.routing.BackNavigationTrap.Companion.trapBackNavigation
import io.peekandpoke.kraft.routing.RouterComponent
import io.peekandpoke.kraft.routing.Static
import io.peekandpoke.kraft.testing.TestBed
import io.peekandpoke.kraft.testing.awaitUri
import io.peekandpoke.kraft.testing.eventually
import io.peekandpoke.kraft.vdom.VDom
import io.peekandpoke.ultra.streams.StreamSource
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.delay
import kotlinx.html.Tag
import kotlinx.html.div

@Suppress("TestFunctionName")
private fun Tag.Trapped() = comp { Trapped(it) }

/** Stands for a FadingModal: a Back trap while it is mounted. */
private class Trapped(ctx: NoProps) : PureComponent(ctx) {
    @Suppress("unused")
    private val trap = trapBackNavigation { BackNavigationTrap.TrapResult.Stop }

    override fun VDom.render() {
        div(classes = "trapped") { }
    }
}

@Suppress("TestFunctionName")
private fun Tag.Page(open: StreamSource<Boolean>) = comp { Page(it, open) }

private class Page(ctx: NoProps, open: StreamSource<Boolean>) : PureComponent(ctx) {
    private val isOpen by subscribingTo(open)

    override fun VDom.render() {
        div(classes = "page") {
            if (isOpen) Trapped()
        }
    }
}

private enum class TrapStrategy { Hash, Path }

/**
 * The Back trap (ULTRA-00266 review, round 2): closing it goes back over its own entry only while that entry still has
 * the address the trap pushed. A replaceUri while the trap was up (a urlParam inside a modal) must survive the close.
 */
class BackNavigationTrapUrlSpec : StringSpec({

    TrapStrategy.entries.forEach { strategy ->

        "$strategy: a replaceUri while the trap is up survives its close, and its marker is gone" {
            val originalHref = window.location.href
            val open = StreamSource(false)

            try {
                TestBed.preact(
                    appSetup = {
                        routing {
                            if (strategy == TrapStrategy.Path) usePathStrategy()
                            mount(Static("/list")) { Page(open) }
                        }
                    },
                    view = { RouterComponent() },
                ) {
                    router.navToUri("/list?f=1")
                    router.awaitUri("/list?f=1")
                    eventually { (document.querySelector(".page") != null) shouldBe true }

                    open(true)
                    eventually { (document.querySelector(".trapped") != null) shouldBe true }

                    router.replaceUri("/list?f=2")
                    router.awaitUri("/list?f=2")

                    open(false)
                    eventually { (document.querySelector(".trapped") == null) shouldBe true }
                    // The trap's back runs on a queue of its own, and a popstate / hashchange would follow it
                    delay(100)

                    router.current().uri shouldBe "/list?f=2"
                    (window.history.state == null) shouldBe true
                }
            } finally {
                window.history.replaceState(null, "", originalHref.substringBefore('#'))
            }
        }

        "$strategy: a trap closed with its address unchanged still goes back over its own entry" {
            val originalHref = window.location.href
            val open = StreamSource(false)

            try {
                TestBed.preact(
                    appSetup = {
                        routing {
                            if (strategy == TrapStrategy.Path) usePathStrategy()
                            mount(Static("/list")) { Page(open) }
                        }
                    },
                    view = { RouterComponent() },
                ) {
                    router.navToUri("/list?f=1")
                    router.awaitUri("/list?f=1")
                    window.history.replaceState("page-entry", "", window.location.href)

                    open(true)
                    eventually { (document.querySelector(".trapped") != null) shouldBe true }
                    (window.history.state == "page-entry") shouldBe false

                    open(false)
                    eventually { (window.history.state == "page-entry") shouldBe true }

                    router.current().uri shouldBe "/list?f=1"
                }
            } finally {
                window.history.replaceState(null, "", originalHref.substringBefore('#'))
            }
        }
    }
})
