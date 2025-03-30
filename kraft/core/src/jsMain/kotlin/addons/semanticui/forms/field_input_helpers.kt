package de.peekandpoke.kraft.addons.semanticui.forms

import de.peekandpoke.kraft.utils.jsObject
import org.w3c.dom.events.InputEvent
import org.w3c.dom.events.KeyboardEvent

class UiInputFieldSimulator<T, P : UiInputFieldComponent.Props<T>>(val field: UiInputFieldComponent<T, P>) {

    fun inputValue(input: String) {
        field.inputElement.value = input
        field.inputElement.dispatchEvent(InputEvent("input"))
    }

    fun keysPressed(inputs: String) {

        val current = field.inputElement.value
        val start = field.inputElement.selectionStart ?: current.length
        val end = field.inputElement.selectionEnd ?: current.length

        val prefix = current.take(start)
        val suffix = current.takeLast(end - start)

        inputs.forEachIndexed { idx, ch ->

            val keyDown = KeyboardEvent(type = "keydown", eventInitDict = jsObject {
                key = ch.toString()
            })

            field.inputElement.dispatchEvent(keyDown)

            val keyUp = KeyboardEvent(type = "keyup", eventInitDict = jsObject {
                key = ch.toString()
            })

            field.inputElement.dispatchEvent(keyUp)

            val nextValue = "$prefix${inputs.take(idx + 1)}$suffix"

            inputValue(nextValue)
        }
    }
}

fun <T, P : UiInputFieldComponent.Props<T>> UiInputFieldComponent<T, P>.simulate() = UiInputFieldSimulator(this)
