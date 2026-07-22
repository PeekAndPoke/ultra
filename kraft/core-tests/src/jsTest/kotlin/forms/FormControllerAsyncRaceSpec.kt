package io.peekandpoke.kraft.coretests.forms

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.kraft.components.Ctx
import io.peekandpoke.kraft.components.NoProps
import io.peekandpoke.kraft.components.PureComponent
import io.peekandpoke.kraft.components.comp
import io.peekandpoke.kraft.forms.FormController
import io.peekandpoke.kraft.forms.FormFieldComponent
import io.peekandpoke.kraft.forms.validation.Rule
import io.peekandpoke.kraft.forms.validation.given
import io.peekandpoke.kraft.testing.TestBed
import io.peekandpoke.kraft.testing.click
import io.peekandpoke.kraft.testing.selectCss
import io.peekandpoke.kraft.testing.textContent
import io.peekandpoke.kraft.testing.typeText
import io.peekandpoke.kraft.vdom.VDom
import io.peekandpoke.ultra.html.onClick
import io.peekandpoke.ultra.html.onInput
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.html.InputType
import kotlinx.html.Tag
import kotlinx.html.button
import kotlinx.html.div
import kotlinx.html.input
import org.w3c.dom.HTMLInputElement

@Suppress("TestFunctionName")
private fun Tag.TestField(
    initialValue: String,
    cssClass: String,
    rules: List<Rule<String>>,
) = comp(
    TestField.Props(
        initialValue = initialValue,
        fromStr = { it },
        onChange = {},
        rules = rules,
        cssClass = cssClass,
    )
) { TestField(it) }

/** Minimal string field: an <input> wired to the built-in [setInput], with the given rules. */
private class TestField(ctx: Ctx<Props>) : FormFieldComponent<String, TestField.Props>(ctx) {
    data class Props(
        override val initialValue: String,
        override val fromStr: (String) -> String,
        override val onChange: (String) -> Unit,
        override val rules: List<Rule<String>>,
        val cssClass: String,
    ) : FormFieldComponent.Props<String>

    override fun VDom.render() {
        input(type = InputType.text, classes = props.cssClass) {
            onInput { evt -> setInput((evt.target as HTMLInputElement).value) }
        }
    }
}

@Suppress("TestFunctionName")
private fun Tag.RaceForm() = comp { RaceForm(it) }

/**
 * Two fields: A is validated instantly, B carries an async rule parked on [gateB]. Reproduces the
 * race where A is edited to become invalid while B is still validating.
 */
private class RaceForm(ctx: NoProps) : PureComponent(ctx) {

    private val formCtrl = FormController(this)
    private var submitCount by value(0)
    private val gateB = CompletableDeferred<Unit>()

    override fun VDom.render() {
        div(classes = "count") { +"$submitCount" }

        // A: passes only when its value is exactly "valid" (instant).
        TestField("valid", "field-a", listOf(given({ it == "valid" }) { "A must be valid" }))

        // B: always passes, but only once the test releases gateB (simulates a slow server check).
        TestField("x", "field-b", listOf(given({ gateB.await(); true }) { "B invalid" }))

        button(classes = "submit") {
            onClick { formCtrl.validate { submitCount++ } }
        }

        button(classes = "release-b") {
            onClick { gateB.complete(Unit) }
        }
    }
}

class FormControllerAsyncRaceSpec : StringSpec({

    "onValid is NOT called when a fast field turns invalid while a slow async field is still validating" {
        TestBed.preact({ RaceForm() }) { root ->
            // Submit: field A validates instantly (valid); field B parks on gateB.
            root.selectCss("button.submit").click()
            delay(50)

            // While B is still validating, edit A so it no longer satisfies its rule.
            root.selectCss("input.field-a").typeText("invalid")
            delay(50)

            // Now let B's async validation complete.
            root.selectCss("button.release-b").click()
            delay(50)

            // A became invalid during the async window → the submit must be rejected.
            root.selectCss(".count").textContent() shouldBe "0"
        }
    }

    "onValid IS called when every field (including the slow async one) ends up valid" {
        TestBed.preact({ RaceForm() }) { root ->
            root.selectCss("button.submit").click()
            delay(50)

            // A is left at its valid initial value; release B.
            root.selectCss("button.release-b").click()
            delay(50)

            root.selectCss(".count").textContent() shouldBe "1"
        }
    }
})
