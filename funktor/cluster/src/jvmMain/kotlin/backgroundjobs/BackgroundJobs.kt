package de.peekandpoke.funktor.cluster.backgroundjobs

import coroutines.measureCoroutine
import de.peekandpoke.funktor.cluster.backgroundjobs.domain.BackgroundJobArchived
import de.peekandpoke.funktor.cluster.backgroundjobs.domain.BackgroundJobExecutionResult
import de.peekandpoke.funktor.cluster.backgroundjobs.domain.BackgroundJobQueued
import de.peekandpoke.funktor.cluster.backgroundjobs.domain.BackgroundJobRetryPolicy
import de.peekandpoke.funktor.cluster.locks.GlobalServerId
import de.peekandpoke.funktor.cluster.locks.LocksException
import de.peekandpoke.funktor.cluster.workers.StateProvider
import de.peekandpoke.funktor.core.Retry
import de.peekandpoke.ultra.common.datetime.Kronos
import de.peekandpoke.ultra.common.datetime.MpInstant
import de.peekandpoke.ultra.common.reflection.TypeRef
import de.peekandpoke.ultra.log.Log
import de.peekandpoke.ultra.slumber.Codec
import de.peekandpoke.ultra.slumber.SlumberConfig
import de.peekandpoke.ultra.vault.Cursor
import de.peekandpoke.ultra.vault.EntityCache
import de.peekandpoke.ultra.vault.Repository
import de.peekandpoke.ultra.vault.Stored
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.milliseconds

