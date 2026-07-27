package io.peekandpoke.funktor.messaging.senders

import io.peekandpoke.funktor.messaging.Email

/**
 * Everything the app "sent" while running in test mode — the oracle for email-driven tests.
 *
 * In test mode `funktorMessaging` swaps the app's provider for a [CapturingEmailSender] writing here,
 * so a test can assert that a mail was sent AT ALL, to whom, and — the part that matters — follow the
 * token inside it, exactly as a user clicking the link would. Nothing registers a sender for this; it
 * is simply what test mode does.
 *
 * The persisted copy of a mail cannot serve as that oracle: `EmailStoring.withAnonymizedContent`,
 * which every auth mail uses, strips every link before storing. That is the right thing to keep in a
 * database and useless as a test oracle, so capture happens at the sender, before storage and before
 * any provider is involved.
 *
 * **This class must never gain a constructor dependency.** That is what keeps it a GLOBAL singleton:
 * kontainer demotes a singleton to `SemiDynamic` — one instance per kontainer, i.e. per request — as
 * soon as it transitively injects a dynamic service. Holding the state here rather than on the sender
 * is precisely what makes the sender's own scope irrelevant: however many sender instances get built,
 * they all write to this one object, which is the one a test can reach.
 */
class CapturedEmails {

    companion object {
        /**
         * Drop-oldest bound, so a suite that never calls [clear] cannot accumulate without limit.
         *
         * Not a licence to enable capture in a long-lived process: what this retains is full mail
         * bodies, which for auth mail means live password-reset tokens sitting in heap — precisely
         * the material `EmailStoring.withAnonymizedContent` scrubs before anything is persisted.
         */
        const val MAX_CAPTURED: Int = 1000
    }

    private val lock = Any()
    private val mails = ArrayDeque<Email>()
    private var counter = 0

    /** Snapshot of the captured mails, oldest first. */
    val captured: List<Email> get() = synchronized(lock) { mails.toList() }

    /**
      * Records [email] and returns its sequence number. Called from whatever thread ktor is handling
      * a request on, so the counter lives under the same lock as the deque.
      */
    fun record(email: Email): Int = synchronized(lock) {
        mails.addLast(email)
        while (mails.size > MAX_CAPTURED) {
            mails.removeFirst()
        }
        ++counter
    }

    /** Forgets everything. One instance is shared by a whole spec, so call this before asserting. */
    fun clear() {
        synchronized(lock) {
            mails.clear()
        }
    }

    /** All captured mails with [address] among their to/cc/bcc recipients, oldest first. */
    fun capturedTo(address: String): List<Email> = captured.filter {
        address in (it.destination.toAddresses + it.destination.ccAddresses + it.destination.bccAddresses)
    }

    /** The most recently captured mail to [address], or `null` if there is none. */
    fun lastTo(address: String): Email? = capturedTo(address).lastOrNull()
}
