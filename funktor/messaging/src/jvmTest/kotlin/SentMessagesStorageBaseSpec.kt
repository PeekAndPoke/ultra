package io.peekandpoke.funktor.messaging

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.peekandpoke.funktor.messaging.api.EmailBody
import io.peekandpoke.funktor.messaging.api.EmailDestination
import io.peekandpoke.funktor.messaging.api.EmailResult
import io.peekandpoke.funktor.messaging.api.SentMessageModel
import io.peekandpoke.funktor.messaging.storage.SentMessagesStorage
import io.peekandpoke.ultra.kontainer.Kontainer
import io.peekandpoke.ultra.kontainer.KontainerBuilder
import io.peekandpoke.ultra.model.PagedSearchFilter
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

abstract class SentMessagesStorageBaseSpec : FreeSpec() {

    abstract fun KontainerBuilder.configureKontainer()

    abstract fun FunktorMessagingBuilder.configureMessaging()

    suspend fun createKontainer(): Kontainer {
        return createMessagingTestContainer(
            configureKontainer = { configureKontainer() },
            configureMessaging = { configureMessaging() },
        )
    }

    private fun emailContent(
        subject: String,
        to: List<String>,
        source: String = "test@example.com",
    ) = SentMessageModel.Content.EmailContent(
        subject = subject,
        destination = EmailDestination(toAddresses = to),
        source = source,
        body = EmailBody.Text(content = "Test body for $subject"),
        result = EmailResult.ofMessageId("msg-$subject"),
    )

    init {
        "Storing and retrieving a sent email" {
            val kontainer = createKontainer()
            val storage = kontainer.get(SentMessagesStorage::class)
            storage.clear()

            val content = emailContent(subject = "Hello", to = listOf("user@example.com"))

            storage.storeSentEmail(
                result = EmailResult.ofMessageId("msg-1"),
                refs = setOf("ref-1"),
                tags = setOf("tag-1"),
                content = content,
                attachments = emptyList(),
            )

            val found = storage.findByRefs(refs = setOf("ref-1"))
            val items = found.toList()

            items shouldHaveSize 1
            items[0].value.refs shouldBe setOf("ref-1")
            items[0].value.tags shouldBe setOf("tag-1")
            items[0].value.content shouldBe content
        }

        "Finding sent messages by ref" {
            val kontainer = createKontainer()
            val storage = kontainer.get(SentMessagesStorage::class)
            storage.clear()

            storage.storeSentEmail(
                result = EmailResult.ofMessageId("msg-1"),
                refs = setOf("ref-A"),
                tags = emptySet(),
                content = emailContent(subject = "Email A", to = listOf("a@example.com")),
                attachments = emptyList(),
            )

            delay(1.milliseconds)

            storage.storeSentEmail(
                result = EmailResult.ofMessageId("msg-2"),
                refs = setOf("ref-B"),
                tags = emptySet(),
                content = emailContent(subject = "Email B", to = listOf("b@example.com")),
                attachments = emptyList(),
            )

            delay(1.milliseconds)

            storage.storeSentEmail(
                result = EmailResult.ofMessageId("msg-3"),
                refs = setOf("ref-A", "ref-C"),
                tags = emptySet(),
                content = emailContent(subject = "Email C", to = listOf("c@example.com")),
                attachments = emptyList(),
            )

            val foundA = storage.findByRefs(refs = setOf("ref-A")).toList()
            foundA shouldHaveSize 2

            val foundB = storage.findByRefs(refs = setOf("ref-B")).toList()
            foundB shouldHaveSize 1

            val foundC = storage.findByRefs(refs = setOf("ref-C")).toList()
            foundC shouldHaveSize 1

            val foundAorB = storage.findByRefs(refs = setOf("ref-A", "ref-B")).toList()
            foundAorB shouldHaveSize 3

            val foundNone = storage.findByRefs(refs = setOf("ref-UNKNOWN")).toList()
            foundNone shouldHaveSize 0

            val foundEmpty = storage.findByRefs(refs = emptySet()).toList()
            foundEmpty shouldHaveSize 0
        }

        "Finding sent messages with pagination" {
            val kontainer = createKontainer()
            val storage = kontainer.get(SentMessagesStorage::class)
            storage.clear()

            // Insert 5 messages
            repeat(5) { i ->
                storage.storeSentEmail(
                    result = EmailResult.ofMessageId("msg-$i"),
                    refs = setOf("ref-page"),
                    tags = emptySet(),
                    content = emailContent(subject = "Email $i", to = listOf("page@example.com")),
                    attachments = emptyList(),
                )
                delay(2.milliseconds)
            }

            val page1 = storage.findByRefs(
                refs = setOf("ref-page"),
                filter = PagedSearchFilter(search = "", page = 1, epp = 2),
            ).toList()
            page1 shouldHaveSize 2

            val page2 = storage.findByRefs(
                refs = setOf("ref-page"),
                filter = PagedSearchFilter(search = "", page = 2, epp = 2),
            ).toList()
            page2 shouldHaveSize 2

            val page3 = storage.findByRefs(
                refs = setOf("ref-page"),
                filter = PagedSearchFilter(search = "", page = 3, epp = 2),
            ).toList()
            page3 shouldHaveSize 1
        }
    }
}
