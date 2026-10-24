package io.peekandpoke.kraft.semanticui.modals

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.peekandpoke.kraft.modals.DefaultModalsManager
import io.peekandpoke.kraft.modals.ModalsManager
import io.peekandpoke.kraft.modals.showAndAwait
import io.peekandpoke.kraft.testing.KQuery
import io.peekandpoke.kraft.testing.TestBed
import io.peekandpoke.kraft.testing.awaitCss
import io.peekandpoke.kraft.testing.click
import io.peekandpoke.kraft.testing.first
import io.peekandpoke.kraft.testing.last
import io.peekandpoke.kraft.testing.selectCss
import io.peekandpoke.kraft.testing.selectDebugId
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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

/** Polls [condition] until it holds; fails the test after [timeoutMs]. */
private suspend fun until(clue: String, timeoutMs: Int = 2000, condition: () -> Boolean) {
    var remaining = timeoutMs
    while (!condition()) {
        if (remaining <= 0) {
            withClue(clue) { condition() shouldBe true }
        }
        delay(10)
        remaining -= 10
    }
}

private fun bodyHasNoScroll(): Boolean =
    document.body?.classList?.contains(FadingModal.Companion.Style.noScroll()) == true

/** Counts the open OkCancelModals in the DOM, without suspending (usable in [until]). */
private fun modalsInDom(): Int = document.querySelectorAll("[debug-id='ok-cancel-modal']").length

/** Renders a TestBed with [m] injected and always closes every modal at the end. */
private suspend fun withModals(m: DefaultModalsManager, test: suspend (KQuery<Element>) -> Unit) {
    TestBed.preact(
        appSetup = { setAttribute(ModalsManager.key, m) },
        view = { },
    ) { root ->
        try {
            test(root)
        } finally {
            m.closeAll()
            until("all modals unmounted at the end") { modalsInDom() == 0 }
        }
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
            until("answered") { answers.isNotEmpty() }
            answers shouldBe listOf(OkCancelModal.Result.Ok)
            until("modal gone") { m().isEmpty() }
        }
    }

    "11: Cancel answers Cancel" {
        val m = DefaultModalsManager()
        val answers = mutableListOf<OkCancelModal.Result?>()

        withModals(m) { root ->
            m.show { h -> testModal(h) { answers += it } }
            root.awaitCss("[debug-id='cancel-button']").click()

            until("answered") { answers.isNotEmpty() }
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

            until("modal gone") { m().isEmpty() }
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

            until("modal gone") { m().isEmpty() }
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

            until("modal gone") { m().isEmpty() }
            answers shouldBe listOf(null)
        }
    }

    "13: a backdrop click dismisses with null" {
        val m = DefaultModalsManager()
        val answers = mutableListOf<OkCancelModal.Result?>()

        withModals(m) { root ->
            m.show { h -> testModal(h) { answers += it } }
            root.awaitCss(".ui.dimmer").click()

            until("answered") { answers.isNotEmpty() }
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
            modalsInDom() shouldBe 1
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
            until("modal unmounted") { modalsInDom() == 0 }
            bodyHasNoScroll() shouldBe false
        }
    }

    "15b: with two stacked modals the body class stays until the last one is gone" {
        val m = DefaultModalsManager()

        withModals(m) { root ->
            m.show { h -> testModal(h, "a") { } }
            m.show { h -> testModal(h, "b") { } }
            until("both rendered") { modalsInDom() == 2 }

            root.selectDebugId("cancel-button").last().click()
            until("top one gone") { modalsInDom() == 1 }
            bodyHasNoScroll() shouldBe true

            root.selectDebugId("cancel-button").click()
            until("second one gone") { modalsInDom() == 0 }
            bodyHasNoScroll() shouldBe false
        }
    }

    "15c: closing the top modal leaves the one below open and unanswered" {
        val m = DefaultModalsManager()
        val answers = mutableListOf<String>()

        withModals(m) { root ->
            m.show { h -> testModal(h, "a") { answers += "a:$it" } }
            m.show { h -> testModal(h, "b") { answers += "b:$it" } }
            until("both rendered") { modalsInDom() == 2 }

            root.selectDebugId("cancel-button").last().click()
            until("b answered") { answers.isNotEmpty() }
            // the back navigation of b's trap must not reach a's trap
            delay(300)

            answers shouldBe listOf("b:Cancel")
            m() shouldHaveSize 1
            modalsInDom() shouldBe 1
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
            val cancel = root.awaitCss("[debug-id='cancel-button']").single()

            document.activeElement shouldBe cancel
        }
    }

    "18: back navigation dismisses with null" {
        val m = DefaultModalsManager()
        val answers = mutableListOf<OkCancelModal.Result?>()

        withModals(m) { root ->
            m.show { h -> testModal(h) { answers += it } }
            root.awaitCss("[debug-id='ok-cancel-modal']")
            // The trap pushes its history entry on mount
            until("trap active") { window.history.state != null }

            window.history.back()

            until("answered") { answers.isNotEmpty() }
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

            until("second rendered") {
                answers.size == 1 && modalsInDom() == 1 && m().size == 1
            }
            delay(50)

            withClue("the second modal is not fading out") {
                root.selectCss(".ui.dimmer.out").size shouldBe 0
            }
            root.selectDebugId("ok-cancel-modal").single().textContent shouldBe "Header secondContent secondNoYes"
            document.activeElement shouldBe root.selectDebugId("cancel-button").single()

            root.selectDebugId("ok-button").click()
            until("second answered") { answers.size == 2 }
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
            until("both rendered") { modalsInDom() == 2 }

            root.selectDebugId("ok-button").first().click()
            until("a answered") { answers.isNotEmpty() }
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
            until("modal unmounted") { modalsInDom() == 0 }
            bodyHasNoScroll() shouldBe false
        }
    }
})
