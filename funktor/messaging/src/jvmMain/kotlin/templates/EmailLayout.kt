package io.peekandpoke.funktor.messaging.templates

import io.peekandpoke.funktor.messaging.api.EmailBody
import io.peekandpoke.ultra.i18n.Locale
import kotlinx.html.BODY
import kotlinx.html.body
import kotlinx.html.head
import kotlinx.html.html
import kotlinx.html.lang
import kotlinx.html.meta
import kotlinx.html.stream.createHTML
import kotlinx.html.style

/**
 * The chrome around an email's content: the document skeleton, and whatever branding an app wraps
 * around it — colours, a header, a frame, a footer.
 *
 * An INSTANCE, not a static helper, because branding is the whole reason an app touches these mails.
 * Wanting your own colours and footer should not force you to reimplement three renderings to get
 * them.
 *
 * **It takes the [Locale] on purpose.** "Chrome is language-independent" stops being true the moment
 * it grows a footer: an unsubscribe line, an imprint or a legal notice has to be translated, and a
 * layout that cannot see the locale can only ever produce a German body under an English footer.
 */
fun interface EmailLayout {

    /** Wraps [content] into the full mail body, in [locale]. */
    fun render(locale: Locale, content: BODY.() -> Unit): EmailBody.Html

    companion object {
        /**
         * The framework default: a minimal, unbranded document.
         *
         * The `<head>` earns its place. Both senders declare UTF-8 at the MIME level, so mainstream
         * clients render `Grüßen` correctly without it — but the declaration is what survives when
         * the MIME envelope does not, i.e. forward-as-inline, "save as HTML", and any tool rendering
         * the PERSISTED body on its own. Without it those all show `GrÃ¼ÃŸen`.
         */
        val default = EmailLayout { locale, content ->
            val html = createHTML().html {
                lang = locale.tag

                head {
                    meta(charset = "utf-8")
                    meta(name = "viewport", content = "width=device-width, initial-scale=1")
                }

                // Inline, because a <style> block is stripped by several webmail clients and Outlook
                // ignores most of it. Not branding — just the difference between a readable mail and
                // Outlook's Times New Roman default at full window width.
                body {
                    style = "font-family:Helvetica,Arial,sans-serif;font-size:16px;" +
                            "line-height:1.5;max-width:600px;margin:0 auto;padding:16px"

                    content()
                }
            }

            // The doctype is what the charset argument above actually depends on: every standalone
            // path (forward-as-inline, "save as HTML", rendering the persisted body) parses in quirks
            // mode without it, which is also where the encoding guess goes wrong.
            EmailBody.Html(content = "<!DOCTYPE html>" + html)
        }
    }
}
