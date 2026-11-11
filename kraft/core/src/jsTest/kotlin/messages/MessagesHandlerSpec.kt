package io.peekandpoke.kraft.messages

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

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

    "a new subscriber is told the last message" {
        val bus = MessagesHandler()
        bus.send(Note("x"))

        var told: String? = null
        bus.stream.subscribeToStream { told = (it as? Note)?.text }

        told shouldBe "x"
    }
})
