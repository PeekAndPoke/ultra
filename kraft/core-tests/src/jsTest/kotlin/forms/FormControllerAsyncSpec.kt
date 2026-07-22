package io.peekandpoke.kraft.coretests.forms

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.kraft.components.NoProps
import io.peekandpoke.kraft.components.PureComponent
import io.peekandpoke.kraft.components.comp
import io.peekandpoke.kraft.forms.FormController
import io.peekandpoke.kraft.testing.TestBed
import io.peekandpoke.kraft.testing.click
import io.peekandpoke.kraft.testing.selectCss
import io.peekandpoke.kraft.testing.textContent
import io.peekandpoke.kraft.vdom.VDom
import io.peekandpoke.ultra.html.onClick
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.html.Tag
import kotlinx.html.button
import kotlinx.html.div

@Suppress("TestFunctionName")
private fun Tag.AsyncSubmitForm() = comp { AsyncSubmitForm(it) }

/**
 * A minimal form component (no fields → always valid) that drives [FormController.validate] so the
 * status pipeline and the double-submit guard can be observed end-to-end from the DOM.
 */
private class AsyncSubmitForm(ctx: NoProps) : PureComponent(ctx) {

    private val formCtrl = FormController(this)

    private var submitCount by value(0)

    // Lets the test hold the onValid callback in the PROCESSING state until it clicks "complete".
    private var gate = CompletableDeferred<Unit>()

    override fun VDom.render() {
        div(classes = "status") { +formCtrl.status.name }
        div(classes = "count") { +"$submitCount" }

        button(classes = "submit") {
            onClick {
                formCtrl.validate {
                    gate.await()
                    submitCount++
                }
            }
        }

        button(classes = "complete") {
            onClick { gate.complete(Unit) }
        }

        button(classes = "boom") {
            onClick {
                formCtrl.validate {
                    throw RuntimeException("boom")
                }
            }
        }
    }
}

class FormControllerAsyncSpec : StringSpec({

    "validate { } drives status IDLE -> PROCESSING -> IDLE and runs onValid exactly once" {
        TestBed.preact({ AsyncSubmitForm() }) { root ->
            root.selectCss(".status").textContent() shouldBe "IDLE"

            // start the submit; the onValid callback parks in the gate → controller stays PROCESSING
            root.selectCss("button.submit").click()
            delay(50)
            root.selectCss(".status").textContent() shouldBe "PROCESSING"
            root.selectCss(".count").textContent() shouldBe "0"

            // let onValid finish
            root.selectCss("button.complete").click()
            delay(50)
            root.selectCss(".status").textContent() shouldBe "IDLE"
            root.selectCss(".count").textContent() shouldBe "1"
        }
    }

    "validate { } drops a SAME-TICK second click (guard flips synchronously, no race window)" {
        TestBed.preact({ AsyncSubmitForm() }) { root ->
            // Two clicks in the same event-loop tick, before the first launched coroutine is
            // dispatched. The guard must already read busy on the second click.
            root.selectCss("button.submit").click()
            root.selectCss("button.submit").click()
            delay(50)
            root.selectCss(".status").textContent() shouldBe "PROCESSING"

            root.selectCss("button.complete").click()
            delay(50)
            // exactly one onValid ran despite two same-tick clicks
            root.selectCss(".count").textContent() shouldBe "1"
        }
    }

    "validate { } is a no-op while busy — prevents double-clicks/double-submits" {
        TestBed.preact({ AsyncSubmitForm() }) { root ->
            root.selectCss("button.submit").click()
            delay(50)
            root.selectCss(".status").textContent() shouldBe "PROCESSING"

            // a second (and third) click while PROCESSING must be ignored
            root.selectCss("button.submit").click()
            root.selectCss("button.submit").click()
            delay(50)
            root.selectCss(".count").textContent() shouldBe "0"

            root.selectCss("button.complete").click()
            delay(50)
            // onValid ran exactly once despite three clicks
            root.selectCss(".count").textContent() shouldBe "1"
        }
    }

    "a throwing onValid moves the status to ERROR" {
        TestBed.preact({ AsyncSubmitForm() }) { root ->
            root.selectCss("button.boom").click()
            delay(50)
            root.selectCss(".status").textContent() shouldBe "ERROR"
            root.selectCss(".count").textContent() shouldBe "0"
        }
    }
})
