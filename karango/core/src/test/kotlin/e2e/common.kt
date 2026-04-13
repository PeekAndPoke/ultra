package io.peekandpoke.karango.e2e

import com.arangodb.ArangoDatabaseAsync
import io.kotest.assertions.withClue
import io.peekandpoke.karango.aql.AqlExpression
import io.peekandpoke.karango.aql.AqlPrinter.Companion.print
import io.peekandpoke.karango.config.ArangoDbConfig
import io.peekandpoke.karango.slumber.KarangoCodec
import io.peekandpoke.karango.testdomain.TestPersonsRepository
import io.peekandpoke.karango.testdomain.TestTimestampedRepository
import io.peekandpoke.karango.toArangoDb
import io.peekandpoke.karango.vault.KarangoDriver
import io.peekandpoke.ultra.common.surround
import io.peekandpoke.ultra.datetime.Kronos
import io.peekandpoke.ultra.slumber.SlumberConfig
import io.peekandpoke.ultra.vault.Database
import io.peekandpoke.ultra.vault.DefaultEntityCache
import io.peekandpoke.ultra.vault.Repository
import io.peekandpoke.ultra.vault.hooks.TimestampedHook
import io.peekandpoke.ultra.vault.slumber.VaultSlumberModule
import kotlinx.coroutines.runBlocking

private val arangoConfig: ArangoDbConfig = ArangoDbConfig.forUnitTests
private val arangoDatabase: ArangoDatabaseAsync = arangoConfig.toArangoDb()

val kronos = Kronos.systemUtc

fun createDatabase(repos: (driver: KarangoDriver) -> List<Repository<*>>): Pair<Database, KarangoDriver> = runBlocking {

    lateinit var driver: KarangoDriver

    val db: Database = Database.of { repos(driver) }

    val codec = KarangoCodec(
        config = SlumberConfig.default.prependModules(VaultSlumberModule),
        database = db,
        entityCache = DefaultEntityCache()
    )

    driver = KarangoDriver(
        lazyCodec = lazy { codec },
        lazyArangoDb = lazy { arangoDatabase },
    )

    db.ensureRepositories()

    Pair(db, driver)
}

private val dbAndDriver = createDatabase { driver ->
    listOf(
        TestPersonsRepository(driver = driver),
        TestTimestampedRepository(driver = driver, timestamped = TimestampedHook(lazy { kronos })),
    )
}

val database: Database = dbAndDriver.first
val karangoDriver: KarangoDriver = dbAndDriver.second

suspend fun <T, R : Any> withDetailedClue(expression: AqlExpression<T>, expected: Any?, block: suspend () -> R): R {

    val printerResult = expression.print()

    val clue = listOf(
        "Type:     $expression",
        "AQL:      ${printerResult.query}",
        "Vars:     ${printerResult.vars}",
        "Readable: ${printerResult.raw}",
        "Expected: $expected"
    ).joinToString("\n").surround("\n")

    return withClue(clue) { block() }
}
