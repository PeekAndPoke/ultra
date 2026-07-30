package io.peekandpoke.funktor.messaging.senders

import io.peekandpoke.funktor.messaging.Email
import io.peekandpoke.funktor.messaging.EmailSender
import io.peekandpoke.funktor.messaging.api.EmailResult

/**
 * An [EmailSender] that delivers nothing and records everything into [CapturedEmails].
 *
 * `funktorMessaging` installs one of these instead of the app's provider whenever the app runs in
 * test mode, so tests capture by default and nothing needs to opt in.
 *
 * It holds NO state of its own — see [CapturedEmails] for why that matters. This class may therefore
 * be constructed as freely as the composition requires; only the store it writes to has to be a
 * shared singleton.
 */
class CapturingEmailSender(private val captured: CapturedEmails) : EmailSender {

    override suspend fun send(email: Email): EmailResult {
        // The id comes from the STORE, not from a field on this object: the composed chain is
        // rebuilt per kontainer, so a per-instance counter would restart at 1 for every request and
        // hand two unrelated mails the same id — which is what `StoringEmailHook` persists.
        val id = captured.record(email)

        return EmailResult(
            success = true,
            messageId = "captured-$id",
            attributes = mapOf(
                "handledBy" to "CapturingEmailSender"
            )
        )
    }
}

/** `[^"]*` rather than a lazy `.*?`, which does not match across a newline in a wrapped attribute. */
private val hrefRegex = "href=\"([^\"]*)\"".toRegex()

/** Exactly the four entities `kotlinx.html` emits when escaping — see its `escapeMap`. */
private val htmlEntityRegex = "&(amp|lt|gt|quot);".toRegex()

/**
 * Decodes the handful of entities `kotlinx.html` writes when escaping an attribute value.
 *
 * Single-pass on purpose: replacing `&amp;` in a separate pass from the others would decode
 * `&amp;lt;` into `<`, turning escaped text into markup.
 *
 * Unknown entities are left ALONE rather than folded into a default branch. A catch-all `else` here
 * would mean that widening the regex by one alternative silently mistranslates it — add `nbsp` and
 * every `&nbsp;` becomes whatever the default happens to be, with no compile error and no red test.
 */
private fun String.decodeHtmlEntities(): String = htmlEntityRegex.replace(this) {
    when (it.groupValues[1]) {
        "amp" -> "&"
        "lt" -> "<"
        "gt" -> ">"
        "quot" -> "\""
        else -> it.value
    }
}

/**
 * All `href` targets in the mail body, in document order, with HTML escaping undone.
 *
 * The undo matters: a link built by `buildUri` separates its query parameters with `&`, which
 * `kotlinx.html` writes into the attribute as `&amp;`. Reading the raw attribute would hand back a
 * URL whose second parameter is named `amp;token`.
 */
fun Email.hrefs(): List<String> = hrefRegex.findAll(body.content)
    .map { it.groupValues[1].decodeHtmlEntities() }
    .toList()
