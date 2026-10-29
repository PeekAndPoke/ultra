package io.peekandpoke.kraft.coretests.routing

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.kraft.routing.Static
import io.peekandpoke.kraft.routing.router
import io.peekandpoke.kraft.testing.TestBed
import io.peekandpoke.kraft.testing.awaitUri
import kotlinx.browser.window
import kotlinx.coroutines.delay
import kotlinx.html.div

class RouterReplaceUriSpec : StringSpec({

    "hash strategy: replaceUri writes the fragment in place, and the router follows once" {
        TestBed.preact(
            appSetup = { routing { mount(Static("/a")) {} } },
            view = { div { } },
        ) {
            router.navToUri("/a?x=1")
            router.awaitUri("/a?x=1")

            val path = window.location.pathname
            val entries = window.history.length
            var emissions = 0
            val unsubscribe = router.current.subscribeToStream { emissions++ }
            emissions = 0

            router.replaceUri("/a?x=2")
            // A hashchange would arrive as a task of its own: give it the chance to emit a second time
            delay(50)
            unsubscribe()

            window.location.hash shouldBe "#/a?x=2"
            window.location.pathname shouldBe path
            window.history.length shouldBe entries
            router.current().uri shouldBe "/a?x=2"
            emissions shouldBe 1
        }

        window.history.replaceState(null, "", window.location.pathname + window.location.search)
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

        window.history.replaceState(null, "", window.location.pathname + window.location.search)
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

            val entries = window.history.length
            var emissions = 0
            val unsubscribe = pathRouter.current.subscribeToStream { emissions++ }
            emissions = 0

            pathRouter.replaceUri("/app/list?page=2")
            unsubscribe()

            window.location.pathname + window.location.search shouldBe "/app/list?page=2"
            window.history.length shouldBe entries
            pathRouter.current().uri shouldBe "/app/list?page=2"
            emissions shouldBe 1
        } finally {
            pathRouter.dispose()
            window.history.replaceState(null, "", originalHref)
        }
    }
})
