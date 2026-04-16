package io.peekandpoke.funktor.logging

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.peekandpoke.funktor.inspect.logging.LogsFilter
import io.peekandpoke.funktor.inspect.logging.api.LogEntryModel
import io.peekandpoke.funktor.inspect.logging.api.LogsRequest
import io.peekandpoke.funktor.logging.karango.KarangoLogEntry
import io.peekandpoke.ultra.kontainer.Kontainer
import io.peekandpoke.ultra.kontainer.KontainerBuilder
import io.peekandpoke.ultra.log.LogLevel
import io.peekandpoke.ultra.vault.Database
import io.peekandpoke.ultra.vault.Repository

abstract class LogsStorageBaseSpec : FreeSpec() {

    abstract fun KontainerBuilder.configureKontainer()

    abstract fun FunktorLoggingBuilder.configureLogging()

    suspend fun createKontainer(): Kontainer {
        return createLoggingTestContainer(
            configureKontainer = { configureKontainer() },
            configureLogging = { configureLogging() },
        )
    }

    private suspend fun insertEntry(repo: Repository<KarangoLogEntry>, entry: KarangoLogEntry) {
        repo.insert(entry)
    }

    init {
        "Inserting and finding a log entry by id" {
            val kontainer = createKontainer()
            val storage = kontainer.get(LogsStorage::class)
            val database = kontainer.get(Database::class)

            // Clean up
            database.getRepositories().forEach { it.removeAll() }

            // Get the log repository and insert directly
            @Suppress("UNCHECKED_CAST")
            val repo = database.getRepositories()
                .first { it.name.contains("log") } as Repository<KarangoLogEntry>

            insertEntry(
                repo, KarangoLogEntry(
                    createdAt = System.currentTimeMillis(),
                    level = LogLevel.ERROR,
                    message = "Test error message",
                    loggerName = "test.logger",
                )
            )

            // Find all entries
            val filter = LogsFilter(
                search = "",
                minLevel = LogLevel.DEBUG,
                state = emptySet(),
                page = 0,
                epp = 100,
            )
            val result = storage.find(filter)

            result.items.size shouldBe 1

            val entry = result.items.first()
            entry.level shouldBe LogLevel.ERROR
            entry.message shouldBe "Test error message"
            entry.loggerName shouldBe "test.logger"

            // Find by id
            val found = storage.getById(entry.id)
            found.shouldNotBeNull()
            found.id shouldBe entry.id
            found.message shouldBe "Test error message"
        }

        "Finding log entries filtered by severity" {
            val kontainer = createKontainer()
            val storage = kontainer.get(LogsStorage::class)
            val database = kontainer.get(Database::class)

            // Clean up
            database.getRepositories().forEach { it.removeAll() }

            @Suppress("UNCHECKED_CAST")
            val repo = database.getRepositories()
                .first { it.name.contains("log") } as Repository<KarangoLogEntry>

            insertEntry(
                repo, KarangoLogEntry(
                    createdAt = System.currentTimeMillis(),
                    level = LogLevel.INFO,
                    message = "Info message",
                    loggerName = "test.logger",
                )
            )
            insertEntry(
                repo, KarangoLogEntry(
                    createdAt = System.currentTimeMillis(),
                    level = LogLevel.WARNING,
                    message = "Warning message",
                    loggerName = "test.logger",
                )
            )
            insertEntry(
                repo, KarangoLogEntry(
                    createdAt = System.currentTimeMillis(),
                    level = LogLevel.ERROR,
                    message = "Error message",
                    loggerName = "test.logger",
                )
            )

            // Filter for WARNING and above
            val filter = LogsFilter(
                search = "",
                minLevel = LogLevel.WARNING,
                state = emptySet(),
                page = 0,
                epp = 100,
            )
            val result = storage.find(filter)

            result.items.size shouldBe 2
            result.items.all { it.severity >= LogLevel.WARNING.severity } shouldBe true
        }

        "Finding log entries filtered by search text" {
            val kontainer = createKontainer()
            val storage = kontainer.get(LogsStorage::class)
            val database = kontainer.get(Database::class)

            // Clean up
            database.getRepositories().forEach { it.removeAll() }

            @Suppress("UNCHECKED_CAST")
            val repo = database.getRepositories()
                .first { it.name.contains("log") } as Repository<KarangoLogEntry>

            insertEntry(
                repo, KarangoLogEntry(
                    createdAt = System.currentTimeMillis(),
                    level = LogLevel.ERROR,
                    message = "Database connection failed",
                    loggerName = "db.connector",
                )
            )
            insertEntry(
                repo, KarangoLogEntry(
                    createdAt = System.currentTimeMillis(),
                    level = LogLevel.ERROR,
                    message = "Authentication error",
                    loggerName = "auth.service",
                )
            )

            // Search for "database"
            val filter = LogsFilter(
                search = "database",
                minLevel = LogLevel.DEBUG,
                state = emptySet(),
                page = 0,
                epp = 100,
            )
            val result = storage.find(filter)

            result.items.size shouldBe 1
            result.items.first().message shouldBe "Database connection failed"
        }

        "update() changes the state of an existing log entry" {
            val kontainer = createKontainer()
            val storage = kontainer.get(LogsStorage::class)
            val database = kontainer.get(Database::class)

            database.getRepositories().forEach { it.removeAll() }

            @Suppress("UNCHECKED_CAST")
            val repo = database.getRepositories()
                .first { it.name.contains("log") } as Repository<KarangoLogEntry>

            insertEntry(
                repo, KarangoLogEntry(
                    createdAt = System.currentTimeMillis(),
                    level = LogLevel.ERROR,
                    message = "update me",
                    loggerName = "test",
                )
            )

            val found = storage.find(defaultFilter()).items.first()
            found.state shouldBe LogEntryModel.State.New

            val updated = storage.update(found.copy(state = LogEntryModel.State.Ack))
            updated.state shouldBe LogEntryModel.State.Ack

            // Re-read from storage to confirm persistence
            storage.getById(found.id)?.state shouldBe LogEntryModel.State.Ack
        }

        "execBulkAction() applies SetState across matching entries" {
            val kontainer = createKontainer()
            val storage = kontainer.get(LogsStorage::class)
            val database = kontainer.get(Database::class)

            database.getRepositories().forEach { it.removeAll() }

            @Suppress("UNCHECKED_CAST")
            val repo = database.getRepositories()
                .first { it.name.contains("log") } as Repository<KarangoLogEntry>

            val now = System.currentTimeMillis()
            repeat(3) { i ->
                insertEntry(
                    repo, KarangoLogEntry(
                        createdAt = now + i,
                        level = LogLevel.ERROR,
                        message = "bulk-$i",
                        loggerName = "test",
                    )
                )
            }

            val response = storage.execBulkAction(
                LogsRequest.BulkAction(
                    filter = LogsRequest.BulkAction.Filter(from = null, to = null, states = null),
                    action = LogsRequest.Action.SetState(LogEntryModel.State.Ack),
                )
            )

            response.numChanged shouldBe 3

            val after = storage.find(defaultFilter()).items
            after.shouldHaveSize(3)
            after.all { it.state == LogEntryModel.State.Ack } shouldBe true
        }

        "find() paginates results" {
            // Note: page=0 vs page=1 semantics differ between backends (tracked as LOW-severity
            // in the v1 audit) — Karango `PAGE` is 1-based, Monko is 0-based. This test walks
            // a superset of pages and asserts that every inserted id is reached at least once,
            // avoiding the inconsistency.
            val kontainer = createKontainer()
            val storage = kontainer.get(LogsStorage::class)
            val database = kontainer.get(Database::class)

            database.getRepositories().forEach { it.removeAll() }

            @Suppress("UNCHECKED_CAST")
            val repo = database.getRepositories()
                .first { it.name.contains("log") } as Repository<KarangoLogEntry>

            val base = System.currentTimeMillis()
            repeat(5) { i ->
                insertEntry(
                    repo, KarangoLogEntry(
                        createdAt = base + i,
                        level = LogLevel.ERROR,
                        message = "page-$i",
                        loggerName = "test",
                    )
                )
            }

            val collected = (0..3).flatMap { page ->
                storage.find(defaultFilter().copy(page = page, epp = 2)).items
            }.map { it.id }.toSet()

            collected shouldHaveSize 5
        }
    }

    private fun defaultFilter() = LogsFilter(
        search = "",
        minLevel = LogLevel.DEBUG,
        state = emptySet(),
        page = 0,
        epp = 100,
    )
}
