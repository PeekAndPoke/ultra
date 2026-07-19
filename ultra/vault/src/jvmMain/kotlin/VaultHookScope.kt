package io.peekandpoke.ultra.vault

import io.peekandpoke.ultra.log.Log
import io.peekandpoke.ultra.log.LogLevel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Decides how repository after-save / after-delete hooks are executed.
 *
 * Hooks themselves are plain `suspend` functions. This service only decides whether the caller
 * awaits them ([Inline]) or whether they are launched and the caller returns immediately
 * ([DeferredVaultHookScope]).
 *
 * Both implementations treat hook failures the same way: they are contained and logged, never
 * propagated. After-save hooks run when the write has already been committed, so failing the
 * repository call would report a failure for an operation that actually succeeded, and a deferred
 * hook has no caller left to propagate to. Hooks are therefore responsible for handling their own
 * errors.
 */
interface VaultHookScope {

    /**
     * Runs [block], which describes itself as [description] for error reporting.
     *
     * Implementations may await [block] or launch it, so callers must not rely on [block] having
     * completed when this function returns. Failures never reach the caller — see the note on
     * [VaultHookScope].
     */
    suspend fun runHook(description: String, block: suspend () -> Unit)

    /**
     * Awaits the hooks, so the repository operation does not return until they are done.
     *
     * This is the default for directly constructed drivers, and keeps tests deterministic.
     */
    class Inline(private val log: Log = FallbackLog) : VaultHookScope {
        override suspend fun runHook(description: String, block: suspend () -> Unit) {
            runHookContained(description = description, log = log, block = block)
        }
    }
}

/**
 * Runs hooks without blocking the repository operation that triggered them.
 *
 * The scope is bound late via [bind], because the Ktor `Application` this is rooted in does not
 * exist yet when the kontainer is built. Until [bind] is called, hooks run inline, so nothing is
 * silently dropped during startup, in CLI commands or in fixtures — none of which ever reach the
 * application lifecycle.
 *
 * IMPORTANT: this must stay free of constructor dependencies. Depending on a service that is
 * (transitively) dynamic makes the kontainer treat this as semi-dynamic, which yields one instance
 * per kontainer — and since every request builds its own kontainer, [bind] would then never be
 * visible to any request. That is why [Log] is supplied to [bind] instead of being injected.
 *
 * For work that must survive a restart prefer a durable queue such as funktor's `BackgroundJobs`:
 * hooks launched here are cancelled when the application shuts down.
 */
class DeferredVaultHookScope : VaultHookScope {

    @Volatile
    private var scope: CoroutineScope? = null

    @Volatile
    private var log: Log = FallbackLog

    /**
     * Binds the [scope] that hooks are launched in, typically the Ktor `Application`, and the
     * [log] that hook failures are reported to.
     *
     * Rooting in the application's scope means in-flight hooks are cancelled on shutdown instead
     * of outliving the process teardown.
     */
    fun bind(scope: CoroutineScope, log: Log) {
        // The log is published first, so a reader that observes the scope also observes the log
        this.log = log
        this.scope = scope
    }

    override suspend fun runHook(description: String, block: suspend () -> Unit) {
        val bound = scope

        // Launching into a cancelled scope would produce an already-cancelled coroutine, so the
        // hook would never run and nothing would be logged. Running inline is the lesser evil.
        if (bound == null || !bound.isActive) {
            runHookContained(description = description, log = log, block = block)
            return
        }

        // No Job is passed here on purpose: an explicit Job in the context would replace the
        // parent and detach the hook from the application's job tree, forfeiting cancellation on
        // shutdown. Failures are contained by runHookContained instead of by a SupervisorJob.
        bound.launch(Dispatchers.IO) {
            runHookContained(description = description, log = log, block = block)
        }
    }
}

/**
 * Runs [block] so that no failure escapes to the caller, reporting problems to [log].
 */
internal suspend fun runHookContained(description: String, log: Log, block: suspend () -> Unit) {
    try {
        block()
    } catch (e: CancellationException) {
        // Typically an application shutdown. Logged so the dropped work is observable instead of
        // vanishing silently, then rethrown to keep cancellation cooperative.
        log.warning("Vault hook '$description' was cancelled before it completed")
        throw e
    } catch (e: Throwable) {
        log.error("Vault hook '$description' failed", e)
    }
}

/**
 * Used when no [Log] has been supplied, i.e. for directly constructed drivers and in tests.
 *
 * Deliberately not [io.peekandpoke.ultra.log.NullLog]: a hook failure that is neither propagated
 * nor logged would be invisible, which is the exact problem this whole area set out to fix.
 */
internal object FallbackLog : Log {
    override fun log(level: LogLevel, message: String) {
        System.err.println("[$level] $message")
    }
}
