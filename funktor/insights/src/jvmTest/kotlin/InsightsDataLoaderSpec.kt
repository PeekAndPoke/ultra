package io.peekandpoke.funktor.insights

import io.kotest.core.spec.style.StringSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.peekandpoke.funktor.cluster.depot.repos.fs.FileSystemRepository
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Runs against a real [FileSystemRepository] over a temp directory rather than a stub, so path
 * handling, listing order and content reading are the production ones.
 */
class InsightsDataLoaderSpec : StringSpec({

    fun loaderOver(dir: java.io.File): Pair<InsightsDataLoader, FileSystemRepository> {
        val repo = object : FileSystemRepository("insights", dir.absolutePath), InsightsRepository {}

        return InsightsDataLoader(repo) to repo
    }

    fun record(
        vararg collectors: String,
        method: String? = "GET",
        uri: String? = "/api/things",
        status: Int? = 200,
    ) = """
        {
          "formatVersion": 1,
          "ts": "2026-07-31T12:00:00",
          "date": "2026-07-31T12:00:00",
          "startedNs": 1000000,
          "endedNs": 4000000,
          ${method?.let { """"method": "$it",""" } ?: ""}
          ${uri?.let { """"uri": "$it",""" } ?: ""}
          ${status?.let { """"status": $it,""" } ?: ""}
          "collectors": [ ${collectors.joinToString(",")} ]
        }
    """.trimIndent()

    val requestSlice = """
        { "key": "request", "data": {
            "method": { "value": "GET" }, "scheme": "https", "host": "example.com",
            "port": 443, "uri": "/api/things", "headers": {}, "queryParams": {}
        } }
    """.trimIndent()

    val responseSlice = """
        { "key": "response", "data": { "status": { "value": 200, "description": "OK" }, "headers": {} } }
    """.trimIndent()

    "a record with an UNKNOWN collector key round-trips instead of failing" {
        val dir = tempdir()
        val (loader, repo) = loaderOver(dir)

        // A collector the framework has never heard of, carrying a shape it cannot name. This is the
        // open envelope: an app-defined collector must be indistinguishable from a built-in one.
        val exotic = """{ "key": "acme-billing", "data": { "tenant": "acme", "credits": 7 } }"""

        runBlocking {
            repo.putFile("records-2026-07-31/a.json", record(requestSlice, responseSlice, exotic))

            val loaded = loader.load("records-2026-07-31/a.json").shouldNotBeNull()

            loaded.collectors.map { it.key } shouldContainExactly listOf("request", "response", "acme-billing")

            val billing = loaded.collectors.single { it.key == "acme-billing" }.data.jsonObject
            billing["tenant"]?.jsonPrimitive?.content shouldBe "acme"
            billing["credits"] shouldBe JsonPrimitive(7)
        }
    }

    "the summary is read from the stored headline" {
        val dir = tempdir()
        val (loader, repo) = loaderOver(dir)

        runBlocking {
            repo.putFile("records-2026-07-31/a.json", record(requestSlice, responseSlice))

            val summary = loader.list(limit = 10).single()

            summary.method shouldBe "GET"
            summary.url shouldBe "/api/things"
            summary.status shouldBe 200
            summary.durationMs shouldBe 3.0
        }
    }

    "a BRIEF record — headline, no collectors — lists like any other" {
        val dir = tempdir()
        val (loader, repo) = loaderOver(dir)

        runBlocking {
            // This is what InsightsLevel.BRIEF writes. The summary must not need a single slice; that
            // is the whole point of storing the headline rather than digging it out of `request`.
            repo.putFile("records-2026-07-31/brief.json", record(method = "POST", uri = "/api/x", status = 201))

            val summary = loader.list(limit = 10).single()

            summary.method shouldBe "POST"
            summary.url shouldBe "/api/x"
            summary.status shouldBe 201

            loader.load("records-2026-07-31/brief.json").shouldNotBeNull().collectors shouldBe emptyList()
        }
    }

    "a record with no headline lists with null columns rather than failing" {
        val dir = tempdir()
        val (loader, repo) = loaderOver(dir)

        runBlocking {
            repo.putFile(
                "records-2026-07-31/a.json",
                record(responseSlice, method = null, uri = null, status = null),
            )

            val summary = loader.list(limit = 10).single()

            summary.method shouldBe null
            summary.url shouldBe null
            summary.status shouldBe null
        }
    }

    "list respects its limit" {
        val dir = tempdir()
        val (loader, repo) = loaderOver(dir)

        runBlocking {
            repeat(5) { i -> repo.putFile("records-2026-07-31/rec-$i.json", record(requestSlice)) }

            loader.list(limit = 3).size shouldBe 3
            loader.list(limit = 100).size shouldBe 5
        }
    }

    "list walks day folders newest-first" {
        val dir = tempdir()
        val (loader, repo) = loaderOver(dir)

        runBlocking {
            repo.putFile("records-2026-07-29/old.json", record(requestSlice))
            repo.putFile("records-2026-07-31/new.json", record(requestSlice))

            loader.list(limit = 10).map { it.path } shouldContainExactly listOf(
                "records-2026-07-31/new.json",
                "records-2026-07-29/old.json",
            )
        }
    }

    "a missing record is null, not an exception" {
        val dir = tempdir()
        val (loader, _) = loaderOver(dir)

        runBlocking {
            loader.load("records-2026-07-31/nope.json") shouldBe null
        }
    }

    "a corrupt record is null rather than propagating a parse error" {
        val dir = tempdir()
        val (loader, repo) = loaderOver(dir)

        runBlocking {
            repo.putFile("records-2026-07-31/broken.json", "{ this is not json")

            loader.load("records-2026-07-31/broken.json") shouldBe null
            // and it must not take the whole listing down with it
            loader.list(limit = 10) shouldBe emptyList()
        }
    }

    "prev/next are null at the ends and point outward in the middle" {
        val dir = tempdir()
        val (loader, repo) = loaderOver(dir)

        runBlocking {
            // written oldest to newest; listItems sorts by lastModifiedAt descending
            listOf("a", "b", "c").forEach { name ->
                repo.putFile("records-2026-07-31/$name.json", record(requestSlice))
                Thread.sleep(10)
            }

            val newest = loader.load("records-2026-07-31/c.json").shouldNotBeNull()
            val middle = loader.load("records-2026-07-31/b.json").shouldNotBeNull()
            val oldest = loader.load("records-2026-07-31/a.json").shouldNotBeNull()

            newest.nextPath shouldBe null
            newest.previousPath shouldBe "records-2026-07-31/b.json"

            middle.nextPath shouldBe "records-2026-07-31/c.json"
            middle.previousPath shouldBe "records-2026-07-31/a.json"

            oldest.nextPath shouldBe "records-2026-07-31/b.json"
            oldest.previousPath shouldBe null
        }
    }

    "an empty depot lists nothing rather than failing" {
        val dir = tempdir()
        val (loader, _) = loaderOver(dir)

        runBlocking {
            loader.list(limit = 10) shouldBe emptyList()
        }
    }

    "a slice with no data key becomes JsonNull rather than being dropped" {
        val dir = tempdir()
        val (loader, repo) = loaderOver(dir)

        runBlocking {
            repo.putFile("records-2026-07-31/a.json", record("""{ "key": "bare" }"""))

            val loaded = loader.load("records-2026-07-31/a.json").shouldNotBeNull()

            loaded.collectors.single().key shouldBe "bare"
            (loaded.collectors.single().data as? JsonObject) shouldBe null
        }
    }
})
