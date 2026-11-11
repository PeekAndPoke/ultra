package io.peekandpoke.kraft.coretests.messages

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.kraft.messages.MessageBase
import io.peekandpoke.kraft.messages.MessagesHandler

/** The message bus delivers every message to every handler (ULTRA-00307). */
class MessagesHandlerSpec : StringSpec({

    class Note(val text: String) : MessageBase<String>(sender = "test")

    "a message sent by a handler during a dispatch does not keep the first message from later handlers" {
        val bus = MessagesHandler()
        val second = mutableListOf<String>()

        bus.stream.subscribeToStream { if ((it as? Note)?.text == "x") bus.send(Note("y")) }
        bus.stream.subscribeToStream { (it as? Note)?.let { note -> second.add(note.text) } }

        bus.send(Note("x"))

        // As before ULTRA-00307: the nested message is delivered at once, then the first one goes on
        second shouldBe listOf("y", "x")
    }

    "a handler unsubscribed during a dispatch is not called with that message" {
        val bus = MessagesHandler()
        val seen = mutableListOf<String>()
        lateinit var leave: () -> Unit

        bus.stream.subscribeToStream { if ((it as? Note)?.text == "x") leave() }
        leave = bus.stream.subscribeToStream { (it as? Note)?.let { note -> seen.add(note.text) } }

        bus.send(Note("x"))

        seen shouldBe emptyList()
    }

    "a handler registered after a message is not called with it, and gets the next one (ULTRA-00310)" {
        val bus = MessagesHandler()
        bus.send(Note("x"))

        val told = mutableListOf<String?>()
        bus.stream.subscribeToStream { told.add((it as? Note)?.text) }

        // Told "no message" first, never the past one; the bus keeps no message to read either
        told shouldBe listOf(null)
        bus.stream() shouldBe null

        bus.send(Note("y"))

        told shouldBe listOf(null, "y")
        bus.stream() shouldBe null
    }

    "a handler that throws while subscribing is not kept" {
        val bus = MessagesHandler()
        val seen = mutableListOf<String?>()

        runCatching {
            bus.stream.subscribeToStream { seen.add((it as? Note)?.text); if (it == null) error("boom") }
        }

        bus.send(Note("y"))

        // Called with the initial null, then never again
        seen shouldBe listOf(null)
    }

    "a handler that throws during a send does not keep the message from the others" {
        val bus = MessagesHandler()
        val second = mutableListOf<String>()

        bus.stream.subscribeToStream { if (it != null) error("boom") }
        bus.stream.subscribeToStream { (it as? Note)?.let { note -> second.add(note.text) } }

        bus.send(Note("x"))

        second shouldBe listOf("x")
    }

    "a handler subscribing during a dispatch gets only the messages sent after it" {
        val bus = MessagesHandler()
        val late = mutableListOf<String?>()

        bus.stream.subscribeToStream {
            if ((it as? Note)?.text == "x") {
                bus.stream.subscribeToStream { msg -> late.add((msg as? Note)?.text) }
                bus.send(Note("y"))
            }
        }

        bus.send(Note("x"))

        late shouldBe listOf(null, "y")
    }
})
