package io.peekandpoke.ultra.vault.profiling

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class QueryProfilerSpec : StringSpec({

    // StopWatch.Null //////////////////////////////////////////////////////////////////////////////

    "StopWatch.Null has empty entriesNs" {
        QueryProfiler.StopWatch.Null.entriesNs.shouldBeEmpty()
    }

    "StopWatch.Null has totalNs of 0" {
        QueryProfiler.StopWatch.Null.totalNs shouldBe 0
    }

    "StopWatch.Null has count of 0" {
        QueryProfiler.StopWatch.Null.count shouldBe 0
    }

    "StopWatch.Null invoke executes block and returns result" {
        val result = QueryProfiler.StopWatch.Null { 42 }

        result shouldBe 42
    }

    "StopWatch.Null async executes block and returns result" {
        val result = QueryProfiler.StopWatch.Null.async { "hello" }

        result shouldBe "hello"
    }

    // StopWatch.Impl //////////////////////////////////////////////////////////////////////////////

    "StopWatch.Impl starts with empty entriesNs" {
        val sw = QueryProfiler.StopWatch.Impl()

        sw.entriesNs.shouldBeEmpty()
        sw.totalNs shouldBe 0
        sw.count shouldBe 0
    }

    "StopWatch.Impl invoke records timing and returns result" {
        val sw = QueryProfiler.StopWatch.Impl()

        val result = sw { "result" }

        result shouldBe "result"
        sw.count shouldBe 1
        sw.entriesNs.size shouldBe 1
        (sw.totalNs >= 0) shouldBe true
    }

    "StopWatch.Impl invoke called multiple times accumulates entries" {
        val sw = QueryProfiler.StopWatch.Impl()

        sw { 1 }
        sw { 2 }
        sw { 3 }

        sw.count shouldBe 3
        sw.entriesNs.size shouldBe 3
    }

    "StopWatch.Impl async records timing and returns result" {
        val sw = QueryProfiler.StopWatch.Impl()

        val result = sw.async { "async-result" }

        result shouldBe "async-result"
        sw.count shouldBe 1
        (sw.totalNs >= 0) shouldBe true
    }

    // Entry.Null //////////////////////////////////////////////////////////////////////////////////

    "Entry.Null has null properties and setting them is a no-op" {
        val entry = QueryProfiler.Entry.Null

        entry.query shouldBe null
        entry.vars shouldBe null
        entry.count shouldBe null
        entry.totalCount shouldBe null
        entry.queryExplained shouldBe null

        // Setting should be a no-op
        entry.query = "SELECT 1"
        entry.vars = mapOf("x" to 1)
        entry.count = 10
        entry.totalCount = 100
        entry.queryExplained = "explained"

        entry.query shouldBe null
        entry.vars shouldBe null
        entry.count shouldBe null
        entry.totalCount shouldBe null
        entry.queryExplained shouldBe null
    }

    "Entry.Null stopwatches are StopWatch.Null" {
        val entry = QueryProfiler.Entry.Null

        entry.measureQuery shouldBe QueryProfiler.StopWatch.Null
        entry.measureIterator shouldBe QueryProfiler.StopWatch.Null
        entry.measureSerializer shouldBe QueryProfiler.StopWatch.Null
        entry.measureDeserializer shouldBe QueryProfiler.StopWatch.Null
        entry.measureExplain shouldBe QueryProfiler.StopWatch.Null
    }

    "Entry.Null totalNs is 0" {
        QueryProfiler.Entry.Null.totalNs shouldBe 0
    }

    // Entry.Impl //////////////////////////////////////////////////////////////////////////////////

    "Entry.Impl stores connection, queryLanguage, and query" {
        val entry = QueryProfiler.Entry.Impl(
            connection = "mydb",
            queryLanguage = "AQL",
            query = "FOR doc IN col RETURN doc",
        )

        entry.connection shouldBe "mydb"
        entry.queryLanguage shouldBe "AQL"
        entry.query shouldBe "FOR doc IN col RETURN doc"
    }

    "Entry.Impl mutable properties can be set" {
        val entry = QueryProfiler.Entry.Impl(connection = "db", queryLanguage = "AQL")

        entry.vars = mapOf("x" to 1)
        entry.count = 5
        entry.totalCount = 50
        entry.queryExplained = "plan"

        entry.vars shouldBe mapOf("x" to 1)
        entry.count shouldBe 5
        entry.totalCount shouldBe 50
        entry.queryExplained shouldBe "plan"
    }

    "Entry.Impl stopwatches are StopWatch.Impl instances" {
        val entry = QueryProfiler.Entry.Impl(connection = "db", queryLanguage = "AQL")

        entry.measureQuery.shouldBeInstanceOf<QueryProfiler.StopWatch.Impl>()
        entry.measureIterator.shouldBeInstanceOf<QueryProfiler.StopWatch.Impl>()
        entry.measureSerializer.shouldBeInstanceOf<QueryProfiler.StopWatch.Impl>()
        entry.measureDeserializer.shouldBeInstanceOf<QueryProfiler.StopWatch.Impl>()
        entry.measureExplain.shouldBeInstanceOf<QueryProfiler.StopWatch.Impl>()
    }

    // NullQueryProfiler ///////////////////////////////////////////////////////////////////////////

    "NullQueryProfiler has explainQueries=false and empty entries" {
        NullQueryProfiler.explainQueries shouldBe false
        NullQueryProfiler.entries.shouldBeEmpty()
    }

    "NullQueryProfiler profile passes Entry.Null to block and returns result" {
        val result = NullQueryProfiler.profile("conn", "AQL", "query") { entry ->
            entry shouldBe QueryProfiler.Entry.Null
            "done"
        }

        result shouldBe "done"
    }

    // DefaultQueryProfiler ////////////////////////////////////////////////////////////////////////

    "DefaultQueryProfiler starts with empty entries" {
        val profiler = DefaultQueryProfiler(explainQueries = false)

        profiler.entries.shouldBeEmpty()
    }

    "DefaultQueryProfiler profile creates an Entry.Impl and adds it to entries" {
        val profiler = DefaultQueryProfiler(explainQueries = true)

        val result = profiler.profile("conn", "AQL", "FOR x IN col RETURN x") { entry ->
            entry.shouldBeInstanceOf<QueryProfiler.Entry.Impl>()
            entry.query shouldBe "FOR x IN col RETURN x"
            "result-value"
        }

        result shouldBe "result-value"
        profiler.entries.size shouldBe 1
        profiler.explainQueries shouldBe true
    }

    "DefaultQueryProfiler profile accumulates entries across multiple calls" {
        val profiler = DefaultQueryProfiler(explainQueries = false)

        profiler.profile("conn1", "AQL", "q1") { "r1" }
        profiler.profile("conn2", "AQL", "q2") { "r2" }
        profiler.profile("conn3", "AQL", "q3") { "r3" }

        profiler.entries.size shouldBe 3
    }
})
