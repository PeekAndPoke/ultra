package io.peekandpoke.funktor.core.lifecycle

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.ktor.server.application.*
import io.ktor.server.testing.*
import io.peekandpoke.funktor.core.lifecycle.AppLifeCycleHooks.*
import io.peekandpoke.ultra.kontainer.kontainer
import java.util.concurrent.atomic.AtomicInteger

// Verifies that hooks registered as plain `singleton(MyHook::class)` — the same way
// EnsureRepositoriesOnAppStarting and AuthSystemAppHooks are wired — are actually picked
// up by the Kontainer's auto-resolution of `List<OnAppStarting>` etc. inside
// `singleton(AppLifeCycleHooks::class)`, and then invoked during the application lifecycle.
class AppLifeCycleKontainerWiringSpec : StringSpec({

    "singleton-registered OnAppStarting hook fires during lifecycle" {
        val hit = AtomicInteger(0)
        val k = kontainer {
            instance(hit)
            singleton(ProbeOnAppStarting::class)
            singleton(AppLifeCycleHooks::class)
        }.create()

        testApplication {
            application { setupLifecycle(k) }
            startApplication()
        }

        hit.get() shouldBe 1
    }

    "singleton-registered OnAppStarted hook fires during lifecycle" {
        val hit = AtomicInteger(0)
        val k = kontainer {
            instance(hit)
            singleton(ProbeOnAppStarted::class)
            singleton(AppLifeCycleHooks::class)
        }.create()

        testApplication {
            application { setupLifecycle(k) }
            startApplication()
        }

        hit.get() shouldBe 1
    }

    "singleton-registered OnAppStopPreparing hook fires during lifecycle" {
        val hit = AtomicInteger(0)
        val k = kontainer {
            instance(hit)
            singleton(ProbeOnAppStopPreparing::class)
            singleton(AppLifeCycleHooks::class)
        }.create()

        testApplication {
            application { setupLifecycle(k) }
            startApplication()
        }

        hit.get() shouldBe 1
    }

    "singleton-registered OnAppStopping hook fires during lifecycle" {
        val hit = AtomicInteger(0)
        val k = kontainer {
            instance(hit)
            singleton(ProbeOnAppStopping::class)
            singleton(AppLifeCycleHooks::class)
        }.create()

        testApplication {
            application { setupLifecycle(k) }
            startApplication()
        }

        hit.get() shouldBe 1
    }

    // NOTE: OnAppStopped doesn't fire reliably inside testApplication — the ktor test harness
    // returns before ApplicationStopped is dispatched. Kept here deliberately as a probe; if it
    // ever starts passing, the test infra caught up with us. If it fails, it does NOT indicate
    // the same regression as OnAppStarting.
    "!singleton-registered OnAppStopped hook fires during lifecycle" {
        val hit = AtomicInteger(0)
        val k = kontainer {
            instance(hit)
            singleton(ProbeOnAppStopped::class)
            singleton(AppLifeCycleHooks::class)
        }.create()

        testApplication {
            application { setupLifecycle(k) }
            startApplication()
        }

        hit.get() shouldBe 1
    }

    "!all five phase hooks registered as concrete-class singletons fire exactly once" {
        val starting = AtomicInteger(0)
        val started = AtomicInteger(0)
        val stopPreparing = AtomicInteger(0)
        val stopping = AtomicInteger(0)
        val stopped = AtomicInteger(0)

        val k = kontainer {
            instance(StartingCounter(starting))
            instance(StartedCounter(started))
            instance(StopPreparingCounter(stopPreparing))
            instance(StoppingCounter(stopping))
            instance(StoppedCounter(stopped))
            singleton(ProbeAllStarting::class)
            singleton(ProbeAllStarted::class)
            singleton(ProbeAllStopPreparing::class)
            singleton(ProbeAllStopping::class)
            singleton(ProbeAllStopped::class)
            singleton(AppLifeCycleHooks::class)
        }.create()

        testApplication {
            application { setupLifecycle(k) }
            startApplication()
        }

        starting.get() shouldBe 1
        started.get() shouldBe 1
        stopPreparing.get() shouldBe 1
        stopping.get() shouldBe 1
        stopped.get() shouldBe 1
    }

    "setupLifecycle + lifeCycle { } both fire once and do not clobber each other" {
        val kontainerHit = AtomicInteger(0)
        val builderHit = AtomicInteger(0)

        val k = kontainer {
            instance(kontainerHit)
            singleton(ProbeOnAppStarting::class)
            singleton(ProbeOnAppStarted::class)
            singleton(AppLifeCycleHooks::class)
        }.create()

        testApplication {
            application {
                setupLifecycle(k)
                lifeCycle {
                    onAppStarting { builderHit.incrementAndGet() }
                    onAppStarted { builderHit.incrementAndGet() }
                }
            }
            startApplication()
        }

        kontainerHit.get() shouldBe 2
        builderHit.get() shouldBe 2
    }

    "two lifeCycle { } calls accumulate listeners without wiping earlier subscriptions" {
        val firstStarted = AtomicInteger(0)
        val secondStarted = AtomicInteger(0)

        testApplication {
            application {
                lifeCycle { onAppStarted { firstStarted.incrementAndGet() } }
                lifeCycle { onAppStarted { secondStarted.incrementAndGet() } }
            }
            startApplication()
        }

        firstStarted.get() shouldBe 1
        secondStarted.get() shouldBe 1
    }
})

