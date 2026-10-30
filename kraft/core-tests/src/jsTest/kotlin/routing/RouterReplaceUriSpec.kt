package io.peekandpoke.kraft.coretests.routing

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.kraft.components.NoProps
import io.peekandpoke.kraft.components.PureComponent
import io.peekandpoke.kraft.components.comp
import io.peekandpoke.kraft.routing.RouterComponent
import io.peekandpoke.kraft.routing.Static
import io.peekandpoke.kraft.routing.router
import io.peekandpoke.kraft.routing.urlParam
import io.peekandpoke.kraft.testing.TestBed
import io.peekandpoke.kraft.testing.awaitUri
import io.peekandpoke.kraft.testing.eventually
import io.peekandpoke.kraft.vdom.VDom
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.delay
import kotlinx.html.Tag
import kotlinx.html.div

/** The page of the urlParam row, reachable from the test. */
private var paramPage: ParamPage? = null

@Suppress("TestFunctionName")
private fun Tag.ParamPage() = comp { ParamPage(it) }

private class ParamPage(ctx: NoProps) : PureComponent(ctx) {
    var x by urlParam("x", 1)

    init {
        lifecycle {
            onMount { paramPage = this@ParamPage }
            onUnmount { paramPage = null }
        }
    }

    override fun VDom.render() {
        div(classes = "param") { +"$x" }
    }
}

class RouterReplaceUriSpec : StringSpec({

    // A hash left by a row, also a failing one, must not reach the next spec
    afterTest { window.history.replaceState(null, "", window.location.pathname + window.location.search) }

    "hash strategy: replaceUri writes the fragment in place, and the router follows once" {
        TestBed.preact(
            appSetup = { routing { mount(Static("/a")) {} } },
            view = { div { } },
        ) {
            router.navToUri("/a?x=1")
            router.awaitUri("/a?x=1")

            val path = window.location.pathname
            // Marked: still marked afterwards means the entry was replaced, not a new one pushed (and its state kept)
            window.history.replaceState("marked", "", window.location.href)
            var emissions = 0
            val unsubscribe = router.current.subscribeToStream { emissions++ }
            emissions = 0

            router.replaceUri("/a?x=2")
            // A hashchange would arrive as a task of its own: give it the chance to emit a second time
            delay(50)
            unsubscribe()

            window.location.hash shouldBe "#/a?x=2"
            window.location.pathname shouldBe path
            window.history.state shouldBe "marked"
            router.current().uri shouldBe "/a?x=2"
            emissions shouldBe 1
        }
    }

    "hash strategy: a replaceUri given with the '#' is the same address" {
        TestBed.preact(
            appSetup = { routing { mount(Static("/a")) {} } },
            view = { div { } },
        ) {
            router.navToUri("/a?x=1")
            router.awaitUri("/a?x=1")

            var emissions = 0
            val unsubscribe = router.current.subscribeToStream { emissions++ }
            emissions = 0

            router.replaceUri("#/a?x=1")
            delay(50)
            unsubscribe()

            emissions shouldBe 0
        }
    }

    "hash strategy: a urlParam change writes the fragment, and another subscriber of the router sees it" {
        TestBed.preact(
            appSetup = { routing { mount(Static("/a")) { ParamPage() } } },
            view = { RouterComponent() },
        ) {
            router.navToUri("/a?x=1")
            router.awaitUri("/a?x=1")
            val page = eventually { paramPage ?: error("not mounted") }

            val seen = mutableListOf<String>()
            val unsubscribe = router.current.subscribeToStream { seen.add(it.uri) }
            seen.clear()

            page.x = 2
            delay(50)
            unsubscribe()

            window.location.hash shouldBe "#/a?x=2"
            router.current().uri shouldBe "/a?x=2"
            seen shouldBe listOf("/a?x=2")
            eventually { document.querySelector(".param")?.textContent shouldBe "2" }
        }
    }

    "path strategy: replaceUri replaces the entry and the router emits once" {
        val originalHref = window.location.href

        val pathRouter = router {
            usePathStrategy()
            mount(Static("/app/list")) {}
            catchAll {}
        }

        try {
            pathRouter.navToUri("/app/list?page=1")
            pathRouter.current().uri shouldBe "/app/list?page=1"

            window.history.replaceState("marked", "", window.location.href)
            var emissions = 0
            val unsubscribe = pathRouter.current.subscribeToStream { emissions++ }
            emissions = 0

            pathRouter.replaceUri("/app/list?page=2")
            unsubscribe()

            window.location.pathname + window.location.search shouldBe "/app/list?page=2"
            window.history.state shouldBe "marked"
            pathRouter.current().uri shouldBe "/app/list?page=2"
            emissions shouldBe 1
        } finally {
            pathRouter.dispose()
            window.history.replaceState(null, "", originalHref)
        }
    }
})
