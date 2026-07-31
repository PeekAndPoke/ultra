package io.peekandpoke.ultra.slumber.builtin.model

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain
import io.peekandpoke.ultra.common.model.Redacted
import io.peekandpoke.ultra.slumber.Codec
import io.peekandpoke.ultra.slumber.awake
import io.peekandpoke.ultra.slumber.slumber

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

    "the placeholder is byte-identical to the kotlinx one" {
        // Both serializers must agree, or a value redacted by one is distinguishable from the other's.
        // Asserting against the shared constant is what keeps them in step.
        val out = codec.slumber(Conf("a", Redacted(secret), Redacted(Aws("r", "a")))) as Map<*, *>

        out["signingKey"] shouldBe Redacted.PLACEHOLDER
        Redacted.PLACEHOLDER shouldBe "***redacted***"
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
