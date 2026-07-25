package io.peekandpoke.funktor.rest.codec

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.funktor.core.broker.CouldNotConvertException
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

    "a require() in a plain data class init is normalized the same way" {
        shouldThrow<CouldNotConvertException> {
            codec.deserialize(kType<GuardedBody>().type, """{"amount":-1}""")
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

data class GuardedBody(val amount: Int) {
    init {
        require(amount >= 0) { "amount must not be negative" }
    }
}
