package de.peekandpoke.karango.e2e

import com.arangodb.ArangoDatabaseAsync
import de.peekandpoke.karango.Karango
import de.peekandpoke.karango.aql.print
import de.peekandpoke.karango.config.ArangoDbConfig
import de.peekandpoke.karango.slumber.KarangoCodec
import de.peekandpoke.karango.testdomain.TestPersonsRepository
import de.peekandpoke.karango.testdomain.TestTimestampedRepository
import de.peekandpoke.karango.toArangoDb
import de.peekandpoke.karango.vault.KarangoDriver
import de.peekandpoke.ultra.common.datetime.Kronos
import de.peekandpoke.ultra.common.surround
import de.peekandpoke.ultra.slumber.SlumberConfig
import de.peekandpoke.ultra.vault.Database
import de.peekandpoke.ultra.vault.DefaultEntityCache
import de.peekandpoke.ultra.vault.Repository
import de.peekandpoke.ultra.vault.hooks.TimestampedHook
import de.peekandpoke.ultra.vault.lang.Expression
import de.peekandpoke.ultra.vault.slumber.VaultSlumberModule
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.scopes.TerminalScope
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

@Karango
data class E2ePerson(val name: String, val age: Int)

@Suppress("unused", "UnusedReceiverParameter")
fun <T, R : Any> TerminalScope.withDetailedClue(expression: Expression<T>, expected: Any?, block: () -> R): R {

    val printerResult = expression.print()

    return withClue(
        listOf(
            "Type:     $expression",
            "AQL:      ${printerResult.query}",
            "Vars:     ${printerResult.vars}",
            "Readable: ${printerResult.raw}",
            "Expected: $expected"
        ).joinToString("\n").surround("\n"),
        block
    )
}
