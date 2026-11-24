package io.peekandpoke.ultra.kontainer.e2e

import io.kotest.assertions.assertSoftly
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.comparables.shouldBeGreaterThanOrEqualTo
import io.kotest.matchers.comparables.shouldBeLessThanOrEqualTo
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.peekandpoke.ultra.kontainer.ServiceProvider.Created
import io.peekandpoke.ultra.kontainer.domain.DebugInfo
import io.peekandpoke.ultra.kontainer.getName
import io.peekandpoke.ultra.kontainer.kontainer
import java.time.Instant
import java.util.Collections
import java.util.IdentityHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread

class CreatedProto

class CreatedSingle

class CreatedDyn

open class CreatedOverridable

class CreatedOverride : CreatedOverridable()

/** Counts its constructor runs; the counter is reset by each test that reads it. */
class CreatedCountedDyn {
    companion object {
        val births = AtomicInteger()
    }

    init {
        births.incrementAndGet()
    }
}

/** Waits in its constructor until a second instance is being constructed at the same time. */
class CreatedRendezvous {
    companion object {
        @Volatile
        var latch = CountDownLatch(2)
    }

    val metTheOther: Boolean = latch.let {
        it.countDown()
        it.await(5, TimeUnit.SECONDS)
    }
}

/** Runs [block] on [threads] threads that start together; returns once all are done. */
private fun runTogether(threads: Int, block: (Int) -> Unit) {
    val barrier = CyclicBarrier(threads)
    val errors = Collections.synchronizedList(mutableListOf<Throwable>())

    (0 until threads)
        .map { idx ->
            thread {
                try {
                    barrier.await()
                    block(idx)
                } catch (e: Throwable) {
                    errors.add(e)
                }
            }
        }
        .forEach { it.join() }

    errors.firstOrNull()?.let { throw it }
}

/**
 * Pins what a provider reports about its creations: a count and the first and last creation time, no instances.
 */