class ProbeOnAppStarting(private val hit: AtomicInteger) : OnAppStarting {
    override suspend fun onAppStarting(application: Application) {
        hit.incrementAndGet()
    }
}

class ProbeOnAppStarted(private val hit: AtomicInteger) : OnAppStarted {
    override suspend fun onAppStarted(application: Application) {
        hit.incrementAndGet()
    }
}

class ProbeOnAppStopPreparing(private val hit: AtomicInteger) : OnAppStopPreparing {
    override suspend fun onAppStopPreparing(env: ApplicationEnvironment) {
        hit.incrementAndGet()
    }
}

class ProbeOnAppStopping(private val hit: AtomicInteger) : OnAppStopping {
    override suspend fun onAppStopping(application: Application) {
        hit.incrementAndGet()
    }
}

class ProbeOnAppStopped(private val hit: AtomicInteger) : OnAppStopped {
    override suspend fun onAppStopped(application: Application) {
        hit.incrementAndGet()
    }
}

// Distinct wrapper types so the "all five phases in one kontainer" test can inject a
// different counter into each probe without AtomicInteger collisions.
class StartingCounter(val v: AtomicInteger)
class StartedCounter(val v: AtomicInteger)
class StopPreparingCounter(val v: AtomicInteger)
class StoppingCounter(val v: AtomicInteger)
class StoppedCounter(val v: AtomicInteger)

class ProbeAllStarting(private val c: StartingCounter) : OnAppStarting {
    override suspend fun onAppStarting(application: Application) {
        c.v.incrementAndGet()
    }
}

class ProbeAllStarted(private val c: StartedCounter) : OnAppStarted {
    override suspend fun onAppStarted(application: Application) {
        c.v.incrementAndGet()
    }
}

class ProbeAllStopPreparing(private val c: StopPreparingCounter) : OnAppStopPreparing {
    override suspend fun onAppStopPreparing(env: ApplicationEnvironment) {
        c.v.incrementAndGet()
    }
}

class ProbeAllStopping(private val c: StoppingCounter) : OnAppStopping {
    override suspend fun onAppStopping(application: Application) {
        c.v.incrementAndGet()
    }
}

class ProbeAllStopped(private val c: StoppedCounter) : OnAppStopped {
    override suspend fun onAppStopped(application: Application) {
        c.v.incrementAndGet()
    }
}
