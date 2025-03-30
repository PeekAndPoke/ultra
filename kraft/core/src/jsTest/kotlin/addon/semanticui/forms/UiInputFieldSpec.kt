package de.peekandpoke.kraft.addon.semanticui.forms

import de.peekandpoke.kraft.addons.semanticui.forms.UiInputField
import de.peekandpoke.kraft.addons.semanticui.forms.UiInputFieldComponent
import de.peekandpoke.kraft.addons.semanticui.forms.simulate
import de.peekandpoke.kraft.components.ComponentRef
import de.peekandpoke.kraft.testbed.TestBed
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class UiInputFieldSpec : StringSpec({

    "UiInputField | String | Rendering" {

        var data = "initial"

        val ref = ComponentRef.Tracker<UiInputFieldComponent<String, UiInputFieldComponent.Props<String>>>()

        TestBed.preact(
            {
                UiInputField(data, { data = it }).track(ref)
            }
        ) {
            val field = ref.get()

            withClue("Initial rendering should set the correct value") {
                field.inputElement.value shouldBe "initial"
                field.currentValue shouldBe "initial"
            }
        }
    }

    "UiInputField | String | Changing the value" {

        var data = "initial"

        val ref = ComponentRef.Tracker<UiInputFieldComponent<String, UiInputFieldComponent.Props<String>>>()

        TestBed.preact(
            {
                UiInputField(data, { data = it }).track(ref)
            }
        ) {
            val field = ref()!!

            withClue("Initial rendering should set the correct value") {
                field.inputElement.value shouldBe "initial"
            }

            withClue("Changing the value must work") {
                field.simulate().inputValue("changed")

                withClue("inputElement.value must be updated") {
                    field.inputElement.value shouldBe "changed"
                }

                withClue("field.currentValue must be updated") {
                    field.currentValue shouldBe "changed"
                }

                withClue("data must be updated") {
                    data shouldBe "changed"
                }
            }
        }
    }

    "UiInputField | String | Simulating keyboard inputs" {

        var data = "initial"

        val ref = ComponentRef.Tracker<UiInputFieldComponent<String, UiInputFieldComponent.Props<String>>>()

        TestBed.preact(
            {
                UiInputField(data, { data = it }).track(ref)
            }
        ) {
            val field = ref()!!

            withClue("Initial rendering should set the correct value") {
                field.inputElement.value shouldBe "initial"
            }

            withClue("Changing the value by typing keys must work") {

                val inputs = "+123"
                field.simulate().keysPressed(inputs)


                withClue("inputElement.value must be updated") {
                    field.inputElement.value shouldBe "initial$inputs"
                }

                withClue("field.currentValue must be updated") {
                    field.currentValue shouldBe "initial$inputs"
                }

                withClue("data must be updated") {
                    data shouldBe "initial$inputs"
                }
            }
        }
    }
})
