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

        val told = mutableListOf<String>()
        bus.stream.subscribeToStream { (it as? Note)?.let { note -> told.add(note.text) } }

        told shouldBe emptyList()

        bus.send(Note("y"))

        told shouldBe listOf("y")
    }

    "a handler that throws while subscribing is not kept" {
        val bus = MessagesHandler()
        var calls = 0

        runCatching {
            bus.stream.subscribeToStream { calls++; if (it == null) error("boom") }
        }

        bus.send(Note("y"))

        calls shouldBe 1
    }
})
