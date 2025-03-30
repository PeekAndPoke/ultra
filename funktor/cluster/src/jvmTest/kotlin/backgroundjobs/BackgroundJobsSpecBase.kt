package de.peekandpoke.ktorfx.cluster.backgroundjobs

import de.peekandpoke.ktorfx.cluster.backgroundjobs.domain.BackgroundJobQueued
import de.peekandpoke.ktorfx.cluster.backgroundjobs.domain.BackgroundJobRetryPolicy
import de.peekandpoke.ktorfx.cluster.backgroundjobs.example.ExampleBackgroundJobHandler01
import de.peekandpoke.ktorfx.cluster.backgroundjobs.example.ExampleBackgroundJobHandler02
import de.peekandpoke.ktorfx.cluster.workers.WorkersRunner
import de.peekandpoke.ultra.kontainer.Kontainer
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.maps.shouldContainAll
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

abstract class BackgroundJobsSpecBase : FreeSpec() {

    protected val kontainer: Kontainer by lazy {
        createKontainer()
    }

    private val backgroundJobs: BackgroundJobs
        get() = kontainer.get(BackgroundJobs::class)

    private val exampleJob01: ExampleBackgroundJobHandler01
        get() = kontainer.get(ExampleBackgroundJobHandler01::class)

    private val exampleJob02: ExampleBackgroundJobHandler02
        get() = kontainer.get(ExampleBackgroundJobHandler02::class)

    private suspend fun runJobsInBackground(): Job = coroutineScope {
        launch(SupervisorJob()) {
            backgroundJobs.runQueuedJobs { WorkersRunner.State.Running }
        }.also { it.start() }
    }

    private suspend fun runJobsAndWaitForAll() {
        runJobsInBackground().join()
    }

    abstract fun createKontainer(): Kontainer

    abstract fun prepareTest()

