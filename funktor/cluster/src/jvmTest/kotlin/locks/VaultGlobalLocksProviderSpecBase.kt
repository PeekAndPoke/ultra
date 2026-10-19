package io.peekandpoke.funktor.cluster.locks

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.ints.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.peekandpoke.funktor.cluster.locks.domain.GlobalLockEntry
import io.peekandpoke.funktor.cluster.locks.domain.ServerBeacon
import io.peekandpoke.funktor.cluster.locks.workers.GlobalLocksCleanupWorker
import io.peekandpoke.funktor.cluster.workers.WorkersRunner
import io.peekandpoke.ultra.datetime.Kronos
import io.peekandpoke.ultra.datetime.MpInstant
import io.peekandpoke.ultra.log.Log
import io.peekandpoke.ultra.log.LogLevel
import io.peekandpoke.ultra.log.NullLog
import io.peekandpoke.ultra.vault.Cursor
import io.peekandpoke.ultra.vault.Stored
import io.peekandpoke.ultra.vault.value
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/** The heartbeat and takeover rules of [VaultGlobalLocksProvider], run against each [VaultGlobalLocksProvider.Repo]. */
abstract class VaultGlobalLocksProviderSpecBase : GlobalLockProviderSpecBase() {

    abstract val repo: VaultGlobalLocksProvider.Repo

    fun createSubject(
        ttl: Duration,
        heartbeatInterval: Duration = ttl / 4,
        log: Log = NullLog,
        repository: VaultGlobalLocksProvider.Repo = repo,
    ) = VaultGlobalLocksProvider(
        repository = repository,
        serverId = GlobalServerId.withoutConfig(),
        retryDelayMs = 10,
        ttl = ttl,
        heartbeatInterval = heartbeatInterval,
        log = log,
    )

    override fun createSubject(): GlobalLocksProvider = createSubject(ttl = 60.seconds)

    protected suspend fun stored(key: String): GlobalLockEntry? = repo.findById(key)?.resolve()

    protected suspend fun storedKeys(): List<String> = repo.findAll().toList().map { it.resolve().key }.sorted()

    protected suspend fun insertLock(
        key: String,
        token: String = "token-$key",
        created: MpInstant = MpInstant.now(),
        lastHeartbeat: MpInstant = created,
        expires: MpInstant = created.plus(VaultGlobalLocksProvider.legacyExpiry),
        serverId: String = "other-server",
    ) {
        repo.insert(
            key = key,
            new = GlobalLockEntry(
                key = key,
                serverId = serverId,
                created = created,
                expires = expires,
                token = token,
                lastHeartbeat = lastHeartbeat,
            ),
        )
    }

    /** The repo, but each read ends by [change]-ing the lock it read: as if it was beaten, or re-acquired, right after. */
    private fun changedAfterRead(change: suspend (GlobalLockEntry) -> Unit): VaultGlobalLocksProvider.Repo {
        // Named apart: inside the object, `repo` is the object's own Repository.repo.
        val inner = repo

        return object : VaultGlobalLocksProvider.Repo by inner {
            override suspend fun findAll(): Cursor<Stored<GlobalLockEntry>> =
                Cursor.of(inner.findAll().toList().onEach { change(it.value) })

            override suspend fun findById(id: String?): Stored<GlobalLockEntry>? =
                inner.findById(id)?.also { change(it.value) }
        }
    }

    /** Its holder beats the lock: strictly later than what was read, also within the same millisecond. */
    private suspend fun beat(entry: GlobalLockEntry) {
        val at = maxOf(MpInstant.now(), entry.lastHeartbeat.plus(1.milliseconds))

        repo.heartbeat(key = entry.key, token = entry.token, heartbeatAt = at, expiresAt = at.plus(15.minutes))
    }

    /** Another server re-acquires the lock. */
    private suspend fun reacquire(entry: GlobalLockEntry) {
        repo.removeIfHeld(key = entry.key, token = entry.token)
        insertLock(entry.key, token = "new-owner")
    }

