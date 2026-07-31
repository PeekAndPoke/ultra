package io.peekandpoke.funktor.insights.collectors

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.maps.shouldNotContainKey
import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import io.peekandpoke.funktor.insights.HeaderAction
import io.peekandpoke.funktor.insights.HeaderLogging

/**
 * Drives the collectors through a **real `ApplicationCall`**.
 *
 * `HeaderLoggingSpec` proves the policy decides correctly; nothing proved the collectors ever ask it.
 * That gap is not academic — restoring `headers = call.request.headers.toMap()`, the exact behaviour
 * this work removed, left the entire suite green while `Set-Cookie` went back into every login record.
 * A policy that is never invoked redacts nothing.
 */
class CollectorRedactionSpec : StringSpec({

    /**
     * Collects from a real call and returns the result **for assertion in the test body**.
     *
     * Deliberately NOT asserting inside the route handler: ktor catches whatever a handler throws and
     * turns it into a 500, so an assertion failure there is swallowed and the test passes regardless.
     * An earlier version of this spec did exactly that and stayed green when redaction was removed —
     * caught by mutation, which is the only reason it is written this way.
     */
    fun <T> collectFrom(
        headers: Map<String, String> = emptyMap(),
        query: String = "",
        collect: (ApplicationCall) -> T,
    ): T {
        // Nullable + a check afterwards, not lateinit: a generic T has a nullable upper bound, and this
        // also proves the handler ran at all rather than the request quietly 404ing.
        var captured: T? = null

        testApplication {
            routing {
                get("/probe") {
                    captured = collect(call)
                    call.respondText("ok")
                }
            }

            client.get("/probe$query") {
                headers.forEach { (k, v) -> header(k, v) }
            }
        }

        return captured ?: error("the probe route never ran — the request did not reach the handler")
    }

    "RequestCollector redacts credential-bearing headers" {
        val data = collectFrom(
            headers = mapOf(
                "Authorization" to "Bearer SECRET-TOKEN",
                "Cookie" to "session=SECRET-SESSION",
                "User-Agent" to "curl/8",
            )
        ) { RequestCollector(HeaderLogging.defaults).finish(it) }

        data.headers["Authorization"] shouldBe listOf(HeaderLogging.REDACTED)
        data.headers["Cookie"] shouldBe listOf(HeaderLogging.REDACTED)
        // an innocuous header is untouched, so this is redaction and not blanket removal
        data.headers["User-Agent"] shouldBe listOf("curl/8")

        data.toString().contains("SECRET-TOKEN") shouldBe false
        data.toString().contains("SECRET-SESSION") shouldBe false
    }

    "RequestCollector redacts credentials in the query string" {
        val data = collectFrom(query = "?token=SECRET-VALUE&page=2") {
            RequestCollector(HeaderLogging.defaults).finish(it)
        }

        data.queryParams["token"] shouldBe listOf(HeaderLogging.REDACTED)
        data.queryParams["page"] shouldBe listOf("2")
        data.toString().contains("SECRET-VALUE") shouldBe false
    }

    "the stored uri carries no query string" {
        // `request.uri` includes the query, so storing it verbatim put the token in the record AND in
        // the summary url the list endpoint renders.
        val data = collectFrom(query = "?token=SECRET-VALUE") {
            RequestCollector(HeaderLogging.defaults).finish(it)
        }

        data.uri shouldBe "/probe"
        data.uri.contains("SECRET-VALUE") shouldBe false
    }

    "an application policy reaches the collector" {
        val headers = mapOf("X-Acme-Key" to "SECRET-VALUE")

        // `x-acme-key` matches no default rule, so this proves the INJECTED policy is the one used —
        // which is what "the deny-list must be extensible by the application" actually requires.
        val withPolicy = collectFrom(headers) {
            RequestCollector(HeaderLogging.defaults.with("x-acme-key", HeaderAction.REDACT)).finish(it)
        }
        withPolicy.headers["X-Acme-Key"] shouldBe listOf(HeaderLogging.REDACTED)

        // and under the DEFAULT policy it is not redacted, so the assertion above cannot pass by luck
        val withDefaults = collectFrom(headers) { RequestCollector(HeaderLogging.defaults).finish(it) }
        withDefaults.headers["X-Acme-Key"] shouldBe listOf("SECRET-VALUE")
    }

    "a DROP rule removes the header from the collected data" {
        val data = collectFrom(mapOf("X-Noise" to "spam")) {
            RequestCollector(HeaderLogging.defaults.with("x-noise", HeaderAction.DROP)).finish(it)
        }

        data.headers shouldNotContainKey "X-Noise"
    }

    "ResponseCollector redacts Set-Cookie — a login record must not carry a session" {
        lateinit var data: ResponseCollector.Data

        testApplication {
            routing {
                get("/login") {
                    call.response.headers.append("Set-Cookie", "session=SECRET-SESSION; HttpOnly")
                    call.response.headers.append("X-Trace", "abc")

                    data = ResponseCollector(HeaderLogging.defaults).finish(call)

                    call.respondText("ok")
                }
            }

            client.get("/login")
        }

        data.headers["Set-Cookie"] shouldBe listOf(HeaderLogging.REDACTED)
        data.headers["X-Trace"] shouldBe listOf("abc")
        data.toString().contains("SECRET-SESSION") shouldBe false
    }
})
