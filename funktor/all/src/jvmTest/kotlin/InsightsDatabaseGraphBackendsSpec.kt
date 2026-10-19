package io.peekandpoke.funktor

import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldMatch
import io.kotest.matchers.string.shouldNotContain
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.peekandpoke.funktor.core.AppKontainers
import io.peekandpoke.funktor.core.funktorApp
import io.peekandpoke.funktor.insights.api.InsightsApiFeature
import io.peekandpoke.funktor.insights.api.InsightsDatabaseGraph
import io.peekandpoke.funktor.testing.AppSpec
import io.peekandpoke.karango.config.ArangoDbConfig
import io.peekandpoke.karango.karango
import io.peekandpoke.karango.vault.EntityRepository
import io.peekandpoke.karango.vault.KarangoDriver
import io.peekandpoke.monko.MongoDbConfig
import io.peekandpoke.monko.MonkoDriver
import io.peekandpoke.monko.MonkoRepository
import io.peekandpoke.monko.monko
import io.peekandpoke.ultra.kontainer.kontainer
import io.peekandpoke.ultra.reflection.kType
import io.peekandpoke.ultra.security.jwt.JwtGenerator
import io.peekandpoke.ultra.security.jwt.JwtUserData
import io.peekandpoke.ultra.security.user.EmailAddress
import io.peekandpoke.ultra.security.user.UserId
import io.peekandpoke.ultra.security.user.UserPermissions
import io.peekandpoke.ultra.vault.Ref
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import java.io.File

//  One repository per backend, and a reference from one to the other  /////////////////////////////////

data class GraphE2eAuthor(val name: String)

/** Stored in Mongo, referencing an Arango-stored author and a type no repository stores. */
data class GraphE2eBook(val title: String, val author: Ref<GraphE2eAuthor>, val draft: Ref<GraphE2eUnstored>?)

data class GraphE2eUnstored(val text: String)

class GraphE2eAuthorsRepo(driver: KarangoDriver) : EntityRepository<GraphE2eAuthor>(
    name = "insights_graph_e2e_authors",
    storedType = kType(),
    driver = driver,
)

class GraphE2eBooksRepo(driver: MonkoDriver) : MonkoRepository<GraphE2eBook>(
    name = "insights_graph_e2e_books",
    storedType = kType(),
    driver = driver,
)

private fun createDatabaseGraphBlueprint(config: FunktorAllTestConfig) = kontainer {
    funktor(
        config = config,
        rest = { jwt() },
        logging = { useKarango() },
        cluster = { useKarango() },
        messaging = { useKarango() },
        saas = { useKarango() },
        auth = { useKarango() },
    )

    karango(config = ArangoDbConfig.forUnitTests)
    monko(config = MongoDbConfig.forUnitTests)

    module(TestUserModule)

    dynamic(GraphE2eAuthorsRepo::class)
    dynamic(GraphE2eBooksRepo::class)
}

private val databaseGraphTestApp = funktorApp<FunktorAllTestConfig>(
    kontainers = { config -> AppKontainers(createDatabaseGraphBlueprint(config)) },
)

/**
 * The database graph against BOTH backends in one app.
 *
 * Not backend-independent, although it is mostly reflection: each repository's `connection` is read
 * off its driver, which asks the SERVER for its version (Arango `version`, Mongo `buildInfo`). So each
 * backend's repositories have to be listed and serialised for real.
 */
class InsightsDatabaseGraphBackendsSpec : AppSpec<FunktorAllTestConfig>(databaseGraphTestApp) {

    private val api by service(InsightsApiFeature::class)
    private val jwt by service(JwtGenerator::class)

    private fun token(permissions: UserPermissions): String = jwt.createJwt(
        user = JwtUserData(
            id = UserId("graph-e2e"),
            desc = "graph e2e",
            type = TestUser.USER_TYPE,
            email = EmailAddress("graph-e2e@test.com"),
        ),
        permissions = permissions,
    )

    private suspend fun fetchGraph(): InsightsDatabaseGraph {
        var body = ""

        testApp(host = "api.funktor.local").apply {
            authenticate(token(UserPermissions(isSuperUser = true))) {
                request(api.insights.getDatabaseGraph) {
                    status shouldBe HttpStatusCode.OK
                    body = bodyAsText()
                }
            }
        }

        // opt-in, like the other insights specs: a real response to re-read the contract from
        System.getenv("INSIGHTS_WIRE_SAMPLES_DIR")?.let { File(it, "database-graph-two-backends.json").writeText(body) }

        return Json.decodeFromJsonElement(
            InsightsDatabaseGraph.serializer(),
            Json.parseToJsonElement(body).jsonObject["data"].shouldNotBeNull(),
        )
    }

    private fun InsightsDatabaseGraph.repo(name: String) = repos.single { it.id.name == name }

    init {
        "an Arango repository is listed with an ArangoDB connection" {
            val authors = fetchGraph().repo("insights_graph_e2e_authors")

            authors.connection shouldMatch Regex("ArangoDB\\(.+\\)::.+")
            authors.id.connection shouldBe authors.connection
            authors.storedClasses.map { it.fqn } shouldBe listOf(GraphE2eAuthor::class.java.name)
        }

        "a Mongo repository is listed with a MongoDB connection" {
            val books = fetchGraph().repo("insights_graph_e2e_books")

            books.connection shouldMatch Regex("MongoDB\\(.+\\)::.+")
            books.id.connection shouldBe books.connection
        }

        "a reference from the Mongo repository resolves to the Arango one" {
            val graph = fetchGraph()
            val book = graph.repo("insights_graph_e2e_books").storedClasses.single()

            val toAuthor = book.references.single { it.fqn == GraphE2eAuthor::class.java.name }
            toAuthor.repo shouldBe graph.repo("insights_graph_e2e_authors").id
            toAuthor.type shouldBe "Direct"

            withClue("a reference to a type no repository stores leaves the known graph: repo is null") {
                book.references.single { it.fqn == GraphE2eUnstored::class.java.name }.repo shouldBe null
            }
        }

        "a non-superuser gets the floor's 401 here too, and no repository names" {
            testApp(host = "api.funktor.local").apply {
                authenticate(token(UserPermissions())) {
                    request(api.insights.getDatabaseGraph) {
                        status shouldBe HttpStatusCode.Unauthorized
                        bodyAsText() shouldNotContain "insights_graph_e2e_books"
                    }
                }
            }
        }
    }
}
