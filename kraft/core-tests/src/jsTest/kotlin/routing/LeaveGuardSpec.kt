package io.peekandpoke.kraft.coretests.routing

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.kraft.components.Component
import io.peekandpoke.kraft.components.Ctx
import io.peekandpoke.kraft.components.comp
import io.peekandpoke.kraft.routing.LeaveResult
import io.peekandpoke.kraft.routing.Route1
import io.peekandpoke.kraft.routing.RouterComponent
import io.peekandpoke.kraft.routing.Static
import io.peekandpoke.kraft.routing.guardLeaving
import io.peekandpoke.kraft.routing.routerMiddleware
import io.peekandpoke.kraft.testing.TestBed
import io.peekandpoke.kraft.testing.TestBedScope
import io.peekandpoke.kraft.testing.awaitUri
import io.peekandpoke.kraft.testing.eventually
import io.peekandpoke.kraft.vdom.VDom
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.html.Tag
import kotlinx.html.div
import org.w3c.dom.HTMLAnchorElement
import org.w3c.dom.events.Event
import org.w3c.dom.events.MouseEvent

/** What the editor's guard answers, and every target it was asked about. */
private class GuardControl {
    var answer: () -> LeaveResult = { LeaveResult.Proceed }
    val asked = mutableListOf<String?>()
}

@Suppress("TestFunctionName")
private fun Tag.Editor(control: GuardControl) = comp(Editor.Props(control)) { Editor(it) }

private class Editor(ctx: Ctx<Props>) : Component<Editor.Props>(ctx) {
    data class Props(val control: GuardControl)

    init {
        guardLeaving(beforeUnload = true) { target ->
            props.control.asked.add(target)
            props.control.answer()
        }
    }

    override fun VDom.render() {
        div(classes = "editor") { +"editing" }
    }
}

private enum class Strategy { Hash, Path }

/**
 * An app on [strategy] with "/other", "/a", "/edit/{id}" (the guarded editor), "/admin" (its middleware redirects to
 * "/login") and "/login". The test starts on "/edit/1", reached through "/other" and "/a".
 */
private suspend fun withEditor(
    control: GuardControl,
    strategy: Strategy = Strategy.Hash,
    test: suspend TestBedScope.() -> Unit,
) {
    val originalHref = window.location.href

    try {
        TestBed.preact(
            appSetup = {
                routing {
                    if (strategy == Strategy.Path) usePathStrategy()

                    mount(Static("/other")) { div(classes = "other") { } }
                    mount(Static("/a")) { div(classes = "a") { } }
                    mount(Route1("/edit/{id}")) { Editor(control) }
                    middleware(routerMiddleware { it.redirect("/login") }) {
                        mount(Static("/admin")) { div(classes = "admin") { } }
                    }
                    mount(Static("/login")) { div(classes = "login") { } }
                }
            },
            view = { RouterComponent() },
        ) {
            router.navToUri("/other")
            router.awaitUri("/other")
            router.navToUri("/a")
            router.awaitUri("/a")
            router.navToUri("/edit/1")
            router.awaitUri("/edit/1")
            // The guard registers when the editor mounts, a render after the route changed
            eventually { (document.querySelector(".editor") != null) shouldBe true }
            control.asked.clear()

            test()
        }
    } finally {
        window.history.replaceState(null, "", originalHref.substringBefore('#'))
    }
}

/** Lets a hashchange, a popstate and a re-render arrive. */
private suspend fun settle() = delay(60)

/** Marks the current history entry: still marked after an action means no entry was pushed. */
private fun markEntry() = window.history.replaceState("marked", "", window.location.href)

/** A plain primary click on a link to [href], as a user's: true when the router took it (prevented the default). */
private fun clickLink(href: String): Boolean {
    val anchor = (document.createElement("a") as HTMLAnchorElement).apply { setAttribute("href", href) }
    document.body!!.appendChild(anchor)

    val options: dynamic = js("{}")
    options.bubbles = true
    options.cancelable = true
    options.button = 0

    var taken = false

    // Records whether the router took the click, then stops the page from really following the link
    val stopper: (Event) -> Unit = {
        taken = it.defaultPrevented
        it.preventDefault()
    }

    // Registered after the router's own listener, so it sees what the router did
    window.addEventListener("click", stopper)
    anchor.dispatchEvent(MouseEvent("click", options))

    window.removeEventListener("click", stopper)
    anchor.remove()

    return taken
}