class BackgroundJobs(
    handlers: Lazy<List<Handler<Any?, *>>>,
    entityCache: Lazy<EntityCache?>,
    queue: Lazy<Queue>,
    archive: Lazy<Archive>,
    serverId: Lazy<GlobalServerId>,
    codec: Lazy<DataCodec>,
    log: Lazy<Log>,
) {
    /**
     * Codec used to serialize and deserialize the data associated to Jobs.
     */
    class DataCodec : Codec(config = SlumberConfig.default)

    /**
     * The Job queue is used for putting background-jobs into the processing queue.
     */
    interface Queue {
        /**
         * Creates the given [job].
         */
        suspend fun create(job: BackgroundJobQueued)

        /**
         * Modifies the given [job].
         */
        suspend fun modify(
            job: Stored<BackgroundJobQueued>,
            modify: (BackgroundJobQueued) -> BackgroundJobQueued,
        ): Stored<BackgroundJobQueued>

        /**
         * Creates the given [job] if there is no other job with the same type and hash-code present.
         */
        suspend fun queueIfNotPresent(job: BackgroundJobQueued)

        /**
         * Removes the given [job].
         */
        suspend fun remove(job: Stored<BackgroundJobQueued>)

        /**
         * Lists all jobs.
         */
        suspend fun listAll(): Cursor<Stored<BackgroundJobQueued>>

        /**
         * Lists all jobs.
         */
        suspend fun listAll(page: Int, epp: Int): Cursor<Stored<BackgroundJobQueued>>

        /**
         * Lists all queued jobs that are due.
         */
        suspend fun listAllDue(due: MpInstant): Cursor<Stored<BackgroundJobQueued>>

        /**
         * Get the next job that is due.
         */
        suspend fun claimNextDue(due: MpInstant): Stored<BackgroundJobQueued>?

        /**
         * Get a job by its [id].
         */
        suspend fun get(id: String): Stored<BackgroundJobQueued>?

        /**
         * Null implementation
         */
        class Null : Queue {
            override suspend fun create(job: BackgroundJobQueued) {
                // not implemented
            }

            override suspend fun modify(
                job: Stored<BackgroundJobQueued>,
                modify: (BackgroundJobQueued) -> BackgroundJobQueued,
            ): Stored<BackgroundJobQueued> {
                // not implemented
                return job
            }

            override suspend fun queueIfNotPresent(job: BackgroundJobQueued) {
                // not implemented
            }

            override suspend fun remove(job: Stored<BackgroundJobQueued>) {
                // not implemented
            }

            override suspend fun listAll(): Cursor<Stored<BackgroundJobQueued>> {
                // not implemented
                return Cursor.empty()
            }

            override suspend fun listAll(page: Int, epp: Int): Cursor<Stored<BackgroundJobQueued>> {
                // not implemented
                return Cursor.empty()
            }

            override suspend fun listAllDue(due: MpInstant): Cursor<Stored<BackgroundJobQueued>> {
                // not implemented
                return Cursor.empty()
            }

            override suspend fun claimNextDue(due: MpInstant): Stored<BackgroundJobQueued>? {
                return null
            }

            override suspend fun get(id: String): Stored<BackgroundJobQueued>? {
                // not implemented
                return null
            }
        }

        /**
         * Vault implementation of the [Queue]
         */
        class Vault(private val repo: Repo) : Queue {

            /**
             * The Vault queue needs a [Repository] with some additional functions.
             */
            interface Repo : Repository<BackgroundJobQueued> {

                /**
                 * Returns all queued Jobs sorted by [BackgroundJobQueued.dueAt] ascending.
                 */
                suspend fun findAllSorted(page: Int? = null, epp: Int? = null): Cursor<Stored<BackgroundJobQueued>>

                /**
                 * Returns all queued Jobs that are [due] sorted by [BackgroundJobQueued.dueAt] ascending.
                 */
                suspend fun findAllDueSorted(due: MpInstant): Cursor<Stored<BackgroundJobQueued>>

                /**
                 * Returns the next job that is [due] with the least [BackgroundJobQueued.dueAt].
                 */
                suspend fun claimNextDue(due: MpInstant): Stored<BackgroundJobQueued>?

                /**
                 * Checks if there already is a job in the queue with given [type] and [dataHash].
                 */
                suspend fun hasWaitingByTypeAndDataHash(type: String, dataHash: Int): Boolean
            }

            override suspend fun create(job: BackgroundJobQueued) {
                repo.insert(job)
            }

            override suspend fun modify(
                job: Stored<BackgroundJobQueued>,
                modify: (BackgroundJobQueued) -> BackgroundJobQueued,
            ): Stored<BackgroundJobQueued> {
                return withRetry { repo.save(job, modify) }
                    ?: throw IllegalStateException("Could not save background job ${job._id}")
            }

            override suspend fun queueIfNotPresent(job: BackgroundJobQueued) {
                withRetry {
                    if (!repo.hasWaitingByTypeAndDataHash(job.type, job.dataHash)) {
                        // IMPORTANT: create without lock otherwise we have a deadlock
                        create(job)
                    }
                }
            }

            override suspend fun remove(job: Stored<BackgroundJobQueued>) {
                withRetry {
                    repo.remove(job)
                }
            }

            override suspend fun listAll(): Cursor<Stored<BackgroundJobQueued>> {
                return repo.findAllSorted()
            }

            override suspend fun listAll(page: Int, epp: Int): Cursor<Stored<BackgroundJobQueued>> {
                return repo.findAllSorted(page = page, epp = epp)
            }

            override suspend fun listAllDue(due: MpInstant): Cursor<Stored<BackgroundJobQueued>> {
                return repo.findAllDueSorted(due)
            }

            override suspend fun claimNextDue(due: MpInstant): Stored<BackgroundJobQueued>? {
                return repo.claimNextDue(due)
            }

            override suspend fun get(id: String): Stored<BackgroundJobQueued>? {
                return repo.findById(id)
            }

            private suspend fun <T> withRetry(block: suspend () -> T): T? {
                return Retry.retry(
                    attempts = 10,
                    delays = 10.milliseconds..100.milliseconds,
                    block = block,
                )
            }
        }
    }

    /**
     * The Archive is used to store processing results of Jobs.
     */
    interface Archive {
        /**
         * Saves the [job] into the archive
         */
        suspend fun save(job: BackgroundJobArchived)

        /**
         * Lists all archived jobs.
         */
        suspend fun list(): Cursor<Stored<BackgroundJobArchived>>

        /**
         * Lists all archived jobs paged.
         */
        suspend fun list(page: Int, epp: Int): Cursor<Stored<BackgroundJobArchived>>

        /**
         * Get an archived job by its [id].
         */
        suspend fun get(id: String): Stored<BackgroundJobArchived>?

        class Null : Archive {
            override suspend fun save(job: BackgroundJobArchived) {
                // not implemented
            }

            override suspend fun list(): Cursor<Stored<BackgroundJobArchived>> {
                // not implemented
                return Cursor.empty()
            }

            override suspend fun list(page: Int, epp: Int): Cursor<Stored<BackgroundJobArchived>> {
                // not implemented
                return Cursor.empty()
            }

            override suspend fun get(id: String): Stored<BackgroundJobArchived>? {
                // not implemented
                return null
            }
        }

        class Vault(private val repo: Repo) : Archive {

            interface Repo : Repository<BackgroundJobArchived> {
                suspend fun findAllNewestFirst(
                    page: Int? = null,
                    epp: Int? = null,
                ): Cursor<Stored<BackgroundJobArchived>>
            }

            override suspend fun save(job: BackgroundJobArchived) {
                repo.insert(job)
            }

            override suspend fun list(): Cursor<Stored<BackgroundJobArchived>> {
                return repo.findAllNewestFirst()
            }

            override suspend fun list(page: Int, epp: Int): Cursor<Stored<BackgroundJobArchived>> {
                return repo.findAllNewestFirst(page = page, epp = epp)
            }

            override suspend fun get(id: String): Stored<BackgroundJobArchived>? {
                return repo.findById(id)
            }
        }
    }

    abstract class Handler<D : Any?, R : Any?>(
        backgroundJobs: Lazy<BackgroundJobs>,
        val dataType: TypeRef<D>,
    ) {
        companion object {
            internal fun systemNow() = Kronos.systemUtc.instantNow()
        }

        /**
         * The BackgroundJobs facade.
         */
        private val backgroundJobs: BackgroundJobs by backgroundJobs

        /**
         * Every handle must define a unique string identifying the jobs it can execute.
         */
        abstract val jobType: String

        /**
         * Every handler must implement a method to execute a job.
         */
        abstract suspend fun execute(job: BackgroundJobQueued, data: D): R

        /**
         * Returns true if the handler is able to execute the given [job].
         */
        fun canHandle(job: BackgroundJobQueued) = job.type == jobType

        /**
         * Queues a new job with the given [data]
         */
        suspend fun queue(
            data: D,
            dueAt: MpInstant = systemNow().plus(200.milliseconds),
            retryPolicy: BackgroundJobRetryPolicy = BackgroundJobRetryPolicy.None,
        ) {
            backgroundJobs.queueJob(
                BackgroundJobQueued(
                    type = jobType,
                    data = data,
                    retryPolicy = retryPolicy,
                    createdAt = systemNow(),
                    dueAt = dueAt,
                )
            )
        }

        /**
         * Queues a new job with the given [data] if there is not yet a job queued with that exact data.
         *
         * The equality of the data will be calculated by its hash-code.
         */
        suspend fun queueIfNotPresent(
            data: D,
            dueAt: MpInstant = systemNow().plus(10.milliseconds),
            retryPolicy: BackgroundJobRetryPolicy = BackgroundJobRetryPolicy.None,
        ) {
            backgroundJobs.queueJobIfNotPresent(
                BackgroundJobQueued(
                    type = jobType,
                    data = data,
                    retryPolicy = retryPolicy,
                    createdAt = systemNow(),
                    dueAt = dueAt,
                )
            )
        }

        /**
         * Queues a new job with the given [data] if there is not yet a job queued with that exact data.
         *
         * The equality of the data will be calculated by its hash-code.
         */
        suspend fun queueIfNotPresent(
            data: D,
            dueIn: Duration = Duration.ZERO,
            retryPolicy: BackgroundJobRetryPolicy = BackgroundJobRetryPolicy.None,
        ) {
            return queueIfNotPresent(
                data = data,
                dueAt = systemNow().plus(dueIn),
                retryPolicy = retryPolicy,
            )
        }

        /**
         * Queues a new job with the given [data] if there is not yet a job queued with that exact data.
         *
         * The equality of the data will be calculated by its hash-code.
         */
        suspend fun queueIfNotPresent(
            data: D,
            retryPolicy: BackgroundJobRetryPolicy = BackgroundJobRetryPolicy.None,
        ) {
            return queueIfNotPresent(
                data = data,
                dueAt = systemNow(),
                retryPolicy = retryPolicy,
            )
        }
    }

    private val handlers: List<Handler<Any?, *>> by handlers
    private val entityCache: EntityCache? by entityCache
    private val queue: Queue by queue
    private val archive: Archive by archive
    private val serverId: GlobalServerId by serverId
    private val codec: DataCodec by codec
    private val log: Log by log

    private val mutex = Mutex()

    suspend fun queueJob(job: BackgroundJobQueued) {
        mutex.withLock {
            queue.create(job)
        }
    }

    suspend fun queueJobIfNotPresent(job: BackgroundJobQueued) {
        mutex.withLock {
            queue.queueIfNotPresent(job)
        }
    }

    suspend fun claimNextDueJob(): Stored<BackgroundJobQueued>? {
        return mutex.withLock {
            queue.claimNextDue(
                Kronos.systemUtc.instantNow()
            )
        }
    }

    suspend fun listQueuedJobs(): Cursor<Stored<BackgroundJobQueued>> {
        return queue.listAll()
    }

    suspend fun listQueuedJobs(page: Int, epp: Int): Cursor<Stored<BackgroundJobQueued>> {
        return queue.listAll(page = page, epp = epp)
    }

    suspend fun getQueuedJob(id: String): Stored<BackgroundJobQueued>? {
        return queue.get(id = id)
    }

    suspend fun listArchivedJobs(): Cursor<Stored<BackgroundJobArchived>> {
        return archive.list()
    }

    suspend fun listArchivedJobs(page: Int, epp: Int): Cursor<Stored<BackgroundJobArchived>> {
        return archive.list(page = page, epp = epp)
    }

    suspend fun getArchivedJob(id: String): Stored<BackgroundJobArchived>? {
        return archive.get(id = id)
    }

    suspend fun runQueuedJobs(state: StateProvider) {
        do {
            val job = try {
                claimNextDueJob()
            } catch (e: Throwable) {
                log.warning("Could not read next due job\n${e.stackTraceToString()}")
                null
            }

            if (job != null) {
                // Clear the entity cache.
                // The "do"-loop might be running a very long time, we need to avoid stale entries in the cache.
                entityCache?.clear()

                val startedAt = Kronos.systemUtc.instantNow()

                // Get lock on the job
                val profiled = measureCoroutine {
                    try {
                        // We try to get a lock on the job
                        runJob(startedAt, job)
                    } catch (e: LocksException.Execution) {
                        BackgroundJobExecutionResult.Failed(
                            serverId = serverId.getId(),
                            data = mapOf(
                                "error" to e.message,
                                "stack" to e.stackTraceToString()
                            ),
                            startedAt = startedAt,
                            endedAt = Kronos.systemUtc.instantNow(),
                        )
                    }
                }

                val result = profiled.value.withCpuProfile(profiled.profile)

                val withResult = job.modify { it.plusResult(result) }

                when (result) {
                    is BackgroundJobExecutionResult.Failed -> {
                        when (val rescheduled =
                            withResult.value.retryPolicy.scheduleRetry(withResult, MpInstant.now())) {
                            // No retry
                            null -> archiveJob(withResult)
                            // Otherwise, schedule re-try
                            else -> queue.modify(withResult) {
                                it.copy(
                                    dueAt = rescheduled,
                                    state = BackgroundJobQueued.State.WAITING,
                                )
                            }
                        }
                    }

                    is BackgroundJobExecutionResult.Success -> {
                        // Archive the job
                        archiveJob(withResult)
                    }
                }
            }
        } while (job != null && state().isRunning)
    }

    private suspend fun runJob(
        startedAt: MpInstant,
        job: Stored<BackgroundJobQueued>,
    ): BackgroundJobExecutionResult {
        val result = try {
            when (val handler = handlers.firstOrNull { it.canHandle(job.value) }) {
                null -> {
                    BackgroundJobExecutionResult.Failed(
                        serverId = serverId.getId(),
                        data = mapOf(
                            "error" to "No handler found"
                        ),
                        startedAt = startedAt,
                        endedAt = Kronos.systemUtc.instantNow(),
                    )
                }

                else -> {
                    val output = handler.execute(
                        job = job.value,
                        data = codec.awake(handler.dataType.type, job.value.data)
                    )

                    BackgroundJobExecutionResult.Success(
                        serverId = serverId.getId(),
                        data = mapOf(
                            "output" to output.takeUnless { it is Unit }
                        ),
                        startedAt = startedAt,
                        endedAt = Kronos.systemUtc.instantNow(),
                    )
                }
            }
        } catch (t: Throwable) {
            BackgroundJobExecutionResult.Failed(
                serverId = serverId.getId(),
                data = mapOf(
                    "error" to t.message,
                    "stack" to t.stackTraceToString(),
                ),
                startedAt = startedAt,
                endedAt = Kronos.systemUtc.instantNow(),
            )
        }

        return result
    }

    private suspend fun archiveJob(job: Stored<BackgroundJobQueued>) {
        // We remove the job from the queue
        queue.remove(job)
        // We create an entry in the archive
        archive.save(
            job.value.toArchived(
                archivedAt = Kronos.systemUtc.instantNow(),
                expiresAfter = 7.days,
            )
        )
    }
}
