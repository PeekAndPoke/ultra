package io.peekandpoke.funktor.rest.codec

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.peekandpoke.funktor.core.broker.CouldNotConvertException
import io.peekandpoke.ultra.datetime.MpZonedDateTime
import io.peekandpoke.ultra.reflection.kType
import io.peekandpoke.ultra.slumber.SlumberConfig

/**
 * A request body is CLIENT-supplied, so a value REJECTED by a type's `init { require(...) }` is a
 * client error (400) — not an internal server error.
 *
 * Value classes are constructed reflectively while awaking, so the failure arrives wrapped in a
 * `java.lang.reflect.InvocationTargetException`, which is neither an `AwakerException` (slumber
 * would handle it) nor a [CouldNotConvertException] (the status pages map it to 400). Left alone it
 * became a 500 plus an internal-error log line per request — and outside production the status page
 * echoes the stack trace back to the caller. The URI-param path already normalizes this case
 * (`IncomingValueClassConverter` → fail-closed 404); this pins the body path.
 */
class SlumberRestCodecSpec : StringSpec({

    val codec = SlumberRestCodec(SlumberConfig.default)

    "a valid body deserializes normally" {
        codec.deserialize(kType<Body>().type, """{"id":"ok","note":"n"}""") shouldBe
                Body(id = VcId("ok"), note = "n")
    }

    "a value class REJECTING the value yields CouldNotConvertException (→ 400, not 500)" {
        shouldThrow<CouldNotConvertException> {
            codec.deserialize(kType<Body>().type, """{"id":"","note":"n"}""")
        }
    }

    "the rejection message is carried through so the client learns what was wrong" {
        val thrown = shouldThrow<CouldNotConvertException> {
            codec.deserialize(kType<Body>().type, """{"id":"  ","note":"n"}""")
        }

        thrown.message shouldBe
                "Could not convert request body to '${kType<Body>().type}': VcId must not be blank"
    }

    // ULTRA-00292: these were a 500 whose answer and log line quoted the whole body, a cleartext password included.
    val secret = "Secret-Marker-4711"

    "a body MISSING a required field is a client error, whose message names the path but no value" {
        val thrown = shouldThrow<CouldNotConvertException> {
            codec.deserialize(kType<Login>().type, """{"password":"$secret"}""")
        }

        // Slumber names the object the field is missing from, not the field.
        thrown.message shouldBe "Could not convert request body to '${kType<Login>().type}': Value at path 'root' must not be null"
        thrown.message shouldNotContain secret
        withClue("the input stays in the cause, for the stack trace only a non-production answer shows") {
            thrown.stackTraceToString() shouldContain secret
        }
    }

    "a WRONG-TYPED field is a client error, whose message names no value" {
        val thrown = shouldThrow<CouldNotConvertException> {
            // The secret INSIDE the failing node: that is what Slumber's "Input value:" quotes.
            codec.deserialize(kType<Login>().type, """{"email":{"nested":"$secret"},"password":"x"}""")
        }

        thrown.message shouldNotContain secret
    }

    "a failure inside a MAP names no path: the raw key is part of it" {
        val thrown = shouldThrow<CouldNotConvertException> {
            codec.deserialize(kType<Attrs>().type, """{"attrs":{"$secret.with.dots":null}}""")
        }

        thrown.message shouldBe
                "Could not convert request body to '${kType<Attrs>().type}': a value inside a map is missing or of the wrong type"
    }

    "a body that is NOT JSON is a client error, whose message does not quote it" {
        val thrown = shouldThrow<CouldNotConvertException> {
            codec.deserialize(kType<Login>().type, """{"email":"a@b.c","password":"$secret" oops""")
        }

        thrown.message shouldBe "Could not convert request body to '${kType<Login>().type}': the body is not valid JSON"
    }

    "a require() in a plain data class init is normalized the same way" {
        shouldThrow<CouldNotConvertException> {
            codec.deserialize(kType<GuardedBody>().type, """{"amount":-1}""")
        }
    }

    // ULTRA-00294: the date awakers threw on a zone they could not read, which made this a 500.
    "a date whose zone is unknown, missing or not a string is a client error, naming the path and not the value" {
        listOf(""""$secret"""", "42", null).forEach { zone ->
            val tz = zone?.let { ""","timezone":$it""" } ?: ""
            val thrown = shouldThrow<CouldNotConvertException> {
                codec.deserialize(kType<Meeting>().type, """{"at":{"ts":1649116800000$tz}}""")
            }

            withClue("zone $zone") {
                thrown.message shouldBe
                        "Could not convert request body to '${kType<Meeting>().type}': Value at path 'root.at' must not be null"
            }
        }
    }
})

@JvmInline
value class VcId(val value: String) {
    init {
        require(value.isNotBlank()) { "VcId must not be blank" }
    }
}

data class Body(val id: VcId, val note: String)

data class Login(val email: String, val password: String)

data class Attrs(val attrs: Map<String, String>)

data class Meeting(val at: MpZonedDateTime)

data class GuardedBody(val amount: Int) {
    init {
        require(amount >= 0) { "amount must not be negative" }
    }
}
