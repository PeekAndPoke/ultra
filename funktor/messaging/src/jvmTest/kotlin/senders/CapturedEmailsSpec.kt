package io.peekandpoke.funktor.messaging.senders

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.peekandpoke.funktor.messaging.Email
import io.peekandpoke.funktor.messaging.api.EmailBody
import io.peekandpoke.funktor.messaging.api.EmailDestination
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.html.a
import kotlinx.html.body
import kotlinx.html.p

class CapturedEmailsSpec : StringSpec({

    fun mail(
        to: String = "user@example.com",
        subject: String = "Subject",
        body: EmailBody = EmailBody.Text("body"),
    ) = Email(
        source = "sender@example.com",
        destination = EmailDestination.to(to),
        subject = subject,
        body = body,
    )

    "the sender records into the store, in order, and reports success" {
        val store = CapturedEmails()
        val subject = CapturingEmailSender(store)

        val result = subject.send(mail(subject = "first"))
        subject.send(mail(subject = "second"))

        result.success shouldBe true
        result.attributes["handledBy"] shouldBe "CapturingEmailSender"

        store.captured.map { it.subject } shouldContainExactly listOf("first", "second")
    }

    "SEPARATE senders sharing one store all record into it" {
        // The whole point of holding the state in the service rather than on the sender. Kontainer
        // rebuilds the composed chain per request, so a test only ever sees a request's mail because
        // every one of those chains writes to the same store.
        val store = CapturedEmails()

        CapturingEmailSender(store).send(mail(subject = "from-sender-a"))
        CapturingEmailSender(store).send(mail(subject = "from-sender-b"))

        store.captured.map { it.subject } shouldContainExactly listOf("from-sender-a", "from-sender-b")
    }

    "clear forgets everything — one store is shared by a whole spec" {
        val store = CapturedEmails()
        CapturingEmailSender(store).send(mail())

        store.clear()

        store.captured shouldBe emptyList()
    }

    "drops the oldest once MAX_CAPTURED is reached, rather than growing without bound" {
        val store = CapturedEmails()
        val sender = CapturingEmailSender(store)

        repeat(CapturedEmails.MAX_CAPTURED + 5) { sender.send(mail(subject = "mail-$it")) }

        store.captured.size shouldBe CapturedEmails.MAX_CAPTURED
        store.captured.first().subject shouldBe "mail-5"
        store.captured.last().subject shouldBe "mail-${CapturedEmails.MAX_CAPTURED + 4}"
    }

    "concurrent recording must lose nothing — the locking is the point of the class" {
        // One store is written to from whatever thread ktor handles a request on. Every other test
        // here is single-threaded and stays green with the synchronization removed, which would
        // leave the one property the class exists to guarantee unpinned. Unsynchronized
        // `ArrayDeque.addLast` interleaves its size and backing-array writes, so a concurrent pair
        // can lose a mail or throw out of the growth path — surfacing as an intermittent
        // `lastTo(...) == null`, i.e. "the reset mail was not sent".
        val store = CapturedEmails()
        val count = 200

        coroutineScope {
            repeat(count) { i ->
                launch(Dispatchers.Default) { CapturingEmailSender(store).send(mail(subject = "concurrent-$i")) }
            }
        }

        store.captured.size shouldBe count
        store.captured.map { it.subject }.toSet().size shouldBe count
    }

    "capturedTo and lastTo look at all recipient fields" {
        val store = CapturedEmails()
        val sender = CapturingEmailSender(store)

        sender.send(mail(to = "a@example.com", subject = "to-a"))
        sender.send(mail(to = "b@example.com", subject = "to-b"))
        sender.send(mail(to = "a@example.com", subject = "to-a-again"))
        sender.send(
            mail(subject = "cc-and-bcc").copy(
                destination = EmailDestination(
                    toAddresses = listOf("x@example.com"),
                    ccAddresses = listOf("c@example.com"),
                    bccAddresses = listOf("d@example.com"),
                )
            )
        )

        store.capturedTo("a@example.com").map { it.subject } shouldContainExactly listOf("to-a", "to-a-again")
        store.lastTo("a@example.com")?.subject shouldBe "to-a-again"
        store.lastTo("c@example.com")?.subject shouldBe "cc-and-bcc"
        store.lastTo("d@example.com")?.subject shouldBe "cc-and-bcc"
        store.lastTo("nobody@example.com").shouldBeNull()
    }

    "hrefs undoes the escaping kotlinx.html applies to an attribute value" {
        // This is the whole reason `hrefs` exists rather than a bare regex at the call site. A link
        // built by `buildUri` separates query parameters with '&', which kotlinx.html writes as
        // '&amp;'. Reading the raw attribute hands back a URL whose second parameter is 'amp;token'.
        val subject = mail(
            body = EmailBody.Html {
                body {
                    p {
                        a(href = "https://example.com/reset?provider=email-password&token=abc%2Bdef") {
                            +"Recover account"
                        }
                    }
                }
            }
        )

        subject.hrefs() shouldContainExactly listOf(
            "https://example.com/reset?provider=email-password&token=abc%2Bdef"
        )
    }

    "hrefs returns every link in document order, and nothing for a body without links" {
        val withLinks = mail(
            body = EmailBody.Html {
                body {
                    a(href = "https://one.example.com") { +"one" }
                    a(href = "https://two.example.com") { +"two" }
                }
            }
        )

        withLinks.hrefs() shouldContainExactly listOf("https://one.example.com", "https://two.example.com")

        mail().hrefs() shouldBe emptyList()
    }
})