    /** The repo, with each heartbeat going through [intercept], which may call [beat] (the real one), or not. */
    private fun interceptingHeartbeat(intercept: suspend (beat: suspend () -> Boolean) -> Boolean): VaultGlobalLocksProvider.Repo {
        // Named apart: inside the object, `repo` is the object's own Repository.repo.
        val inner = repo

        return object : VaultGlobalLocksProvider.Repo by inner {
            override suspend fun heartbeat(key: String, token: String, heartbeatAt: MpInstant, expiresAt: MpInstant): Boolean =
                intercept { inner.heartbeat(key = key, token = token, heartbeatAt = heartbeatAt, expiresAt = expiresAt) }
        }
    }

    private suspend fun runCleanupWorker(servers: GlobalServerList, log: Log = NullLog) {
        GlobalLocksCleanupWorker(servers = servers, globalLocks = createSubject(ttl = 300.milliseconds), log = log)
            .execute { WorkersRunner.State.Running }
    }

    init {
        beforeEach {
            repo.removeAll()
        }

        // Provider //////////////////////////////////////////////////////////////////////////////////////////////

        "A handler running longer than the ttl keeps its lock" {
            val ttl = 300.milliseconds
            val holder = createSubject(ttl = ttl)
            val contender = createSubject(ttl = ttl)

            coroutineScope {
                val held = async { holder.lock("key") { delay(ttl * 4); "holder" } }

                delay(ttl * 2)
                // Long past the ttl since the lock was taken, yet its heartbeat keeps it alive.
                shouldThrow<LocksException.Timeout> {
                    contender.lock("key", timeout = ttl) { "contender" }
                }

                held.await() shouldBe "holder"
            }

            contender.lock("key", timeout = 1.seconds) { "after" } shouldBe "after"
        }

        "A handler that blocks its thread keeps its lock" {
            val ttl = 300.milliseconds
            val holder = createSubject(ttl = ttl)
            val contender = createSubject(ttl = ttl)

            Executors.newSingleThreadExecutor().asCoroutineDispatcher().use { thread ->
                coroutineScope {
                    // Collected on a thread of its own, which the handler then blocks.
                    val held = async(thread) {
                        holder.acquire("key") {
                            @Suppress("BlockingMethodInNonBlockingContext")
                            Thread.sleep((ttl * 4).inWholeMilliseconds)
                            "holder"
                        }.single()
                    }

                    delay(ttl * 2)
                    shouldThrow<LocksException.Timeout> {
                        contender.lock("key", timeout = ttl) { "contender" }
                    }

                    held.await() shouldBe "holder"
                }
            }
        }

        "A holder keeps beating while blocking work saturates Dispatchers.IO" {
            val ttl = 300.milliseconds
            // When each beat landed. Kept in memory: reading it needs no IO thread (a repo read might).
            val beats = ConcurrentLinkedDeque<MpInstant>()
            val subject = createSubject(
                ttl = ttl,
                heartbeatInterval = 50.milliseconds,
                repository = interceptingHeartbeat { beat -> beat().also { if (it) beats.add(MpInstant.now()) } },
            )
            val holding = CompletableDeferred<Unit>()
            val done = CompletableDeferred<Unit>()

            // On Default: the test itself must not wait for an IO thread.
            withContext(Dispatchers.Default) {
                val held = async { subject.lock("key") { holding.complete(Unit); done.await() } }
                holding.await()
                val acquiredAt = MpInstant.now()

                // More blocking work than Dispatchers.IO has threads (64, or the number of cores).
                val blockers = List(maxOf(64, Runtime.getRuntime().availableProcessors()) + 8) {
                    launch(Dispatchers.IO) {
                        @Suppress("BlockingMethodInNonBlockingContext")
                        Thread.sleep((ttl * 3).inWholeMilliseconds)
                    }
                }

                delay(ttl * 2)
                // IO is still saturated: a beat must have landed within the last ttl.
                val silentFor = MpInstant.now() - (beats.peekLast() ?: acquiredAt)

                blockers.joinAll()
                done.complete(Unit)
                held.await()

                silentFor shouldBeLessThan ttl
            }
        }

        "While its handler runs, the holder beats its lock and keeps the legacy expiry ahead of the beat" {
            val subject = createSubject(ttl = 300.milliseconds, heartbeatInterval = 50.milliseconds)

            subject.lock("key") {
                val acquired = stored("key")!!
                delay(200.milliseconds)
                val beaten = stored("key")!!

                beaten.lastHeartbeat.toEpochMillis() shouldBeGreaterThan acquired.lastHeartbeat.toEpochMillis()
                // Versions without heartbeats only read `expires`: it must stay 15 minutes ahead of the last beat.
                (beaten.expires - beaten.lastHeartbeat) shouldBe 15.minutes
            }
        }

        "A failed heartbeat is retried with the next beat" {
            val log = CapturingLog()
            val failures = AtomicInteger(2)
            val subject = createSubject(
                ttl = 300.milliseconds,
                heartbeatInterval = 50.milliseconds,
                log = log,
                // The first two beats fail, as on a database hiccup.
                repository = interceptingHeartbeat { beat -> if (failures.getAndDecrement() > 0) error("database hiccup") else beat() },
            )

            subject.lock("key") {
                val acquired = stored("key")!!
                delay(300.milliseconds)

                stored("key")!!.lastHeartbeat.toEpochMillis() shouldBeGreaterThan acquired.lastHeartbeat.toEpochMillis()
            }

            // Only the injected failures: a real beat slower than the 50 ms interval also times out under load.
            log.warningCauses.filter { it.message == "database hiccup" } shouldHaveSize 2
        }

        "A hanging heartbeat times out, and the next beat retries" {
            val log = CapturingLog()
            val hangs = AtomicInteger(2)
            val subject = createSubject(
                ttl = 300.milliseconds,
                heartbeatInterval = 50.milliseconds,
                log = log,
                // The first two beats hang, as on a stuck connection.
                repository = interceptingHeartbeat { beat -> if (hangs.getAndDecrement() > 0) awaitCancellation() else beat() },
            )

            subject.lock("key") {
                val acquired = stored("key")!!
                delay(400.milliseconds)

                stored("key")!!.lastHeartbeat.toEpochMillis() shouldBeGreaterThan acquired.lastHeartbeat.toEpochMillis()
            }

            withClue("the two injected hangs each end in a timeout warning; a slow real beat may add more") {
                log.warningCauses.filterIsInstance<TimeoutCancellationException>().size shouldBeGreaterThanOrEqual 2
            }
            hangs.get() shouldBeLessThanOrEqual 0
        }

        "A lock whose holder stopped beating is taken over once the ttl has passed" {
            val now = MpInstant.now()
            // Its expiry is far away: what counts is the heartbeat.
            insertLock("key", created = now.minus(2.seconds), lastHeartbeat = now.minus(1.seconds), expires = now.plus(10.minutes))

            val subject = createSubject(ttl = 300.milliseconds)
            val start = Kronos.systemUtc.instantNow()

            subject.lock("key", timeout = 2.seconds) { "taken over" } shouldBe "taken over"
            (Kronos.systemUtc.instantNow() - start) shouldBeLessThan 500.milliseconds
        }

        "A lock that is still beating is not taken over, however old it is" {
            val now = MpInstant.now()
            // Created long ago and past its expiry: what counts is the heartbeat.
            insertLock("key", token = "busy", created = now.minus(1.hours), lastHeartbeat = now, expires = now.minus(1.minutes))

            shouldThrow<LocksException.Timeout> {
                createSubject(ttl = 5.seconds).lock("key", timeout = 300.milliseconds) { "stolen" }
            }

            stored("key")?.token shouldBe "busy"
        }

        "A lock without a token (written before heartbeats existed) is taken over only once it has expired" {
            val now = MpInstant.now()
            // Its heartbeat defaults to its creation, an hour ago, and must not count.
            insertLock("alive", token = "", created = now.minus(1.hours), expires = now.plus(10.minutes))
            insertLock("expired", token = "", created = now.minus(30.minutes), expires = now.minus(15.minutes))

            val subject = createSubject(ttl = 300.milliseconds)

            shouldThrow<LocksException.Timeout> {
                subject.lock("alive", timeout = 300.milliseconds) { "stolen" }
            }

            val start = Kronos.systemUtc.instantNow()

            subject.lock("expired", timeout = 2.seconds) { "taken over" } shouldBe "taken over"
            // Taken over by the acquire itself, without waiting for the cleanup worker.
            (Kronos.systemUtc.instantNow() - start) shouldBeLessThan 500.milliseconds
        }

        "Contenders taking over the same dead lock get it one at a time" {
            val now = MpInstant.now()
            insertLock("key", created = now.minus(2.minutes), lastHeartbeat = now.minus(1.minutes))

            val inside = AtomicInteger(0)
            val mostInside = AtomicInteger(0)
            val runs = AtomicInteger(0)

            // Several at once, so that their takeovers collide (on Arango: write-write conflicts).
            coroutineScope {
                List(6) { createSubject(ttl = 300.milliseconds) }.map { contender ->
                    async {
                        contender.lock("key", timeout = 10.seconds) {
                            mostInside.accumulateAndGet(inside.incrementAndGet()) { a, b -> maxOf(a, b) }
                            delay(50.milliseconds)
                            inside.decrementAndGet()
                            runs.incrementAndGet()
                        }
                    }
                }.awaitAll()
            }

            runs.get() shouldBe 6
            mostInside.get() shouldBe 1
        }

        "The takeover leaves a lock alone that changed after it was read" {
            listOf<Pair<String, suspend (GlobalLockEntry) -> Unit>>("beaten" to { beat(it) }, "re-acquired" to { reacquire(it) })
                .forEach { (how, change) ->
                    withClue(how) {
                        repo.removeAll()
                        val now = MpInstant.now()
                        // Silent when read.
                        insertLock("key", created = now.minus(2.minutes), lastHeartbeat = now.minus(1.minutes))

                        shouldThrow<LocksException.Timeout> {
                            createSubject(ttl = 300.milliseconds, repository = changedAfterRead(change))
                                .lock("key", timeout = 200.milliseconds) { "taken over" }
                        }

                        storedKeys() shouldBe listOf("key")
                    }
                }
        }

        "A lock whose key looks like a Mongo ObjectId is taken over, held and released like any other" {
            val key = "5f2b8c1e9d3a4b6c7e8f9a0b"
            val now = MpInstant.now()
            insertLock(key, created = now.minus(2.minutes), lastHeartbeat = now.minus(1.minutes))

            val subject = createSubject(ttl = 300.milliseconds)
            val start = Kronos.systemUtc.instantNow()

            subject.lock(key, timeout = 2.seconds) {
                stored(key)?.serverId shouldBe GlobalServerId.withoutConfig().getId()
            }

            // Taken over by the acquire itself, without waiting for the cleanup worker.
            (Kronos.systemUtc.instantNow() - start) shouldBeLessThan 1.seconds
            storedKeys().shouldBeEmpty()
        }

        "A holder whose lock was taken over leaves the new owner's lock alone, and warns once" {
            val log = CapturingLog()
            val subject = createSubject(ttl = 300.milliseconds, heartbeatInterval = 50.milliseconds, log = log)

            subject.lock("key") {
                // Another server takes the lock over while the handler still runs.
                repo.removeIfHeld(key = "key", token = stored("key")!!.token) shouldBe true
                insertLock("key", token = "new-owner")
                // Long enough for several heartbeats to find the lock gone.
                delay(250.milliseconds)
            }

            stored("key")?.token shouldBe "new-owner"
            log.warnings.single() shouldContain "taken over"
        }

        "A cancelled handler still releases its lock" {
            val subject = createSubject(ttl = 60.seconds)
            val holding = CompletableDeferred<Unit>()

            coroutineScope {
                val holder = launch {
                    // The cancellation surfaces as a LocksException.Execution, which is not what this test is about.
                    runCatching {
                        subject.lock("key") { holding.complete(Unit); awaitCancellation() }
                    }
                }

                holding.await()
                stored("key") shouldNotBe null

                holder.cancelAndJoin()
            }

            // Released by the holder itself, not left for the ttl.
            storedKeys().shouldBeEmpty()
        }

        "releaseExpired releases the dead locks and keeps the live ones" {
            val now = MpInstant.now()
            insertLock("silent", created = now.minus(1.seconds))
            insertLock("beating", created = now.minus(1.hours), lastHeartbeat = now)
            insertLock("legacy-expired", token = "", created = now.minus(30.minutes), expires = now.minus(1.minutes))
            insertLock("legacy-alive", token = "", created = now.minus(1.hours), expires = now.plus(10.minutes))

            val released = createSubject(ttl = 300.milliseconds).releaseExpired()

            released.map { it.key }.sorted() shouldBe listOf("legacy-expired", "silent")
            storedKeys() shouldBe listOf("beating", "legacy-alive")
        }

        "The cleanup worker releases the locks whose holder went silent, and keeps the beating ones" {
            val now = MpInstant.now()
            // Silent for longer than the ttl, though its expiry is far away.
            insertLock("silent", created = now.minus(2.seconds), lastHeartbeat = now.minus(1.seconds), expires = now.plus(10.minutes))
            // Beating, though created long ago and past its expiry.
            insertLock("beating", created = now.minus(1.hours), lastHeartbeat = now, expires = now.minus(1.minutes))

            // The holding server is alive, so only the heartbeat decides.
            val servers = GlobalServerList().apply {
                setAll(listOf(ServerBeacon(serverId = "other-server", lastPing = now)))
            }

            runCleanupWorker(servers)

            storedKeys() shouldBe listOf("beating")
        }

        "releaseExpired and releaseBy leave a lock alone that changed after they read it" {
            val now = MpInstant.now()
            // Silent when read, beaten right after.
            insertLock("key", created = now.minus(2.minutes), lastHeartbeat = now.minus(1.minutes))

            createSubject(ttl = 300.milliseconds, repository = changedAfterRead { beat(it) })
                .releaseExpired().shouldBeEmpty()

            stored("key")!!.lastHeartbeat.toEpochMillis() shouldBeGreaterThan now.minus(1.minutes).toEpochMillis()

            // Re-acquired by another server right after it was read.
            createSubject(ttl = 300.milliseconds, repository = changedAfterRead { reacquire(it) })
                .releaseBy { true }.shouldBeEmpty()

            stored("key")?.token shouldBe "new-owner"
        }

        "The cleanup worker leaves a beating lock alone, though its server has no beacon (yet)" {
            // E.g. a server that just started, and has not written its first beacon.
            insertLock("beating", lastHeartbeat = MpInstant.now(), serverId = "starting-server")

            runCleanupWorker(
                GlobalServerList().apply { setAll(listOf(ServerBeacon(serverId = "other-server", lastPing = MpInstant.now()))) }
            )

            storedKeys() shouldBe listOf("beating")
        }

        "Before the beacons were read once, the cleanup worker releases no lock for its server being dead" {
            // Without a token and not expired: only the dead-server rule could release it.
            insertLock("legacy", token = "", created = MpInstant.now().minus(1.hours), expires = MpInstant.now().plus(10.minutes))

            runCleanupWorker(GlobalServerList())

            storedKeys() shouldBe listOf("legacy")
        }

        "Once the beacons were read, the cleanup worker releases a lock without a token whose server is dead" {
            val now = MpInstant.now()
            // Neither is expired, so only the dead-server rule decides.
            insertLock("dead", token = "", created = now.minus(1.hours), expires = now.plus(10.minutes), serverId = "dead-server")
            insertLock("alive", token = "", created = now.minus(1.hours), expires = now.plus(10.minutes), serverId = "other-server")
            val log = CapturingLog()
            val dead = stored("dead")!!

            runCleanupWorker(
                servers = GlobalServerList().apply {
                    setAll(
                        listOf(
                            ServerBeacon(serverId = "dead-server", lastPing = now.minus(10.minutes)),
                            ServerBeacon(serverId = "other-server", lastPing = now),
                        )
                    )
                },
                log = log,
            )

            storedKeys() shouldBe listOf("alive")
            // Logged with the field a lock without heartbeats is judged by.
            log.warnings.single() shouldContain "dead-server => dead => ${dead.created.toIsoString()} => expires ${dead.expires.toIsoString()}"
        }

        // Repo //////////////////////////////////////////////////////////////////////////////////////////////////

        "heartbeat moves the heartbeat and the expiry, of a lock held with the token only" {
            val then = MpInstant.now().minus(1.minutes)
            insertLock("key", token = "mine", created = then)

            val now = MpInstant.now()
            val expiresAt = now.plus(VaultGlobalLocksProvider.legacyExpiry)

            repo.heartbeat(key = "key", token = "not-mine", heartbeatAt = now, expiresAt = expiresAt) shouldBe false
            stored("key")!!.lastHeartbeat.toEpochMillis() shouldBe then.toEpochMillis()

            repo.heartbeat(key = "key", token = "mine", heartbeatAt = now, expiresAt = expiresAt) shouldBe true
            // Read back: the written instants must decode, and nothing else may change.
            stored("key")!!.let {
                it.lastHeartbeat.toEpochMillis() shouldBe now.toEpochMillis()
                it.expires.toEpochMillis() shouldBe expiresAt.toEpochMillis()
                it.created.toEpochMillis() shouldBe then.toEpochMillis()
                it.token shouldBe "mine"
                it.serverId shouldBe "other-server"
            }

            repo.heartbeat(key = "other", token = "mine", heartbeatAt = now, expiresAt = expiresAt) shouldBe false
        }

        "removeIfHeld removes a lock for its own token only" {
            insertLock("key", token = "mine")

            repo.removeIfHeld(key = "key", token = "not-mine") shouldBe false
            stored("key") shouldNotBe null

            repo.removeIfHeld(key = "key", token = "mine") shouldBe true
            stored("key") shouldBe null

            repo.removeIfHeld(key = "key", token = "mine") shouldBe false
        }

        "removeIfUnchanged loses to a heartbeat in between" {
            insertLock("key", token = "mine", created = MpInstant.now().minus(1.minutes))
            val read = stored("key")!!

            val now = MpInstant.now()
            repo.heartbeat(key = "key", token = "mine", heartbeatAt = now, expiresAt = now.plus(1.minutes)) shouldBe true

            repo.removeIfUnchanged(read) shouldBe false
            stored("key") shouldNotBe null

            repo.removeIfUnchanged(stored("key")!!) shouldBe true
            stored("key") shouldBe null
        }

        "removeIfUnchanged loses to a re-acquisition in between" {
            val created = MpInstant.now().minus(1.minutes)
            insertLock("key", token = "first", created = created)
            val read = stored("key")!!

            repo.removeIfHeld(key = "key", token = "first") shouldBe true
            // The same timestamps: only the token tells the two acquisitions apart.
            insertLock("key", token = "second", created = created)

            repo.removeIfUnchanged(read) shouldBe false
            stored("key")?.token shouldBe "second"
        }

        "removeIfUnchanged of one lock by several at once: exactly one wins, and none fails" {
            // Many rounds of a crowd, so that their removes collide (on Arango: write-write conflicts).
            repeat(20) {
                insertLock("key", created = MpInstant.now().minus(1.minutes))
                val read = stored("key")!!

                val removed = coroutineScope {
                    List(8) { async(Dispatchers.IO) { repo.removeIfUnchanged(read) } }.awaitAll()
                }

                removed.count { it } shouldBe 1
            }
        }

        "removeIfUnchanged tells two token-less locks apart by their creation time" {
            insertLock("key", token = "", created = MpInstant.now().minus(2.minutes))
            val read = stored("key")!!

            repo.remove("key")
            insertLock("key", token = "", created = MpInstant.now().minus(1.minutes))

            repo.removeIfUnchanged(read) shouldBe false
            stored("key") shouldNotBe null

            repo.removeIfUnchanged(stored("key")!!) shouldBe true
            stored("key") shouldBe null
        }
    }

    private class CapturingLog : Log {
        private val captured = mutableListOf<Pair<String, Throwable?>>()

        val warnings: List<String> get() = synchronized(captured) { captured.map { it.first } }

        /** The errors logged with the warnings, so a test can count the failures it injected. */
        val warningCauses: List<Throwable> get() = synchronized(captured) { captured.mapNotNull { it.second } }

        override fun log(level: LogLevel, message: String, error: Throwable?) {
            if (level == LogLevel.WARNING) {
                synchronized(captured) { captured.add(message to error) }
            }
        }
    }
}
