package io.peekandpoke.ultra.kontainer.e2e

import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.peekandpoke.ultra.kontainer.kontainer
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

class LazyDyn

/**
 * Pins the lazy override (ULTRA-00333): one run under concurrent requests, and a retry after a throw.
 */
class LazyOverrideSpec : StringSpec({

    "Two threads request an override at once: the lambda runs once, the second waits for it, both get one instance" {

        val runs = AtomicInteger()
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)

        val subject = kontainer { dynamic(LazyDyn::class) }.create {
            with {
                runs.incrementAndGet()
                entered.countDown()
                release.await(5, TimeUnit.SECONDS)
                LazyDyn()
            }
        }

        val first = AtomicReference<Any>()
        val second = AtomicReference<Any>()

        val threadA = Thread { first.set(subject.get(LazyDyn::class)) }.apply { start() }
        entered.await(5, TimeUnit.SECONDS) shouldBe true

        val threadB = Thread { second.set(subject.get(LazyDyn::class)) }.apply { start() }

        // Bounded wait until B blocks on the provider's lock, which A holds while the lambda runs
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5)
        while (threadB.state != Thread.State.BLOCKED && System.nanoTime() < deadline) {
            Thread.sleep(5)
        }
        val stateOfB = threadB.state

        release.countDown()
        threadA.join(5_000)
        threadB.join(5_000)

        assertSoftly {
            stateOfB shouldBe Thread.State.BLOCKED
            runs.get() shouldBe 1
            first.get().shouldNotBeNull()
            first.get() shouldBeSameInstanceAs second.get()
        }
    }

    "An override that throws keeps no value: the next request runs it again, until it has returned a value" {

        val runs = AtomicInteger()

        val subject = kontainer { dynamic(LazyDyn::class) }.create {
            with {
                if (runs.incrementAndGet() == 1) error("first run fails")
                LazyDyn()
            }
        }

        shouldThrow<IllegalStateException> {
            subject.get(LazyDyn::class)
        }.message shouldBe "first run fails"

        val value = subject.get(LazyDyn::class)

        assertSoftly {
            subject.get(LazyDyn::class) shouldBeSameInstanceAs value
            runs.get() shouldBe 2
        }
    }
})
