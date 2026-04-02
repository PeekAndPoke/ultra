package io.peekandpoke.funktor.logging

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.peekandpoke.funktor.inspect.logging.LogsFilter
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
    }
}