/** `guardLeaving` (ULTRA-00266, from Klang): a page holds a navigation away from it. */
class LeaveGuardSpec : StringSpec({

    Strategy.entries.forEach { strategy ->

        "$strategy: Stop holds navToUri; the route, the address and the history entry stay" {
            val control = GuardControl().apply { answer = { LeaveResult.Stop } }

            withEditor(control, strategy) {
                markEntry()
                router.navToUri("/other")
                settle()

                router.current().uri shouldBe "/edit/1"
                window.history.state shouldBe "marked"
                control.asked shouldBe listOf("/other")
            }
        }

        "$strategy: Stop holds a link click before the browser writes it" {
            val control = GuardControl().apply { answer = { LeaveResult.Stop } }

            withEditor(control, strategy) {
                markEntry()
                clickLink(if (strategy == Strategy.Hash) "#/other" else "/other") shouldBe true
                settle()

                router.current().uri shouldBe "/edit/1"
                window.history.state shouldBe "marked"
                control.asked shouldBe listOf("/other")
            }
        }

        "$strategy: Stop holds a Back, and the page's address is put back" {
            val control = GuardControl().apply { answer = { LeaveResult.Stop } }

            withEditor(control, strategy) {
                window.history.back()
                eventually { control.asked shouldBe listOf("/a") }
                settle()

                router.current().uri shouldBe "/edit/1"
                if (strategy == Strategy.Hash) {
                    window.location.hash shouldBe "#/edit/1"
                } else {
                    window.location.pathname shouldBe "/edit/1"
                }
            }
        }

        "$strategy: Ask answered yes on a Back replays the Back: the next Back goes on to the page before" {
            val control = GuardControl().apply { answer = { LeaveResult.Ask { true } } }

            withEditor(control, strategy) {
                window.history.back()
                router.awaitUri("/a")
                control.asked shouldBe listOf("/a")

                // Had the yes pushed "/a" anew, this Back would land on the editor again
                window.history.back()
                router.awaitUri("/other")
            }
        }

        "$strategy: Ask answered yes onto a redirecting route lands on the redirect, asked once" {
            val control = GuardControl().apply { answer = { LeaveResult.Ask { true } } }

            withEditor(control, strategy) {
                router.navToUri("/admin")
                router.awaitUri("/login")
                settle()

                control.asked shouldBe listOf("/admin")
            }
        }

        "$strategy: withoutLeaveGuards passes a page that says Stop" {
            val control = GuardControl().apply { answer = { LeaveResult.Stop } }

            withEditor(control, strategy) {
                router.withoutLeaveGuards { router.navToUri("/login") }
                router.awaitUri("/login")
                settle()

                control.asked shouldBe emptyList()
            }
        }

        "$strategy: a navBack held by Stop keeps the router's history; a second one does not throw" {
            val control = GuardControl().apply { answer = { LeaveResult.Stop } }

            withEditor(control, strategy) {
                val before = router.history().entries.size

                router.history().navBack()
                settle()
                router.history().navBack()
                settle()

                router.current().uri shouldBe "/edit/1"
                router.history().entries.size shouldBe before
                control.asked shouldBe listOf("/a", "/a")
            }
        }

        "$strategy: Proceed lets it through, asked once with the target" {
            val control = GuardControl()

            withEditor(control, strategy) {
                router.navToUri("/other")
                router.awaitUri("/other")
                settle()

                control.asked shouldBe listOf("/other")
            }
        }

        "$strategy: Ask: nothing moves while asking; yes goes on to the target" {
            val control = GuardControl()
            val answer = CompletableDeferred<Boolean>()
            control.answer = { LeaveResult.Ask { answer.await() } }

            withEditor(control, strategy) {
                router.navToUri("/other")
                settle()
                router.current().uri shouldBe "/edit/1"

                // Further navigations while asking are dropped, and not asked
                router.navToUri("/a")
                settle()
                control.asked shouldBe listOf("/other")

                answer.complete(true)
                router.awaitUri("/other")
                settle()

                control.asked shouldBe listOf("/other")
            }
        }

        "$strategy: Ask: no stays" {
            val control = GuardControl().apply { answer = { LeaveResult.Ask { false } } }

            withEditor(control, strategy) {
                markEntry()
                router.navToUri("/other")
                settle()

                router.current().uri shouldBe "/edit/1"
                window.history.state shouldBe "marked"
            }
        }

        "$strategy: an Ask answered after a navigation past the guards is dropped" {
            val control = GuardControl()
            val answer = CompletableDeferred<Boolean>()
            control.answer = { LeaveResult.Ask { answer.await() } }

            withEditor(control, strategy) {
                router.navToUri("/other")
                settle()

                // A logout while the modal is open
                router.withoutLeaveGuards { router.navToUri("/login") }
                router.awaitUri("/login")

                answer.complete(true)
                settle()

                router.current().uri shouldBe "/login"
            }
        }

        "$strategy: Stop holds a leaving replaceUri and clearHistoryAndNavTo" {
            val control = GuardControl().apply { answer = { LeaveResult.Stop } }

            withEditor(control, strategy) {
                val entries = router.history().entries.size

                router.replaceUri("/other")
                settle()
                router.current().uri shouldBe "/edit/1"

                router.clearHistoryAndNavTo("/a")
                settle()
                router.current().uri shouldBe "/edit/1"
                router.history().entries.size shouldBe entries

                control.asked shouldBe listOf("/other", "/a")
            }
        }

        "$strategy: a guard is bound to its page: after leaving it, nothing asks" {
            val control = GuardControl()

            withEditor(control, strategy) {
                router.navToUri("/other")
                router.awaitUri("/other")
                control.asked.clear()

                router.navToUri("/a")
                router.awaitUri("/a")
                settle()

                control.asked shouldBe emptyList()
            }
        }

        "$strategy: the same params on another path (%31 for 1) is another page: the guard is asked" {
            val control = GuardControl().apply { answer = { LeaveResult.Stop } }

            withEditor(control, strategy) {
                clickLink(if (strategy == Strategy.Hash) "#/edit/%31" else "/edit/%31") shouldBe true
                settle()

                router.current().uri shouldBe "/edit/1"
                (document.querySelector(".editor") != null) shouldBe true
                control.asked shouldBe listOf("/edit/%31")
            }
        }

        "$strategy: a query-only change while asking keeps the question; a yes still goes" {
            val control = GuardControl()
            val answer = CompletableDeferred<Boolean>()
            control.answer = { LeaveResult.Ask { answer.await() } }

            withEditor(control, strategy) {
                router.navToUri("/other")
                settle()

                // A urlParam written while the modal is open
                router.replaceUri("/edit/1?tab=2")
                router.awaitUri("/edit/1?tab=2")

                answer.complete(true)
                router.awaitUri("/other")
            }
        }

        "$strategy: an Ask on a Back answered while something stands on the restored entry still leaves" {
            val control = GuardControl()
            val answer = CompletableDeferred<Boolean>()
            control.answer = { LeaveResult.Ask { answer.await() } }

            withEditor(control, strategy) {
                window.history.back()
                eventually { control.asked shouldBe listOf("/a") }
                settle()

                // A confirm UI's own history entry on top of the restored one, still there when the user answers
                window.history.pushState("confirm-ui", "", window.location.href)
                answer.complete(true)

                router.awaitUri("/a")
                settle()
                // The yes was the answer: the navigation it starts is not asked again
                control.asked shouldBe listOf("/a")
            }
        }

        "$strategy: an unmounted guard is not asked again when its page is opened anew" {
            val control = GuardControl()

            withEditor(control, strategy) {
                router.navToUri("/other")
                router.awaitUri("/other")
                router.navToUri("/edit/1")
                router.awaitUri("/edit/1")
                control.asked.clear()

                router.navToUri("/a")
                router.awaitUri("/a")
                settle()

                // One editor mounted, one guard asked: the first editor's guard went with it
                control.asked shouldBe listOf("/a")
            }
        }

        "$strategy: an Ask on a Back keeps its question over a query-only move; a yes still goes" {
            val control = GuardControl()
            val answer = CompletableDeferred<Boolean>()
            control.answer = { LeaveResult.Ask { answer.await() } }

            withEditor(control, strategy) {
                window.history.back()
                eventually { control.asked shouldBe listOf("/a") }
                settle()

                // A urlParam written while the question is open
                router.replaceUri("/edit/1?tab=2")
                router.awaitUri("/edit/1?tab=2")

                answer.complete(true)
                router.awaitUri("/a")
            }
        }

        "$strategy: a navigation past the guards drops a pending Ask and closes its question" {
            val control = GuardControl()
            var cancelled = false
            control.answer = {
                LeaveResult.Ask {
                    try {
                        CompletableDeferred<Boolean>().await()
                    } catch (e: CancellationException) {
                        cancelled = true
                        throw e
                    }
                }
            }

            withEditor(control, strategy) {
                router.navToUri("/other")
                settle()

                router.withoutLeaveGuards { router.navToUri("/login") }
                router.awaitUri("/login")
                settle()

                cancelled shouldBe true
            }
        }
    }

    "a query-only change is not leaving; other route params are" {
        val control = GuardControl().apply { answer = { LeaveResult.Stop } }

        withEditor(control) {
            router.replaceUri("/edit/1?tab=2")
            router.awaitUri("/edit/1?tab=2")
            control.asked shouldBe emptyList()

            router.navToUri("/edit/2")
            settle()
            router.current().uri shouldBe "/edit/1?tab=2"
            control.asked shouldBe listOf("/edit/2")
        }
    }

    "Hash: a typed hash is held, and the page's address put back" {
        val control = GuardControl().apply { answer = { LeaveResult.Stop } }

        withEditor(control) {
            window.location.hash = "/other"
            eventually { control.asked shouldBe listOf("/other") }
            settle()

            router.current().uri shouldBe "/edit/1"
            window.location.hash shouldBe "#/edit/1"
        }
    }

    "Hash: a route link whose own onClick navigated first navigates once" {
        val control = GuardControl()

        withEditor(control) {
            var emissions = 0
            val unsubscribe = router.current.subscribeToStream { emissions++ }
            emissions = 0

            // The link's own handler navigated: the address is already the link's
            router.navToUri("/other")
            clickLink("#/other")
            router.awaitUri("/other")
            settle()
            unsubscribe()

            emissions shouldBe 1
            control.asked shouldBe listOf("/other")
        }
    }

    "Path: a navigation that throws lets nothing through: a later Back is asked" {
        val control = GuardControl()

        withEditor(control, Strategy.Path) {
            // A backslash path resolves to another origin: the browser refuses to write it (no open redirect)
            runCatching { router.navToUri("/" + Char(0x5C) + "evil.example/a") }
            control.asked.clear()
            control.answer = { LeaveResult.Stop }

            window.history.back()
            eventually { control.asked shouldBe listOf("/a") }
        }
    }

    "Hash: a guard mounted before the router has a route (an app shell) is on no page, also not on the home page" {
        val control = GuardControl().apply { answer = { LeaveResult.Stop } }
        val originalHref = window.location.href

        try {
            TestBed.preact(
                appSetup = {
                    routing {
                        mount(Static("")) { div(classes = "home") { } }
                        mount(Static("/a")) { div(classes = "a") { } }
                    }
                },
                view = {
                    Editor(control)
                    RouterComponent()
                },
            ) {
                router.awaitUri("")
                router.navToUri("/a")
                router.awaitUri("/a")

                control.asked shouldBe emptyList()
            }
        } finally {
            window.history.replaceState(null, "", originalHref.substringBefore('#'))
        }
    }

    "beforeUnload: the browser's prompt is armed while the guard does not proceed" {
        // Karma treats a dispatched beforeunload as a page reload and aborts the run: the handler is called directly
        val handlers = mutableListOf<(Event) -> Unit>()
        val win = window.asDynamic()
        val add = win.addEventListener
        win.addEventListener = { type: String, handler: dynamic, options: dynamic ->
            if (type == "beforeunload") handlers.add(handler.unsafeCast<(Event) -> Unit>())
            add.call(window, type, handler, options)
        }

        val control = GuardControl().apply { answer = { LeaveResult.Stop } }
        val removed = mutableListOf<String>()
        val remove = win.removeEventListener
        win.removeEventListener = { type: String, handler: dynamic, options: dynamic ->
            if (type == "beforeunload") removed.add(type)
            remove.call(window, type, handler, options)
        }

        try {
            withEditor(control) {
                // The listener of the editor mounted last: an unmounted one is no longer on the window
                fun unload(): Boolean {
                    val options: dynamic = js("{}")
                    options.cancelable = true
                    val event = Event("beforeunload", options)
                    handlers.last()(event)
                    return event.defaultPrevented
                }

                handlers.size shouldBe 1

                unload() shouldBe true
                control.asked shouldBe listOf(null)

                control.answer = { LeaveResult.Proceed }
                unload() shouldBe false
                control.answer = { LeaveResult.Stop }

                // The router's own approvals (a guarded navigation let through) approve no page load
                control.answer = { LeaveResult.Proceed }
                router.navToUri("/edit/2")
                router.awaitUri("/edit/2")
                eventually { handlers.size shouldBe 2 }
                control.answer = { LeaveResult.Stop }
                unload() shouldBe true

                // A page load inside withoutLeaveGuards (a logout through the server) leaves without the prompt,
                // also when the browser fires beforeunload a little later
                router.withoutLeaveGuards { unload() } shouldBe false
                router.withoutLeaveGuards { }
                unload() shouldBe false
            }
        } finally {
            win.addEventListener = add
            win.removeEventListener = remove
        }

        // Each editor's beforeunload listener went with it: the one left for /edit/2, and the last with the TestBed
        removed shouldBe listOf("beforeunload", "beforeunload")
    }
})