class ServiceProviderCreatedSpec : StringSpec({

    "A provider that created nothing reports none" {

        val subject = kontainer {
            prototype(CreatedProto::class)
            singleton(CreatedSingle::class)
            dynamic(CreatedDyn::class)
        }.create()

        assertSoftly {
            subject.getProvider(CreatedProto::class).created shouldBe Created.none
            subject.getProvider(CreatedSingle::class).created shouldBe Created.none
            subject.getProvider(CreatedDyn::class).created shouldBe Created.none
        }
    }

    "A prototype provider counts every creation, keeps the first time and moves the last time" {

        val subject = kontainer { prototype(CreatedProto::class) }.create()
        val provider = subject.getProvider(CreatedProto::class)

        subject.get(CreatedProto::class)
        val afterOne = provider.created

        Thread.sleep(5)
        subject.get(CreatedProto::class)
        val afterTwo = provider.created

        assertSoftly {
            afterOne.count shouldBe 1L
            afterOne.firstAt.shouldNotBeNull()
            afterOne.lastAt shouldBe afterOne.firstAt

            afterTwo.count shouldBe 2L
            withClue("the first time stays") {
                afterTwo.firstAt shouldBe afterOne.firstAt
            }
            withClue("the last time moves") {
                afterTwo.lastAt.shouldNotBeNull() shouldBeGreaterThan afterOne.lastAt.shouldNotBeNull()
            }
        }
    }

    "Concurrent creations of one prototype are all counted" {

        val subject = kontainer { prototype(CreatedProto::class) }.create()
        val threads = 16
        val perThread = 500

        runTogether(threads) {
            repeat(perThread) { subject.get(CreatedProto::class) }
        }

        val created = subject.getProvider(CreatedProto::class).created

        assertSoftly {
            created.count shouldBe (threads * perThread).toLong()
            created.firstAt.shouldNotBeNull() shouldBeLessThanOrEqualTo created.lastAt.shouldNotBeNull()
        }
    }

    "Concurrent creations of one prototype run in parallel, they are not serialized by the provider" {

        CreatedRendezvous.latch = CountDownLatch(2)

        val subject = kontainer { prototype(CreatedRendezvous::class) }.create()
        val met = Collections.synchronizedList(mutableListOf<Boolean>())

        runTogether(2) {
            met.add(subject.get(CreatedRendezvous::class).metTheOther)
        }

        withClue("each construction saw the other one running at the same time") {
            met shouldBe listOf(true, true)
        }
    }

    "A singleton counts one creation, at the time it was created, and keeps it" {

        val subject = kontainer { dynamic(CreatedDyn::class) }.create()
        val provider = subject.getProvider(CreatedDyn::class)

        val before = Instant.now()
        subject.get(CreatedDyn::class)
        val after = Instant.now()
        val created = provider.created

        subject.get(CreatedDyn::class)

        assertSoftly {
            created.count shouldBe 1L
            created.lastAt shouldBe created.firstAt
            created.firstAt.shouldNotBeNull() shouldBeGreaterThanOrEqualTo before
            created.firstAt.shouldNotBeNull() shouldBeLessThanOrEqualTo after

            provider.created shouldBe created
        }
    }

    "Concurrent first gets of a singleton create it once" {

        repeat(50) {
            CreatedCountedDyn.births.set(0)

            val subject = kontainer { dynamic(CreatedCountedDyn::class) }.create()
            val seen = Collections.synchronizedSet(Collections.newSetFromMap(IdentityHashMap<Any, Boolean>()))

            runTogether(8) { seen.add(subject.get(CreatedCountedDyn::class)) }

            assertSoftly {
                seen shouldHaveSize 1
                CreatedCountedDyn.births.get() shouldBe 1
                subject.getProvider(CreatedCountedDyn::class).created.count shouldBe 1L
            }
        }
    }

    "An override counts one creation, made when its kontainer was created" {

        val blueprint = kontainer { dynamic(CreatedOverridable::class) }

        val before = Instant.now()
        val subject = blueprint.create { with { CreatedOverride() } }
        val after = Instant.now()

        val created = subject.getProvider(CreatedOverridable::class).created
        subject.get(CreatedOverridable::class)

        assertSoftly {
            created.count shouldBe 1L
            created.lastAt shouldBe created.firstAt
            created.firstAt.shouldNotBeNull() shouldBeGreaterThanOrEqualTo before
            created.firstAt.shouldNotBeNull() shouldBeLessThanOrEqualTo after

            subject.getProvider(CreatedOverridable::class).created shouldBe created
        }
    }

    "Concurrent first requests of a provider get one and the same provider" {

        repeat(200) {
            val subject = kontainer { dynamic(CreatedDyn::class) }.create()
            val factory = subject.getServiceProviderFactory()
            val seen = Collections.synchronizedSet(Collections.newSetFromMap(IdentityHashMap<Any, Boolean>()))

            runTogether(8) { seen.add(factory.getProvider(CreatedDyn::class)) }

            seen shouldHaveSize 1
        }
    }

    "The debug info and the dump show the provider's count and both times" {

        val subject = kontainer { prototype(CreatedProto::class) }.create()

        subject.get(CreatedProto::class)
        Thread.sleep(5)
        subject.get(CreatedProto::class)

        val created = subject.getProvider(CreatedProto::class).created
        val info = subject.tools.getDebugInfo().services.first { it.cls.fqn == CreatedProto::class.java.name }
        val row = subject.tools.dumpKontainer().lines().first { it.startsWith(CreatedProto::class.getName() + " ") }

        assertSoftly {
            info.created shouldBe DebugInfo.CreatedDebugInfo(
                count = 2,
                firstAt = created.firstAt,
                lastAt = created.lastAt,
            )
            row shouldContain "| 2, first ${created.firstAt}, last ${created.lastAt}"
        }
    }
})
