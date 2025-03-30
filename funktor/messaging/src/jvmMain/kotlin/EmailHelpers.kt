package de.peekandpoke.ktorfx.messaging

import de.peekandpoke.ktorfx.messaging.api.EmailBody
import kotlinx.html.HTML

/**
 * An [EmailHelpers] produces and [Email]
 */
interface EmailHelpers {

    fun textEmailBody(block: () -> String): EmailBody.Text {
        return EmailBody.Text(
            content = block()
        )
    }

    fun htmlEmailBody(block: HTML.() -> Unit): EmailBody.Html {
        return EmailBody.Html(block)
    }
}
