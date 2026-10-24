package io.peekandpoke.kraft.semanticui.modals

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.peekandpoke.kraft.components.Ctx
import io.peekandpoke.kraft.components.comp
import io.peekandpoke.kraft.modals.DefaultModalsManager
import io.peekandpoke.kraft.modals.ModalsManager
import io.peekandpoke.kraft.modals.showAndAwait
import io.peekandpoke.kraft.testing.KQuery
import io.peekandpoke.kraft.testing.TestBed
import io.peekandpoke.kraft.testing.awaitCss
import io.peekandpoke.kraft.testing.awaitGone
import io.peekandpoke.kraft.testing.click
import io.peekandpoke.kraft.testing.eventually
import io.peekandpoke.kraft.testing.selectCss
import io.peekandpoke.kraft.testing.hasFocus
import io.peekandpoke.kraft.testing.selectDebugId
import io.peekandpoke.ultra.html.debugId
import io.peekandpoke.ultra.html.onClick
import io.peekandpoke.ultra.semanticui.ui
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.html.FlowContent
import kotlinx.html.Tag
import org.w3c.dom.Element
import org.w3c.dom.HTMLButtonElement

private val fastFade = FadingModal.Transition(fadeInTimeMs = 0, fadeOutTimeMs = 100)

private fun Tag.testModal(
    handle: ModalsManager.Handle<OkCancelModal.Result>,
    name: String = "modal",
    onResult: (OkCancelModal.Result?) -> Unit,
) = OkCancelModal(
    handle = handle,
    transition = fastFade,
    view = OkCancelModal.View(
        appearance = { mini },
        header = { +"Header $name" },
        content = { +"Content $name" },
        okText = { +"Yes" },
        cancelText = { +"No" },
    ),
    onResult = onResult,
)

/** A FadingModal subclass that overrides the deprecated close(onClose), as Klang's and Spross's modals do. */
private class FunnelModal(ctx: Ctx<Props>) : FadingModal<FunnelModal.Props, String>(ctx) {

    class Props(
        override val handle: ModalsManager.Handle<String>,
        val calls: MutableList<String>,
        val vetoFirst: Boolean,
    ) : FadingModal.Props<String>() {
        override val transition: Transition = fastFade
    }

    private var vetoed = false

    @Suppress("OVERRIDE_DEPRECATION")
    override fun close(onClose: (suspend () -> Unit)?) {
        props.calls += "override"

        if (props.vetoFirst && !vetoed) {
            vetoed = true
            return
        }

        @Suppress("DEPRECATION")
        super.close(onClose)
    }

    override fun FlowContent.renderContent() {
        ui.modal.transition.visible.active.front {
            debugId("funnel-modal")
            ui.button Button {
                debugId("answer-button")
                onClick { close("A") }
                +"Answer"
            }
        }
    }
}

private fun Tag.funnelModal(handle: ModalsManager.Handle<String>, calls: MutableList<String>, vetoFirst: Boolean = false) =
    comp(FunnelModal.Props(handle = handle, calls = calls, vetoFirst = vetoFirst)) { FunnelModal(it) }

private fun bodyHasNoScroll(): Boolean =
    document.body?.classList?.contains(FadingModal.Companion.Style.noScroll()) == true

private suspend fun KQuery<Element>.modalCount(): Int = selectDebugId("ok-cancel-modal").size

/** Renders a TestBed with [m] injected. TestBed unmounts the app at the end, so no modal outlives its test. */
private suspend fun withModals(m: DefaultModalsManager, test: suspend (KQuery<Element>) -> Unit) {
    TestBed.preact(
        appSetup = { setAttribute(ModalsManager.key, m) },
        view = { },
    ) { root ->
        test(root)
    }
}

