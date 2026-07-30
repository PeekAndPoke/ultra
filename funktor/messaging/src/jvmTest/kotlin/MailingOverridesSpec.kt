package io.peekandpoke.funktor.messaging

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.string.shouldStartWith
import io.peekandpoke.funktor.core.config.AppConfig
import io.peekandpoke.funktor.core.config.ktor.KtorConfig
import io.peekandpoke.funktor.messaging.api.EmailBody
import io.peekandpoke.funktor.messaging.api.EmailDestination

/**
 * The dev banner had no test at all, and two defects that both made a dev-box mail look exactly like
 * a production one — the failure mode a dev banner exists to prevent.
 */
class MailingOverridesSpec : StringSpec({

    fun mail(body: EmailBody) = Email(
        source = "sender@corp.test",
        destination = EmailDestination.to("user@corp.test"),
        subject = "Subject",
        body = body,
    )

    val devConfig = MailingDevConfig(destination = "dev-inbox@corp.test")

    val config: AppConfig = AppConfig.of(
        ktor = KtorConfig(deployment = KtorConfig.Deployment(environment = "dev"))
    )

    fun developmentMode(email: Email): Email =
        MailingOverrides.build { developmentMode(config, devConfig) }(email)

    "the banner is inserted into a <body> tag that carries ATTRIBUTES" {
        // The bug this pins: matching the literal "<body>" silently did nothing for a template
        // written as `body { style = "..." }` — which is routine in mail, where inline styles are
        // the only styling that survives. The banner simply never appeared.
        val subject = developmentMode(
            mail(EmailBody.Html("""<html><body style="margin:0"><p>Hello</p></body></html>"""))
        )

        val content = (subject.body as EmailBody.Html).content

        content shouldContain "DEV"
        content shouldStartWith """<html><body style="margin:0"><div>"""
    }

    "the banner is inserted into a plain <body> tag too" {
        val subject = developmentMode(mail(EmailBody.Html("<html><body><p>Hello</p></body></html>")))

        (subject.body as EmailBody.Html).content shouldStartWith "<html><body><div>"
    }

    "a body-less HTML fragment still gets the banner rather than silently losing it" {
        val subject = developmentMode(mail(EmailBody.Html("<p>Hello</p>")))

        (subject.body as EmailBody.Html).content shouldStartWith "<div>"
    }

    "a TEXT body gets a text banner, not raw markup" {
        // Prepending the HTML banner to a plain-text mail showed the user a literal
        // `<div style="background-color: #ff0000...">`.
        val subject = developmentMode(mail(EmailBody.Text("Hello")))

        val content = (subject.body as EmailBody.Text).content

        content shouldContain "DEV"
        content shouldNotContain "<div"
        content shouldContain "Hello"
    }

    "the subject is prefixed and the destination redirected" {
        val subject = developmentMode(mail(EmailBody.Text("Hello")))

        subject.subject shouldBe "[Application | dev]Subject"
        subject.destination.toAddresses shouldBe listOf("dev-inbox@corp.test")
    }

    "no dev config means no overrides at all" {
        val original = mail(EmailBody.Text("Hello"))

        val subject = MailingOverrides.build { developmentMode(config, null) }(original)

        subject shouldBe original
    }
})
