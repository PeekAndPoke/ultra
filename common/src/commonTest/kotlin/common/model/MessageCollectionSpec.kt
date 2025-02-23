package de.peekandpoke.ultra.common.model

import de.peekandpoke.ultra.common.datetime.MpInstant
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.async
import kotlinx.coroutines.delay

class MessageCollectionSpec : StringSpec() {
    init {
        "Creation using messages()" {
            val now = MpInstant.now()

            val result = messages("Title") {
                addInfo("Text", now)
                addWarning("Warning", now)
                addError("Error", now)
            }

            result.title shouldBe "Title"
            result.messages shouldBe listOf(
                Message(type = Message.Type.info, text = "Text", ts = now),
                Message(type = Message.Type.warning, text = "Warning", ts = now),
                Message(type = Message.Type.error, text = "Error", ts = now),
            )
        }

        "Creation using withMessages()" {
            val now = MpInstant.now()

            val result = withMessages("Title") {
                addInfo("Text", now)

                "Result"
            }

            result.messages.title shouldBe "Title"
            result.messages.messages shouldBe listOf(
                Message(type = Message.Type.info, text = "Text", ts = now)
            )
        }

        "Creation using messages() with async inner function" {
            val now = MpInstant.now()

            val result = messages("Title") {
                addInfo("Text", now)
                addWarning("Warning", now)
                addError("Error", now)

                async {
                    delay(50)
                    "Result"
                }.await()
            }

            result.title shouldBe "Title"
            result.messages shouldBe listOf(
                Message(type = Message.Type.info, text = "Text", ts = now),
                Message(type = Message.Type.warning, text = "Warning", ts = now),
                Message(type = Message.Type.error, text = "Error", ts = now),
            )
        }

        "Creation using withMessages() with async inner function" {
            val now = MpInstant.now()

            val result = withMessages("Title") {
                addInfo("Text", now)

                async {
                    delay(50)
                    "Result"
                }.await()
            }

            result.messages.title shouldBe "Title"
            result.messages.messages shouldBe listOf(
                Message(type = Message.Type.info, text = "Text", ts = now)
            )
        }
    }
}
