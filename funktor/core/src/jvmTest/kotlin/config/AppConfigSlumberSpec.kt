package io.peekandpoke.funktor.core.config

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.funktor.core.config.funktor.FunktorConfig
import io.peekandpoke.funktor.core.config.ktor.KtorConfig
import io.peekandpoke.ultra.common.model.Redacted
import io.peekandpoke.ultra.slumber.Codec
import io.peekandpoke.ultra.slumber.slumber

/**
 * `AppConfigCollector` slumbers the whole config tree into every FULL insights record, so **whatever
 * [AppConfig.of] returns must be slumberable**.
 *
 * Slumber only describes data classes; a plain class matches no branch in `BuiltInModule` and raises
 * "There is no known way to slumber the type". The in-repo config classes are all data classes, so
 * nothing exercised the published factory — an app built on `AppConfig.of(...)` would have thrown on
 * every request with insights at FULL. Jackson reflected over any POJO and so never needed this.
 */
class AppConfigSlumberSpec : StringSpec({

    val codec = Codec.default

    fun configWith(vararg keys: Pair<String, String>) = AppConfig.of(
        ktor = KtorConfig(),
        funktor = FunktorConfig(),
        keys = keys.associate { (k, v) -> k to Redacted(v) },
    )

    "the config returned by AppConfig.of(...) can be slumbered" {
        val slumbered = withClue("a plain (non-data) class throws here rather than producing a tree") {
            codec.slumber(configWith())
        }

        val map = slumbered as? Map<*, *>

        withClue("expected an object tree, got: $slumbered") {
            (map != null) shouldBe true
        }

        map!!.keys.map { it.toString() }.toSet() shouldBe setOf("ktor", "funktor", "keys")
    }

    "secrets in AppConfig.keys are redacted leaf-wise, not left to the container" {
        val rendered = codec.slumber(configWith("SENDGRID_API_KEY" to "sg-live-do-not-log")).toString()

        withClue("the value must never appear; the KEY NAME deliberately still does") {
            rendered.contains("sg-live-do-not-log") shouldBe false
            rendered.contains(Redacted.PLACEHOLDER) shouldBe true
            rendered.contains("SENDGRID_API_KEY") shouldBe true
        }
    }
})
