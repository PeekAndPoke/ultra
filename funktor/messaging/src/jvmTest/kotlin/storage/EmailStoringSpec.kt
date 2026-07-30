package io.peekandpoke.funktor.messaging.storage

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.peekandpoke.funktor.messaging.storage.EmailStoring.WithContent

/**
 * `withAnonymizedContent` is the control that keeps a live token out of the database — the persisted
 * copy of a password-reset or activation mail is readable by anyone with access to the sent-messages
 * inspector. It had no tests at all, which is how the plain-text hole below survived.
 */
class EmailStoringSpec : StringSpec({

    fun anonymize(content: String): String =
        (EmailStoring.withAnonymizedContent() as WithContent).modifyContent(content)

    "anonymizes the href of an HTML link" {
        val result = anonymize(
            """<p><a href="https://app.test/auth/email-password/reset-password/S3cr3t%2BT0ken">Reset</a></p>"""
        )

        result shouldNotContain "S3cr3t"
        result shouldContain "#anonymized"
    }

    "anonymizes a BARE url — the case a plain-text mail is made of" {
        // `EmailStoring.WithContent.modifyContent` is applied to `EmailBody.Text` too, where there is
        // no href to match. Before this, a plain-text reset mail persisted its token verbatim.
        val result = anonymize(
            "Open https://app.test/auth/email-password/reset-password/S3cr3t%2BT0ken to continue."
        )

        result shouldNotContain "S3cr3t"
        result shouldBe "Open #anonymized to continue."
    }

    "anonymizes a url printed as TEXT next to the link, for copy-paste" {
        // The routine shape for a mail carrying a token, and the one that defeats an href-only rule.
        val result = anonymize(
            """
            <p><a href="https://app.test/r/S3cr3t">Reset</a></p>
            <p>Or paste this: https://app.test/r/S3cr3t</p>
            """.trimIndent()
        )

        result shouldNotContain "S3cr3t"
    }

    "does not swallow the markup that follows a bare url" {
        val result = anonymize("<p>https://app.test/r/tok</p><p>next</p>")

        result shouldBe "<p>#anonymized</p><p>next</p>"
    }

    "leaves content without links untouched" {
        val content = "Your password was changed. If this was not you, please contact us!"

        anonymize(content) shouldBe content
    }

    "an href spanning a newline is still anonymized" {
        // The previous lazy `.*?` did not match across a newline, so a wrapped attribute survived.
        val result = anonymize("<a href=\"https://app.test/r/\nS3cr3t\">Reset</a>")

        result shouldNotContain "S3cr3t"
    }

    "anonymizes an UPPERCASE scheme — the regexes are case-insensitive" {
        anonymize("Open HTTPS://app.test/r/S3cr3t now") shouldNotContain "S3cr3t"
        anonymize("""<a HREF="https://app.test/r/S3cr3t">Reset</a>""") shouldNotContain "S3cr3t"
    }

    "anonymizes a protocol-relative link and a custom scheme" {
        // A token does not become safe by arriving under a different scheme. Mobile apps routinely
        // send deep links, and protocol-relative URLs are common in HTML mail.
        anonymize("Open //app.test/r/S3cr3t") shouldNotContain "S3cr3t"
        anonymize("Open myapp://activate/S3cr3t") shouldNotContain "S3cr3t"
    }

    "withContent does NOT anonymize — the two policies must stay distinguishable" {
        val content = """<a href="https://app.test/r/S3cr3t">Reset</a>"""

        (EmailStoring.withContent() as WithContent).modifyContent(content) shouldBe content
    }
})
