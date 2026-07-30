package io.peekandpoke.ultra.vault

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.ultra.log.Log
import io.peekandpoke.ultra.log.LogLevel
import io.peekandpoke.ultra.log.NullLog
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class VaultHookScopeSpec : StringSpec({

    class RecordingLog : Log {
        val messages = mutableListOf<String>()

        override fun log(level: LogLevel, message: String, error: Throwable?) {
            // the error arrives alongside the message now, rather than folded into it
            val rendered = "$level: $message" + (error?.let { " <${it::class.simpleName}>" } ?: "")
            synchronized(messages) { messages.add(rendered) }
        }

        fun snapshot(): List<String> = synchronized(messages) { messages.toList() }
    }

    // Inline //////////////////////////////////////////////////////////////////////////////////////

    "Inline awaits the hook before returning" {
        var ran = false

        VaultHookScope.Inline().runHook("test") { ran = true }

        ran shouldBe true
    }

    "Inline contains and logs hook exceptions instead of failing the repository call" {
        val log = RecordingLog()

        val thrown = runCatching {
            VaultHookScope.Inline(log).runHook("failing-hook") { error("hook blew up") }
        }.exceptionOrNull()

        // The write has already been committed at this point, so failing the caller would report
        // a failure for an operation that actually succeeded
        thrown shouldBe null
        log.snapshot().single().contains("failing-hook") shouldBe true
    }

    "Inline contains a CancellationException the hook raised itself" {
        val log = RecordingLog()

        val thrown = runCatching {
            VaultHookScope.Inline(log).runHook("test") { throw CancellationException("hook gave up") }
        }.exceptionOrNull()

        // Nothing cancelled the caller, so this is the hook failing — an after-save hook must not
        // fail a write that already committed, whatever exception type it picks
        thrown shouldBe null
        log.snapshot().single().contains("test") shouldBe true
    }

    "Inline contains a hook that times itself out" {
        val log = RecordingLog()

        val thrown = runCatching {
            VaultHookScope.Inline(log).runHook("test") {
                withTimeout(20.milliseconds) { delay(10.seconds) }
            }
        }.exceptionOrNull()

        // withTimeout raises a CancellationException, but only the hook's own scope was cancelled
        thrown shouldBe null
    }

    "Inline still propagates cancellation of the surrounding coroutine" {
        val log = RecordingLog()
        val scope = CoroutineScope(SupervisorJob())
        val entered = CompletableDeferred<Unit>()
        var escaped: Throwable? = null

        val job = scope.launch {
            try {
                VaultHookScope.Inline(log).runHook("test") {
                    entered.complete(Unit)
                    delay(10.seconds)
                }
            } catch (e: Throwable) {
                escaped = e
            }
        }

        entered.await()
        job.cancelAndJoin()

        // Containing this would break cooperative cancellation of the caller
        (escaped is CancellationException) shouldBe true
    }

    // Deferred, before binding ////////////////////////////////////////////////////////////////////

    "Deferred runs hooks inline while no scope is bound" {
        var ran = false

        DeferredVaultHookScope().runHook("test") { ran = true }

        // Nothing is silently dropped during startup, in CLI commands or in fixtures
        ran shouldBe true
    }

    "Deferred runs hooks inline again once the bound scope is no longer active" {
        val job = SupervisorJob()
        val subject = DeferredVaultHookScope().apply { bind(CoroutineScope(job), NullLog) }

        job.cancelAndJoin()

        var ran = false
        // Launching into a cancelled scope would drop the hook without ever logging it
        subject.runHook("test") { ran = true }

        ran shouldBe true
    }

    // Deferred, after binding /////////////////////////////////////////////////////////////////////

    "Deferred returns to the caller before the hook has finished" {
        val job = SupervisorJob()
        val subject = DeferredVaultHookScope().apply { bind(CoroutineScope(job), NullLog) }

        val release = CompletableDeferred<Unit>()
        val started = CompletableDeferred<Unit>()
        var hookFinished = false

        subject.runHook("test") {
            started.complete(Unit)
            release.await()
            hookFinished = true
        }

        // The hook is still parked here, so returning proves the caller did not await it.
        // An inline implementation would have deadlocked on release.await() above.
        withTimeout(5.seconds) { started.await() }
        hookFinished shouldBe false

        release.complete(Unit)
        job.cancelAndJoin()
    }

    "Deferred contains and logs a failing hook instead of failing the scope" {
        val log = RecordingLog()
        val job = SupervisorJob()
        val subject = DeferredVaultHookScope().apply { bind(CoroutineScope(job), log) }

        subject.runHook("failing-hook") { error("hook blew up") }

        withTimeout(5.seconds) {
            while (log.snapshot().isEmpty()) {
                delay(10)
            }
        }

        log.snapshot().single().contains("failing-hook") shouldBe true

        // A failing hook must not take the surrounding scope down with it
        job.isActive shouldBe true

        job.cancelAndJoin()
    }

    "Deferred hooks are actually cancelled when the bound scope is cancelled" {
        val job = SupervisorJob()
        val subject = DeferredVaultHookScope().apply { bind(CoroutineScope(job), NullLog) }

        val started = CompletableDeferred<Unit>()
        val cancelled = CompletableDeferred<Unit>()

        subject.runHook("test") {
            try {
                started.complete(Unit)
                CompletableDeferred<Unit>().await() // never completes on its own
            } catch (e: CancellationException) {
                // Record cancellation positively — asserting "never completed" would also hold
                // for a hook that simply leaked and stayed suspended forever
                cancelled.complete(Unit)
                throw e
            }
        }

        withTimeout(5.seconds) { started.await() }

        job.cancelAndJoin()

        withTimeout(5.seconds) { cancelled.await() }
    }
})