    init {
        "queue() different Jobs" {
            prepareTest()

            exampleJob01.queue(ExampleBackgroundJobHandler01.Input(text = "ex01-01"))
            delay(10)
            exampleJob02.queue(ExampleBackgroundJobHandler02.Input(text = "ex02-01"))
            delay(10)
            exampleJob02.queue(ExampleBackgroundJobHandler02.Input(text = "ex02-02"))
            delay(10)

            val jobs = backgroundJobs.listQueuedJobs().map { it.value }

            assertSoftly {
                withClue("There should be three jobs queued") {
                    jobs.shouldHaveSize(3)
                }

                withClue("The first queued job must have the correct properties") {
                    with(jobs[0]) {
                        type shouldBe exampleJob01.jobType
                        @Suppress("UNCHECKED_CAST")
                        (data as Map<String, Any?>) shouldContainAll mapOf(
                            "text" to "ex01-01",
                        )
                    }
                }

                withClue("The second queued job must have the correct properties") {
                    with(jobs[1]) {
                        type shouldBe exampleJob02.jobType
                        @Suppress("UNCHECKED_CAST")
                        (data as Map<String, Any?>) shouldContainAll mapOf(
                            "text" to "ex02-01",
                        )
                    }
                }

                withClue("The third queued job must have the correct properties") {
                    with(jobs[2]) {
                        type shouldBe exampleJob02.jobType
                        @Suppress("UNCHECKED_CAST")
                        (data as Map<String, Any?>) shouldContainAll mapOf(
                            "text" to "ex02-02",
                        )
                    }
                }
            }
        }

        "queueIfNotQueued() different Jobs" {
            prepareTest()

            exampleJob01.queueIfNotPresent(ExampleBackgroundJobHandler01.Input(text = "ex01-01"))
            delay(10)
            exampleJob01.queueIfNotPresent(ExampleBackgroundJobHandler01.Input(text = "ex01-01"))
            delay(10)

            exampleJob02.queue(ExampleBackgroundJobHandler02.Input(text = "ex02-01"))
            delay(10)
            exampleJob02.queue(ExampleBackgroundJobHandler02.Input(text = "ex02-01"))
            delay(10)

            val jobs = backgroundJobs.listQueuedJobs().map { it.value }

            assertSoftly {
                withClue("There should be three jobs queued") {
                    jobs.shouldHaveSize(3)
                }

                withClue("The first queued job must have the correct properties") {
                    with(jobs[0]) {
                        type shouldBe exampleJob01.jobType
                        @Suppress("UNCHECKED_CAST")
                        (data as Map<String, Any?>) shouldContainAll mapOf(
                            "text" to "ex01-01",
                        )
                    }
                }

                withClue("The second queued job must have the correct properties") {
                    with(jobs[1]) {
                        type shouldBe exampleJob02.jobType
                        @Suppress("UNCHECKED_CAST")
                        (data as Map<String, Any?>) shouldContainAll mapOf(
                            "text" to "ex02-01",
                        )
                    }
                }
            }
        }

        "queueIfNotQueued() same Job while Job is already processing" {
            prepareTest()

            val jobData = ExampleBackgroundJobHandler01.Input(execDelayMs = 1000, text = "ex01-01")

            // Queue the job one time
            exampleJob01.queueIfNotPresent(jobData)

            // Start processing it in the background
            val background = runJobsInBackground()

            // We for the job to go into PROCESSING state
            eventually(2000.milliseconds) {
                val queued = backgroundJobs.listQueuedJobs()
                queued.filter { it.value.state == BackgroundJobQueued.State.PROCESSING }.shouldNotBeEmpty()
            }

            // Queue the Job one more time
            exampleJob01.queueIfNotPresent(jobData)

            withClue("The Job must be in the queue twice, even though it has the same data / dataHash") {
                val queuedJobs = backgroundJobs.listQueuedJobs().map { it.value }
                queuedJobs.shouldHaveSize(2)
            }

            // Wait for the jobs to be processed
            background.join()

            withClue("After the queued Jobs are processed there must be no more jobs in the queued") {
                val queuedJobs = backgroundJobs.listQueuedJobs().map { it.value }
                queuedJobs.shouldBeEmpty()
            }

            withClue("After the queued Jobs are processed there must be two jobs in the archive") {
                val queuedJobs = backgroundJobs.listArchivedJobs().map { it.value }
                queuedJobs.shouldHaveSize(2)
            }
        }

        "Retrying failed jobs" - {

            "A job with RetryPolicy.None must be archived after failure" {

                prepareTest()

                val jobData = ExampleBackgroundJobHandler01.Input(
                    execDelayMs = 0,
                    text = "ex01-01-fail-immediate",
                    behaviour = ExampleBackgroundJobHandler01.Input.Behaviour.Fail,
                )

                // Queue the job
                exampleJob01.queue(data = jobData, retryPolicy = BackgroundJobRetryPolicy.None)

                eventually(3.seconds) {
                    // Retry delay
                    delay(100)

                    // Run queued jobs
                    runJobsAndWaitForAll()

                    withClue("The queue must be empty") {
                        val queuedJobs = backgroundJobs.listQueuedJobs().map { it.value }
                        queuedJobs.shouldBeEmpty()
                    }

                    withClue("The archive must contain one failed job") {
                        val archivedJobs = backgroundJobs.listArchivedJobs().map { it.value }
                        archivedJobs.shouldHaveSize(1)
                        archivedJobs[0].results.shouldHaveSize(1)
                        archivedJobs[0].didFinallySucceed() shouldBe false
                    }
                }
            }

            "A job with RetryPolicy.LinearDelay must be archived when finally successful after several retries " {

                /**
                 * See [ExampleBackgroundJobHandler01] for the inner workings of the [ExampleBackgroundJobHandler01.Input.Behaviour]
                 */

                prepareTest()

                val numFailures = 2

                val jobData = ExampleBackgroundJobHandler01.Input(
                    execDelayMs = 0,
                    text = "ex01-01",
                    behaviour = ExampleBackgroundJobHandler01.Input.Behaviour.SucceedAfterNthFailure(failures = numFailures),
                )

                // Queue the job
                val retryDelay = 200L

                exampleJob01.queue(
                    data = jobData,
                    retryPolicy = BackgroundJobRetryPolicy.LinearDelay(delayInMs = retryDelay)
                )

                repeat(numFailures) { attempt ->
                    delay(retryDelay)

                    // Run queued jobs
                    runJobsAndWaitForAll()

                    withClue("The queue must have one element after attempt #${attempt + 1}") {
                        val queuedJobs = backgroundJobs.listQueuedJobs().map { it.value }
                        queuedJobs.shouldHaveSize(1)
                        queuedJobs[0].results.shouldHaveSize(attempt + 1)
                    }
                }

                // Run queued jobs once again to get the successful result
                delay(retryDelay)
                // Run all jobs again
                runJobsAndWaitForAll()

                withClue("The queue must be empty after the final try") {
                    val queuedJobs = backgroundJobs.listQueuedJobs().map { it.value }
                    queuedJobs.shouldBeEmpty()
                }

                withClue("The archive must contain one failed job") {
                    val archivedJobs = backgroundJobs.listArchivedJobs().map { it.value }
                    archivedJobs.shouldHaveSize(1)
                    archivedJobs[0].results.shouldHaveSize(numFailures + 1)
                    archivedJobs[0].didFinallySucceed() shouldBe true
                }
            }

            "A job with RetryPolicy.LinearDelay must be archived when finally failed after several retries " {

                /**
                 * See [ExampleBackgroundJobHandler01] for the inner workings of the [ExampleBackgroundJobHandler01.Input.Behaviour]
                 */

                prepareTest()

                val jobData = ExampleBackgroundJobHandler01.Input(
                    execDelayMs = 0,
                    text = "ex01-01",
                    behaviour = ExampleBackgroundJobHandler01.Input.Behaviour.Fail,
                )

                // Queue the job
                val retryDelay = 200L
                val maxTries = 5
                exampleJob01.queue(
                    data = jobData,
                    retryPolicy = BackgroundJobRetryPolicy.LinearDelay(delayInMs = retryDelay, maxTries = maxTries)
                )

                repeat(maxTries - 1) { attempt ->
                    delay(retryDelay)

                    // Run queued jobs
                    runJobsAndWaitForAll()

                    withClue("The queue must have one element after attempt #${attempt + 1}") {
                        val queuedJobs = backgroundJobs.listQueuedJobs().map { it.value }
                        queuedJobs.shouldHaveSize(1)
                        queuedJobs[0].results.shouldHaveSize(attempt + 1)
                    }
                }

                // Run queued jobs once again to get the successful result
                delay(retryDelay)
                // Run all jobs again
                runJobsAndWaitForAll()

                withClue("The queue must be empty after the final try") {
                    val queuedJobs = backgroundJobs.listQueuedJobs().map { it.value }
                    queuedJobs.shouldBeEmpty()
                }

                withClue("The archive must contain one failed job") {
                    val archivedJobs = backgroundJobs.listArchivedJobs().map { it.value }
                    archivedJobs.shouldHaveSize(1)
                    archivedJobs[0].results.shouldHaveSize(maxTries)
                    archivedJobs[0].didFinallySucceed() shouldBe false
                }
            }
        }
    }
}
