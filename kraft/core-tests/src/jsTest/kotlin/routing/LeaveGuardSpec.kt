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
import io.peekandpoke.kraft.testing.TestBed
import io.peekandpoke.kraft.testing.TestBedScope
import io.peekandpoke.kraft.testing.awaitUri
import io.peekandpoke.kraft.vdom.VDom
import kotlinx.browser.window
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.html.Tag
import kotlinx.html.div
import org.w3c.dom.events.Event

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

/** An app on hash routing: "/a" (no guard), "/edit/{id}" (the guarded editor), "/other". */
private suspend fun withEditor(control: GuardControl, test: suspend TestBedScope.() -> Unit) {
    TestBed.preact(
        appSetup = {
            routing {
                mount(Static("/a")) { div(classes = "a") { } }
                mount(Route1("/edit/{id}")) { Editor(control) }
                mount(Static("/other")) { div(classes = "other") { } }
            }
        },
        view = { RouterComponent() },
    ) {
        router.navToUri("/a")
        router.awaitUri("/a")
        router.navToUri("/edit/1")
        router.awaitUri("/edit/1")
        control.asked.clear()

        test()
    }

    window.history.replaceState(null, "", window.location.pathname + window.location.search)
}

/** Lets a hashchange, a popstate and a re-render arrive. */
private suspend fun settle() = delay(60)

/** `guardLeaving` (ULTRA-00266, from Klang): a page holds a navigation away from it. */
class LeaveGuardSpec : StringSpec({

    "Stop holds navToUri: the route and the address stay" {
        val control = GuardControl().apply { answer = { LeaveResult.Stop } }

        withEditor(control) {
            router.navToUri("/other")
            settle()

            router.current().uri shouldBe "/edit/1"
            window.location.hash shouldBe "#/edit/1"
            control.asked shouldBe listOf("/other")
        }
    }

    "Proceed lets it through, asked once with the target" {
        val control = GuardControl()

        withEditor(control) {
            router.navToUri("/other")
            router.awaitUri("/other")
            settle()

            control.asked shouldBe listOf("/other")
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

    "Ask: nothing moves while asking; yes goes on to the target with one new history entry" {
        val control = GuardControl()
        val answer = CompletableDeferred<Boolean>()
        control.answer = { LeaveResult.Ask { answer.await() } }

        withEditor(control) {
            val entries = window.history.length

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

            window.history.length shouldBe entries + 1
            control.asked shouldBe listOf("/other")
        }
    }

    "Ask: no stays" {
        val control = GuardControl().apply { answer = { LeaveResult.Ask { false } } }

        withEditor(control) {
            router.navToUri("/other")
            settle()

            router.current().uri shouldBe "/edit/1"
            window.location.hash shouldBe "#/edit/1"
        }
    }

    "a Back is held by Stop and the address put back" {
        val control = GuardControl().apply { answer = { LeaveResult.Stop } }

        withEditor(control) {
            window.history.back()
            settle()
            settle()

            router.current().uri shouldBe "/edit/1"
            window.location.hash shouldBe "#/edit/1"
            control.asked shouldBe listOf("/a")
        }
    }

    "a Back that the guard lets through follows the browser" {
        val control = GuardControl()

        withEditor(control) {
            window.history.back()
            router.awaitUri("/a")

            control.asked shouldBe listOf("/a")
        }
    }

    "the guard goes with its component: after leaving, nothing asks" {
        val control = GuardControl()

        withEditor(control) {
            router.navToUri("/other")
            router.awaitUri("/other")
            control.asked.clear()

            router.navToUri("/a")
            router.awaitUri("/a")
            settle()

            control.asked shouldBe emptyList()
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

        try {
            withEditor(control) {
                fun unload(): Boolean {
                    val options: dynamic = js("{}")
                    options.cancelable = true
                    val event = Event("beforeunload", options)
                    handlers.forEach { it(event) }
                    return event.defaultPrevented
                }

                handlers.size shouldBe 1

                unload() shouldBe true
                control.asked shouldBe listOf(null)

                control.answer = { LeaveResult.Proceed }
                unload() shouldBe false
            }
        } finally {
            win.addEventListener = add
        }
    }
})
