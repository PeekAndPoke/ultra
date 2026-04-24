package io.peekandpoke.funktor.core.lifecycle

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.ktor.server.application.*
import io.ktor.server.testing.*
import io.peekandpoke.funktor.core.lifecycle.AppLifeCycleHooks.*
import io.peekandpoke.ultra.kontainer.kontainer
import kotlinx.coroutines.delay
import kotlinx.coroutines.yield
import java.util.concurrent.atomic.AtomicInteger

class AppLifeCycleSpec : StringSpec({

    "onAppStarting DSL runs the handler inline and supports suspension (delay)" {
        val counter = AtomicInteger(0)

        testApplication {
            application {
                lifeCycle {
                    onAppStarting {
                        delay(10)
                        counter.incrementAndGet()
                    }
                    onAppStarting {
                        delay(10)
                        counter.incrementAndGet()
                    }
                }
            }

            startApplication()
        }

        counter.get() shouldBe 2
    }

    "two onAppStarting hooks using yield() do not deadlock" {
        val counter = AtomicInteger(0)

        testApplication {
            application {
                lifeCycle {
                    onAppStarting {
                        yield()
                        counter.incrementAndGet()
                    }
                    onAppStarting {
                        yield()
                        counter.incrementAndGet()
                    }
                }
            }

            startApplication()
        }

        counter.get() shouldBe 2
    }

    "AppStartException thrown from an onAppStarting hook propagates out of lifeCycle" {
        shouldThrow<AppStartException> {
            testApplication {
                application {
                    lifeCycle {
                        onAppStarting {
                            throw AppStartException("boom")
                        }
                    }
                }
                startApplication()
            }
        }
    }

    "a non-AppStartException from a kontainer OnAppStarting hook is swallowed (startup continues)" {
        val reached = AtomicInteger(0)

        val throwing = object : OnAppStarting {
            override val executionOrder get() = ExecutionOrder.Early
            override suspend fun onAppStarting(application: Application) {
                throw IllegalStateException("not fatal")
            }
        }
        val counting = object : OnAppStarting {
            override val executionOrder get() = ExecutionOrder.Late
            override suspend fun onAppStarting(application: Application) {
                reached.incrementAndGet()
            }
        }

        val k = kontainer {
            singleton(AppLifeCycleHooks::class) {
                AppLifeCycleHooks(
                    onAppStarting = listOf(throwing, counting),
                    onAppStarted = emptyList(),
                    onAppStopPreparing = emptyList(),
                    onAppStopping = emptyList(),
                    onAppStopped = emptyList(),
                )
            }
        }.create()

        testApplication {
            application {
                setupLifecycle(k)
            }
            startApplication()
        }

        reached.get() shouldBe 1
    }

    "kontainer-based OnAppStarting hooks run in executionOrder (highest first)" {
        val order = mutableListOf<String>()

        val earlyHook = object : OnAppStarting {
            override val executionOrder get() = ExecutionOrder.Early
            override suspend fun onAppStarting(application: Application) {
                order.add("early")
            }
        }
        val lateHook = object : OnAppStarting {
            override val executionOrder get() = ExecutionOrder.Late
            override suspend fun onAppStarting(application: Application) {
                order.add("late")
            }
        }

        val k = kontainer {
            singleton(AppLifeCycleHooks::class) {
                AppLifeCycleHooks(
                    onAppStarting = listOf(lateHook, earlyHook),
                    onAppStarted = emptyList(),
                    onAppStopPreparing = emptyList(),
                    onAppStopping = emptyList(),
                    onAppStopped = emptyList(),
                )
            }
        }.create()

        testApplication {
            application {
                setupLifecycle(k)
            }
            startApplication()
        }

        order shouldBe listOf("early", "late")
    }

    "onAppStarted subscribed via kontainer hooks fires on ApplicationStarted event" {
        val counter = AtomicInteger(0)

        val startedHook = object : OnAppStarted {
            override suspend fun onAppStarted(application: Application) {
                delay(5)
                counter.incrementAndGet()
            }
        }

        val k = kontainer {
            singleton(AppLifeCycleHooks::class) {
                AppLifeCycleHooks(
                    onAppStarting = emptyList(),
                    onAppStarted = listOf(startedHook),
                    onAppStopPreparing = emptyList(),
                    onAppStopping = emptyList(),
                    onAppStopped = emptyList(),
                )
            }
        }.create()

        testApplication {
            application {
                setupLifecycle(k)
            }
            startApplication()
        }

        counter.get() shouldBe 1
    }
})
