package io.peekandpoke.funktor.messaging.senders

import io.peekandpoke.funktor.messaging.EmailSender

/**
 * The app's mail provider, held behind a type that is deliberately NOT an [EmailSender].
 *
 * Two things follow from that, and both are the point:
 *
 * 1. **`EmailSender` is not a kontainer service at all.** Nothing can inject one, so nothing can get
 *    hold of an undecorated provider — no dev overrides, no test-mode capture, no storing hook. The
 *    only way to send is `Mailing`, which is where the module composes the chain. If the provider
 *    were registered under `EmailSender`, injecting that obvious-looking type would quietly bypass
 *    every guarantee the module makes, and in a test would reach the real provider.
 * 2. Registering it under its own type as well as `EmailSender` was never an option anyway: kontainer
 *    resolves by assignability, so two registrations make every `EmailSender` parameter ambiguous.
 *
 * The provider is built **at most once per blueprint** and only on first use — `AwsSesSender.of(...)`
 * opens an SDK client, and the composed chain is rebuilt per request, so constructing it eagerly or
 * per-composition would open one client per HTTP request.
 */
class ConfiguredEmailSender internal constructor(provider: () -> EmailSender) {
    val sender: EmailSender by lazy(provider)
}