@OptIn(DelicateCoroutinesApi::class)
class OkCancelModalSpec : StringSpec({

    "10: Ok answers Ok after the fade, and the modal is gone" {
        val m = DefaultModalsManager()
        val answers = mutableListOf<OkCancelModal.Result?>()

        withModals(m) { root ->
            m.show { h -> testModal(h) { answers += it } }
            root.awaitCss("[debug-id='ok-button']").click()

            answers.shouldBeEmpty()
            eventually { answers.shouldNotBeEmpty() }
            answers shouldBe listOf(OkCancelModal.Result.Ok)
            eventually { m().shouldBeEmpty() }
        }
    }

    "11: Cancel answers Cancel" {
        val m = DefaultModalsManager()
        val answers = mutableListOf<OkCancelModal.Result?>()

        withModals(m) { root ->
            m.show { h -> testModal(h) { answers += it } }
            root.awaitCss("[debug-id='cancel-button']").click()

            eventually { answers.shouldNotBeEmpty() }
            answers shouldBe listOf(OkCancelModal.Result.Cancel)
        }
    }

    "12: Ok then Cancel without a pause answers Ok only" {
        val m = DefaultModalsManager()
        val answers = mutableListOf<OkCancelModal.Result?>()

        withModals(m) { root ->
            m.show { h -> testModal(h) { answers += it } }
            root.awaitCss("[debug-id='ok-button']").click()
            root.selectDebugId("cancel-button").click()

            eventually { m().shouldBeEmpty() }
            answers shouldBe listOf(OkCancelModal.Result.Ok)
        }
    }

    "12b: backdrop then Ok without a pause answers null" {
        val m = DefaultModalsManager()
        val answers = mutableListOf<OkCancelModal.Result?>()

        withModals(m) { root ->
            m.show { h -> testModal(h) { answers += it } }
            root.awaitCss(".ui.dimmer").click()
            root.selectDebugId("ok-button").click()

            eventually { m().shouldBeEmpty() }
            answers shouldBe listOf(null)
        }
    }

    "12c: backdrop then a direct handle.resolve during the fade answers null" {
        val m = DefaultModalsManager()
        val answers = mutableListOf<OkCancelModal.Result?>()

        withModals(m) { root ->
            val handle = m.show { h -> testModal(h) { answers += it } }
            root.awaitCss(".ui.dimmer").click()
            handle.resolve(OkCancelModal.Result.Ok)

            eventually { m().shouldBeEmpty() }
            answers shouldBe listOf(null)
        }
    }

    "13: a backdrop click dismisses with null" {
        val m = DefaultModalsManager()
        val answers = mutableListOf<OkCancelModal.Result?>()

        withModals(m) { root ->
            m.show { h -> testModal(h) { answers += it } }
            root.awaitCss(".ui.dimmer").click()

            eventually { answers.shouldNotBeEmpty() }
            answers shouldBe listOf(null)
        }
    }

    "14: a click inside the content does not close" {
        val m = DefaultModalsManager()
        val answers = mutableListOf<OkCancelModal.Result?>()

        withModals(m) { root ->
            m.show { h -> testModal(h) { answers += it } }
            root.awaitCss("[debug-id='ok-cancel-modal']").click()

            delay(250)
            answers.shouldBeEmpty()
            m() shouldHaveSize 1
            root.modalCount() shouldBe 1
        }
    }

    "15: closeAll answers null, removes the modal and the body class" {
        val m = DefaultModalsManager()
        val answers = mutableListOf<OkCancelModal.Result?>()

        withModals(m) { root ->
            m.show { h -> testModal(h) { answers += it } }
            root.awaitCss("[debug-id='ok-cancel-modal']")
            bodyHasNoScroll() shouldBe true

            m.closeAll()

            answers shouldBe listOf(null)
            root.awaitGone("[debug-id=\'ok-cancel-modal\']")
            bodyHasNoScroll() shouldBe false
        }
    }

    "15b: with two stacked modals the body class stays until the last one is gone" {
        val m = DefaultModalsManager()

        withModals(m) { root ->
            m.show { h -> testModal(h, "a") { } }
            m.show { h -> testModal(h, "b") { } }
            eventually { root.modalCount() shouldBe 2 }

            root.selectDebugId("cancel-button").last().click()
            eventually { root.modalCount() shouldBe 1 }
            bodyHasNoScroll() shouldBe true

            root.selectDebugId("cancel-button").click()
            root.awaitGone("[debug-id=\'ok-cancel-modal\']")
            bodyHasNoScroll() shouldBe false
        }
    }

    "15c: closing the top modal leaves the one below open and unanswered" {
        val m = DefaultModalsManager()
        val answers = mutableListOf<String>()

        withModals(m) { root ->
            m.show { h -> testModal(h, "a") { answers += "a:$it" } }
            m.show { h -> testModal(h, "b") { answers += "b:$it" } }
            eventually { root.modalCount() shouldBe 2 }

            root.selectDebugId("cancel-button").last().click()
            eventually { answers.shouldNotBeEmpty() }
            // the back navigation of b's trap must not reach a's trap
            delay(300)

            answers shouldBe listOf("b:Cancel")
            m() shouldHaveSize 1
            root.modalCount() shouldBe 1
        }
    }

    "16: both buttons are real buttons of type button" {
        val m = DefaultModalsManager()

        withModals(m) { root ->
            m.show { h -> testModal(h) { } }
            root.awaitCss("[debug-id='ok-cancel-modal']")

            listOf("ok-button", "cancel-button").forEach { id ->
                val button = root.selectDebugId(id).single()
                withClue(id) {
                    button.tagName shouldBe "BUTTON"
                    (button as HTMLButtonElement).type shouldBe "button"
                }
            }
        }
    }

    "17: Cancel has the focus when the modal opens" {
        val m = DefaultModalsManager()

        withModals(m) { root ->
            m.show { h -> testModal(h) { } }
            root.awaitCss("[debug-id='cancel-button']")

            root.selectDebugId("cancel-button").hasFocus() shouldBe true
        }
    }

    "18: back navigation dismisses with null" {
        val m = DefaultModalsManager()
        val answers = mutableListOf<OkCancelModal.Result?>()

        withModals(m) { root ->
            m.show { h -> testModal(h) { answers += it } }
            root.awaitCss("[debug-id='ok-cancel-modal']")
            // The trap pushes its history entry on mount
            eventually { window.history.state shouldNotBe null }

            window.history.back()

            eventually { answers.shouldNotBeEmpty() }
            answers shouldBe listOf(null)
        }
    }

    "19: a modal opened from onResult renders, has the focus and answers once" {
        val m = DefaultModalsManager()
        val answers = mutableListOf<String>()

        withModals(m) { root ->
            m.show { h ->
                testModal(h, "first") {
                    answers += "first:$it"
                    m.show { h2 -> testModal(h2, "second") { answers += "second:$it" } }
                }
            }
            root.awaitCss("[debug-id='ok-button']").click()

            eventually {
                answers.size shouldBe 1
                root.modalCount() shouldBe 1
                m().size shouldBe 1
            }
            delay(50)

            withClue("the second modal is not fading out") {
                root.selectCss(".ui.dimmer.out").size shouldBe 0
            }
            root.selectDebugId("ok-cancel-modal").single().textContent shouldBe "Header secondContent secondNoYes"
            root.selectDebugId("cancel-button").hasFocus() shouldBe true

            root.selectDebugId("ok-button").click()
            eventually { answers shouldHaveSize 2 }
            delay(150)

            answers shouldBe listOf("first:Ok", "second:Ok")
        }
    }

    "20: a modal below another answers exactly once" {
        val m = DefaultModalsManager()
        val answers = mutableListOf<OkCancelModal.Result?>()

        withModals(m) { root ->
            m.show { h -> testModal(h, "a") { answers += it } }
            root.awaitCss("[debug-id='ok-cancel-modal']")
            m.show { h -> testModal(h, "b") { } }
            eventually { root.modalCount() shouldBe 2 }

            root.selectDebugId("ok-button").first().click()
            eventually { answers.shouldNotBeEmpty() }
            delay(150)

            answers shouldBe listOf(OkCancelModal.Result.Ok)
        }
    }

    "21: cancelling showAndAwait removes the modal and the body class" {
        val m = DefaultModalsManager()

        withModals(m) { root ->
            val job = GlobalScope.launch(start = CoroutineStart.UNDISPATCHED) {
                m.showAndAwait { h -> testModal(h) { } }
            }
            root.awaitCss("[debug-id='ok-cancel-modal']")
            bodyHasNoScroll() shouldBe true

            job.cancel()
            job.join()

            m().shouldBeEmpty()
            root.awaitGone("[debug-id=\'ok-cancel-modal\']")
            bodyHasNoScroll() shouldBe false
        }
    }

    "22: close(result), the backdrop and back navigation each reach an override of the deprecated close once" {
        listOf("answer", "backdrop", "back").forEach { how ->
            val m = DefaultModalsManager()
            val calls = mutableListOf<String>()
            val answers = mutableListOf<String?>()

            withModals(m) { root ->
                m.show { h -> funnelModal(h, calls) }.onResult { answers += it }
                root.awaitCss("[debug-id='funnel-modal']")

                when (how) {
                    "answer" -> root.selectDebugId("answer-button").click()
                    "backdrop" -> root.selectCss(".ui.dimmer").click()
                    else -> {
                        eventually { window.history.state shouldNotBe null }
                        window.history.back()
                    }
                }

                eventually { answers.shouldNotBeEmpty() }
                withClue(how) {
                    calls shouldBe listOf("override")
                    answers shouldBe listOf(if (how == "answer") "A" else null)
                }
            }
        }
    }

    "23: an override that vetoes a dismissal leaves the answer open" {
        val m = DefaultModalsManager()
        val calls = mutableListOf<String>()
        val answers = mutableListOf<String?>()

        withModals(m) { root ->
            m.show { h -> funnelModal(h, calls, vetoFirst = true) }.onResult { answers += it }
            root.awaitCss(".ui.dimmer").click()

            delay(200)
            m() shouldHaveSize 1
            answers.shouldBeEmpty()

            root.selectDebugId("answer-button").click()
            eventually { answers.shouldNotBeEmpty() }

            calls shouldBe listOf("override", "override")
            answers shouldBe listOf("A")
        }
    }
})
