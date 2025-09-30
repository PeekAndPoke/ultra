package de.peekandpoke.kraft.coretests.messages

import de.peekandpoke.kraft.components.Component
import de.peekandpoke.kraft.components.Ctx
import de.peekandpoke.kraft.components.NoProps
import de.peekandpoke.kraft.components.PureComponent
import de.peekandpoke.kraft.components.comp
import de.peekandpoke.kraft.components.onClick
import de.peekandpoke.kraft.messages.MessageBase
import de.peekandpoke.kraft.messages.onMessage
import de.peekandpoke.kraft.messages.sendMessage
import de.peekandpoke.kraft.testing.TestBed
import de.peekandpoke.kraft.testing.click
import de.peekandpoke.kraft.testing.textContent
import de.peekandpoke.kraft.vdom.VDom
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay
import kotlinx.html.Tag
import kotlinx.html.button
import kotlinx.html.div
import kotlinx.html.h2

private class TestMessage(sender: Component<*>) : MessageBase<Component<*>>(sender) {
    companion object {
        var receivedCounter = 0
    }
}

@Suppress("TestFunctionName")
private fun Tag.SimpleSender() = comp {
    SimpleSender(it)
}

private class SimpleSender(ctx: NoProps) : PureComponent(ctx) {

    //  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    init {
        onMessage<TestMessage> {
            TestMessage.receivedCounter++
        }
    }

    override fun VDom.render() {

        h2 {
            +"Simple Sender"
        }

        div {
            button(classes = "send-button") {
                onClick {
                    sendMessage(TestMessage(this@SimpleSender))
                }
                +"SEND"
            }
        }
    }
}

@Suppress("TestFunctionName")
private fun Tag.InBetween(stopMessages: Boolean) = comp(
    InBetween.Props(
        stopMessages = stopMessages,
    )
) {
    InBetween(it)
}

private class InBetween(ctx: Ctx<Props>) : Component<InBetween.Props>(ctx) {

    data class Props(
        val stopMessages: Boolean,
    )

    //  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    init {
        onMessage<TestMessage> {
            if (props.stopMessages) {
                it.stop()
            }
        }
    }

    override fun VDom.render() {

        h2 {
            +"InBetween"
        }

        SimpleSender()
    }
}

@Suppress("TestFunctionName")
private fun Tag.SimpleReceiver(stopMessages: Boolean) = comp(
    SimpleReceiver.Props(
        stopMessages = stopMessages
    )
) {
    SimpleReceiver(it)
}

private class SimpleReceiver(ctx: Ctx<Props>) : Component<SimpleReceiver.Props>(ctx) {

    data class Props(
        val stopMessages: Boolean,
    )

    //  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    var count by value(0)

    //  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    init {
        onMessage<TestMessage> {
            TestMessage.receivedCounter++
            count++
        }
    }

    override fun VDom.render() {

        h2 {
            +"Simple Receiver"
        }

        div(classes = "received-count") { +"$count" }

        InBetween(stopMessages = props.stopMessages)
    }
}

class MessagesSpec : StringSpec({

    "A message must not be received by the sender itself" {

        TestBed.preact({
            SimpleSender()
        }) { root ->

            delay(50)

            repeat(5) {
                root.selectCss("button.send-button").click()
                delay(20)
            }

            delay(50)

            TestMessage.receivedCounter shouldBe 0
        }
    }

    "A message must be received by the parent component" {

        TestBed.preact({
            SimpleReceiver(stopMessages = false)
        }) { root ->

            delay(50)

            repeat(5) {
                root.selectCss("button.send-button").click()
                delay(10)
            }

            delay(50)

            root.selectCss(".received-count").textContent() shouldBe "5"
        }
    }

    "A message must not be received by the parent when it is stopped in between" {

        TestBed.preact({
            SimpleReceiver(stopMessages = true)
        }) { root ->

            delay(50)

            repeat(5) {
                root.selectCss("button.send-button").click()
                delay(10)
            }

            delay(50)

            root.selectCss(".received-count").textContent() shouldBe "0"
        }
    }
})
