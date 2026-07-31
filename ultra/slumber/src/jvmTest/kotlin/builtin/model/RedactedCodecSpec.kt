package io.peekandpoke.ultra.slumber.builtin.model

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldNotContain
import io.peekandpoke.ultra.common.model.Redacted
import io.peekandpoke.ultra.common.model.RedactedSerializer
import io.peekandpoke.ultra.slumber.AwakerException
import io.peekandpoke.ultra.slumber.Codec
import io.peekandpoke.ultra.slumber.awake
import io.peekandpoke.ultra.slumber.slumber
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonPrimitive

private data class Aws(val region: String, val account: String)

private data class Conf(
    val issuer: String,
    val signingKey: Redacted<String>,
    val aws: Redacted<Aws>,
)

private data class Nullable(val maybe: Redacted<String>?)

/**
 * The Slumber half of `Redacted`. Config loading goes through this and nothing else — HOCON only
 * produces a `Map<String, Any?>`, and `Codec.awake` does all the typing
 * (`funktor/core/.../config/AppConfig.kt:94-108`), so these tests are what say whether a server app can
 * load a config containing a redacted field at all.
 */
class RedactedCodecSpec : StringSpec({

    val secret = "ka2fEBWhmjPFpaPhg5Iir6tAX1COT0lG"
    val codec = Codec.default

    "a config loads from the NATURAL hocon shape — no config file has to change" {
        // This is the whole point of the awaker. `signingKey = "abc"`, not `signingKey { value = "abc" }`.
        // Without it Slumber falls through to DataClassAwaker and this throws.
        val hocon = mapOf(
            "issuer" to "app",
            "signingKey" to secret,
            "aws" to mapOf("region" to "eu-central-1", "account" to "acct"),
        )

        val conf = codec.awake<Conf>(hocon)!!

        conf.issuer shouldBe "app"
        conf.signingKey.value shouldBe secret
        conf.aws.value shouldBe Aws(region = "eu-central-1", account = "acct")
    }

    "slumbering emits the placeholder, for a scalar and for a whole subtree" {
        val conf = Conf("app", Redacted(secret), Redacted(Aws("eu-central-1", "SECRET-ACCT")))

        val out = codec.slumber(conf) as Map<*, *>

        out["issuer"] shouldBe "app"
        out["signingKey"] shouldBe Redacted.PLACEHOLDER
        // the subtree is REPLACED, not walked into and partly redacted
        out["aws"] shouldBe Redacted.PLACEHOLDER

        out.toString() shouldNotContain secret
        out.toString() shouldNotContain "SECRET-ACCT"
        out.toString() shouldNotContain "eu-central-1"
    }

    "Slumber's output matches kotlinx's, observed rather than assumed" {
        // The previous version asserted `out["signingKey"] shouldBe Redacted.PLACEHOLDER` and
        // `Redacted.PLACEHOLDER shouldBe "***redacted***"` — neither statement observes the kotlinx
        // serializer at all, so mutating RedactedSerializer.serialize to emit "REDACTED" left it green.
        // Compare the two ACTUAL outputs. RedactedSerializer is built by hand rather than via
        // @Serializable because ultra:slumber carries the kotlinx runtime but not its compiler plugin.
        val slumbered = codec.slumber(Conf("a", Redacted(secret), Redacted(Aws("r", "a")))) as Map<*, *>

        val viaKotlinx = Json.parseToJsonElement(
            Json.encodeToString(RedactedSerializer(String.serializer()), Redacted(secret))
        ).jsonPrimitive.content

        slumbered["signingKey"] shouldBe viaKotlinx
        slumbered["signingKey"] shouldNotBe secret
    }

    "reading the placeholder back THROWS — equivalently to kotlinx" {
        // The sharp edge, made loud on both sides. Previously the scalar case silently produced a
        // Redacted holding "***redacted***": a real object carrying a publicly known constant where a
        // secret belongs. A config rebuilt from an insights record would have booted and signed JWTs
        // with it.
        shouldThrow<AwakerException> {
            codec.awake<Conf>(
                mapOf(
                    "issuer" to "a",
                    "signingKey" to Redacted.PLACEHOLDER,
                    "aws" to mapOf("region" to "r", "account" to "a"),
                )
            )
        }
    }

    "a value that merely CONTAINS the placeholder text still awakes" {
        // Equality, not containment — a passphrase embedding the marker must stay usable.
        val awkward = "prefix-${Redacted.PLACEHOLDER}-suffix"

        val out = codec.awake<Conf>(
            mapOf(
                "issuer" to "a",
                "signingKey" to awkward,
                "aws" to mapOf("region" to "r", "account" to "a"),
            )
        )

        out!!.signingKey.value shouldBe awkward
    }

    "a null stays null rather than becoming a wrapper around nothing" {
        codec.awake<Nullable>(mapOf("maybe" to null))!!.maybe shouldBe null
    }

    "nothing else is disturbed — the branch claims Redacted and only Redacted" {
        val out = codec.slumber(Aws("eu", "acct")) as Map<*, *>

        out["region"] shouldBe "eu"
        out["account"] shouldBe "acct"
    }
})
