package io.peekandpoke.funktor.codegen

import com.github.ajalt.clikt.core.parse
import io.kotest.assertions.withClue
import java.io.File
import io.kotest.engine.spec.tempdir
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

    /** A real builder over one small feature — enough to emit a client, models and the runtime. */
    private fun realBuilder(): Lazy<TsSdkBuilder> = lazy {
        TsSdkBuilder.forTesting(
            listOf(RestApiTsContributor(lazyOf(listOf(FxDemoApiFeature(listOf(FxTalksApiRoutes()))))))
        )
    }

    /** Runs the command exactly as the CLI would, returning whatever it threw. */
    private fun run(vararg args: String): Throwable? = runCatching {
        TsSdkGenerateCliCommand(realBuilder()).parse(arrayOf(*args))
    }.exceptionOrNull()

    init {
        "--check, the way CI runs it" - {

            // `--check` is the entire reason a stale SDK is catchable: without it, a checked-in SDK
            // drifts from the server and nobody learns until a frontend developer hits a runtime
            // shape mismatch. Nothing had ever run it end to end.

            "passes on a freshly generated directory" {
                val dir = tempdir()

                run("--out", dir.absolutePath) shouldBe null

                withClue("a clean check must not throw, and must not rewrite anything") {
                    run("--out", dir.absolutePath, "--check") shouldBe null
                }
            }

            "FAILS when an emitted file was edited, naming it" {
                val dir = tempdir()

                run("--out", dir.absolutePath)

                val client = File(dir, "fxDemoClient.ts")
                client.writeText(client.readText() + "\n// someone edited the generated SDK\n")

                val thrown = run("--out", dir.absolutePath, "--check")

                withClue("an edited file must fail the check") {
                    (thrown is TsSdkGenerateCliCommand.StaleSdkException) shouldBe true
                }

                (thrown as TsSdkGenerateCliCommand.StaleSdkException).differences shouldBe 1
            }

            "FAILS when an emitted file was deleted" {
                val dir = tempdir()

                run("--out", dir.absolutePath)

                File(dir, "models.ts").delete() shouldBe true

                val thrown = run("--out", dir.absolutePath, "--check")

                (thrown is TsSdkGenerateCliCommand.StaleSdkException) shouldBe true
            }

            "FAILS on a file that is on disk but no longer generated" {
                // The stale-output case. A withdrawn endpoint's client lingers, keeps working, and a
                // check that only walked PLANNED entries would report the SDK as up to date — the
                // signal being false exactly when it matters.
                val dir = tempdir()

                run("--out", dir.absolutePath)

                File(dir, "withdrawnClient.ts").writeText("// an endpoint that no longer exists\n")

                val thrown = run("--out", dir.absolutePath, "--check")

                (thrown is TsSdkGenerateCliCommand.StaleSdkException) shouldBe true
            }

            "--dry-run writes nothing at all" {
                val dir = tempdir()

                run("--out", dir.absolutePath, "--dry-run") shouldBe null

                withClue("a dry run must leave the directory untouched, marker included") {
                    dir.listFiles().orEmpty().map { it.name } shouldBe emptyList()
                }
            }
        }

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
