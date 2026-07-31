package io.peekandpoke.funktor.codegen

import com.github.ajalt.clikt.core.parse
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.peekandpoke.funktor.codegen.cli.TsSdkGenerateCliCommand
import io.peekandpoke.ultra.codegen.sdk.TsSdkBuilder

class TsSdkGenerateCliCommandSpec : FreeSpec() {

    /**
     * A builder that fails if it is ever touched.
     *
     * Lets a test assert not just THAT the command refused, but that it refused before doing any
     * work — an argument check that runs after generation is most of a wasted run.
     */
    private fun explodingBuilder(): Lazy<TsSdkBuilder> = lazy {
        error("the builder must not be reached when the arguments are invalid")
    }

    init {
        "--out must be absolute" - {

            // A relative path resolves against the JVM's working directory, which for
            // `gradlew :funktor-demo:server:run` is the SERVER MODULE, not the repo root. Passing
            // `funktor-demo/sdkgen-app/src/funktorsdk` wrote a complete SDK to
            // `funktor-demo/server/funktor-demo/sdkgen-app/src/funktorsdk` — silently, because that
            // path is perfectly valid. Found by doing exactly that, 2026-07-31.
            listOf(
                "funktor-demo/sdkgen-app/src/funktorsdk",
                "./out",
                "../sibling/sdk",
                "out",
            ).forEach { relative ->
                "rejects '$relative'" {
                    val command = TsSdkGenerateCliCommand(explodingBuilder())

                    val thrown = runCatching {
                        command.parse(arrayOf("--out", relative))
                    }.exceptionOrNull()

                    withClue("a relative --out must be refused, not resolved against a surprise root") {
                        (thrown != null) shouldBe true
                    }

                    thrown!!.message!! shouldContain "absolute"

                    withClue("the message must name the path so the mistake is obvious") {
                        thrown.message!! shouldContain relative
                    }
                }
            }

            "the refusal happens BEFORE the builder is touched" {
                // `explodingBuilder` throws a different message if it is reached, so this
                // distinguishes "refused early" from "generated, then refused".
                val command = TsSdkGenerateCliCommand(explodingBuilder())

                val thrown = runCatching { command.parse(arrayOf("--out", "relative/path")) }
                    .exceptionOrNull()

                thrown!!.message!! shouldContain "absolute"

                withClue("reaching the builder means the check ran too late") {
                    thrown.message!!.contains("must not be reached") shouldBe false
                }
            }
        }
    }
}
