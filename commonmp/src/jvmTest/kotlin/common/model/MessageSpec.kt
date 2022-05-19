package de.peekandpoke.ultra.common.model

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.shouldBe

class MessageSpec : FreeSpec() {

    init {

        "MessageType" - {
            "MessageType values must not change" {
                Message.Type.info.toString() shouldBe "info"
                Message.Type.info.name shouldBe "info"

                Message.Type.warning.toString() shouldBe "warning"
                Message.Type.warning.name shouldBe "warning"

                Message.Type.error.toString() shouldBe "error"
                Message.Type.error.name shouldBe "error"
            }

            "MessageType order must not change" {

                Message.Type.info shouldBeLessThan Message.Type.warning

                Message.Type.info shouldBeLessThan Message.Type.error

                Message.Type.warning shouldBeLessThan Message.Type.error
            }
        }

        "Adding messages" - {

            "Adding nothing" {

                val subject = Messages(title = "empty").withMessage()

                subject.getWorstMessageType() shouldBe Message.Type.info
                subject.getAllMessages() shouldBe emptyList()
            }

            "Adding multiple" {

                val children = listOf(
                    Messages("child")
                )

                val subject = Messages(
                    title = "adding",
                    messages = listOf(
                        Message.info("#1")
                    ),
                    children = children
                ).withMessage(
                    Message.info("#2"),
                    Message.info("#3"),
                )

                subject.getWorstMessageType() shouldBe Message.Type.info
                subject.isSuccess shouldBe true
                subject.isWarningOrBetter shouldBe true
                subject.isError shouldBe false

                subject.getAllMessages() shouldBe listOf(
                    Message.info("#1"),
                    Message.info("#2"),
                    Message.info("#3"),
                )

                subject.children shouldBe children
            }
        }

        "Simple List without children" - {

            "Empty collection" {

                val subject = Messages(title = "empty")

                subject.title shouldBe "empty"
                subject.getWorstMessageType() shouldBe Message.Type.info
                subject.getAllMessages() shouldBe emptyList()
            }

            "Most severe type 'info'" {

                val messages = listOf(
                    Message.info("This is an info")
                )

                val subject = MessageCollection(title = "test", messages = messages)

                subject.getWorstMessageType() shouldBe Message.Type.info
                subject.isSuccess shouldBe true
                subject.isWarningOrBetter shouldBe true
                subject.isError shouldBe false

                subject.getAllMessages() shouldBe messages
            }

            "Most severe type 'warning' #1" {

                val messages = listOf(
                    Message.info("This is an info"),
                    Message.warning("This is a warning")
                )

                val subject = MessageCollection(title = "test", messages = messages)

                subject.getWorstMessageType() shouldBe Message.Type.warning
                subject.getAllMessages() shouldBe messages
            }

            "Most severe type 'warning' #2" {

                val messages = listOf(
                    Message.warning("This is a warning"),
                    Message.info("This is an info")
                )

                val subject = MessageCollection(title = "test", messages = messages)

                subject.getWorstMessageType() shouldBe Message.Type.warning
                subject.isSuccess shouldBe false
                subject.isWarningOrBetter shouldBe true
                subject.isError shouldBe false

                subject.getAllMessages() shouldBe messages
            }

            "Most severe type 'error' #1" {

                val messages = listOf(
                    Message.error("This is an error"),
                    Message.warning("This is an info"),
                    Message.info("This is an info"),
                )

                val subject = MessageCollection(title = "test", messages = messages)

                subject.getWorstMessageType() shouldBe Message.Type.error
                subject.getAllMessages() shouldBe messages
            }

            "Most severe type 'error' #2" {

                val messages = listOf(
                    Message.warning("This is an info"),
                    Message.error("This is an error"),
                    Message.info("This is an info"),
                )

                val subject = MessageCollection(title = "test", messages = messages)

                subject.getWorstMessageType() shouldBe Message.Type.error
                subject.isSuccess shouldBe false
                subject.isWarningOrBetter shouldBe false
                subject.isError shouldBe true

                subject.getAllMessages() shouldBe messages
            }

            "Most severe type 'error' #3" {

                val messages = listOf(
                    Message.warning("This is an info"),
                    Message.info("This is an info"),
                    Message.error("This is an error"),
                )

                val subject = MessageCollection(title = "test", messages = messages)

                subject.getWorstMessageType() shouldBe Message.Type.error
                subject.getAllMessages() shouldBe messages
            }
        }

        "Nested Lists with children" - {

            "Most severe is 'info' #1" {

                val subject = Messages(
                    title = "nested",
                    children = listOf(
                        Messages(
                            title = "nested 1",
                            children = listOf(
                                Messages(title = "nested 1-1"),
                            )
                        ),
                        Messages("nested 2"),
                    )
                )

                subject.getWorstMessageType() shouldBe Message.Type.info
            }

            "Most severe is 'info' #2" {

                val subject = Messages(
                    title = "nested",
                    children = listOf(
                        Messages(
                            title = "nested 1",
                            children = listOf(
                                Messages(title = "nested 1-1")
                                    .withMessage(Message.info("Info")),
                            )
                        ),
                        Messages("nested 2"),
                    )
                )

                subject.getWorstMessageType() shouldBe Message.Type.info
            }

            "Most severe is 'warning'" {

                val subject = Messages(
                    title = "nested",
                    children = listOf(
                        Messages(
                            title = "nested 1",
                            children = listOf(
                                Messages(title = "nested 1-1")
                                    .withMessage(Message.warning("Warning")),
                            )
                        ),
                        Messages("nested 2"),
                    )
                )

                subject.getWorstMessageType() shouldBe Message.Type.warning
            }

            "Most severe is 'error'" {

                val subject = Messages(
                    title = "nested",
                    children = listOf(
                        Messages(
                            title = "nested 1",
                            children = listOf(
                                Messages(title = "nested 1-1")
                                    .withMessage(Message.error("Error")),
                            )
                        ),
                        Messages("nested 2"),
                    )
                )

                subject.getWorstMessageType() shouldBe Message.Type.error
            }
        }
    }
}
