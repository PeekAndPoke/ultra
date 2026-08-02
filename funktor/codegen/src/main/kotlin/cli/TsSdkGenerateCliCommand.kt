package io.peekandpoke.funktor.codegen.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import io.peekandpoke.ultra.codegen.sdk.TsSdkBuilder
import java.io.File

/**
 * Generates the TypeScript SDK.
 *
 * ```
 * ./gradlew :funktor-demo:server:run --args="--cli sdk:ts:generate --out ../frontend/src/api"
 * ```
 *
 * A thin wrapper on purpose: everything it does is [TsSdkBuilder.build] plus file I/O, so a Gradle
 * task or a test can drive the same generation without clikt in the picture.
 */
class TsSdkGenerateCliCommand(
    builder: Lazy<TsSdkBuilder>,
) : CliktCommand(name = "sdk:ts:generate") {

    private val builder by builder

    private val out by option(
        "--out",
        help = "The APP ROOT. Must be ABSOLUTE — see the error for why.",
    ).required()

    private val sdkDir by option(
        "--sdkDir",
        help = "Where the SDK goes, RELATIVE to --out. The generator owns this directory outright.",
    ).default(DEFAULT_SDK_DIR)

    private val dryRun by option("--dry-run", help = "Print the planned files; write nothing").flag()

    private val check by option(
        "--check",
        help = "Exit non-zero when the SDK on disk differs from what would be generated",
    ).flag()

    private val verbose by option("--verbose", help = "Print a per-file and per-advisory summary").flag()

    override fun help(context: Context): String =
        "Generate the TypeScript SDK from the server's API features"

    companion object {
        /**
         * Where the SDK lands inside the app by default.
         *
         * `src/` because the consuming apps are Vue, whose sources live there — a default that is
         * wrong for the standard layout is a default nobody uses.
         */
        const val DEFAULT_SDK_DIR: String = "src/funktorsdk"
    }

    override fun run() {
        val appRoot = File(out)

        // THE destructive path — and it is NOT `--out`.
        //
        // `--out` is the app root; the generator owns only `<out>/<sdkDir>` and wipes it on every
        // run. Point this at `appRoot` and the generator deletes the whole application, silently,
        // the first time someone runs it against a directory that happens to carry the marker.
        // The distinction is two characters and there is no warning, so it is pinned by a test that
        // leaves a sentinel file at the app root and asserts it survives.
        val target = File(appRoot, sdkDir)

        // A relative --out resolves against the JVM's working directory, which for
        // `gradlew :funktor-demo:server:run` is the SERVER MODULE, not the repo root. Passing
        // `funktor-demo/sdkgen-app` therefore wrote a whole SDK under
        // `funktor-demo/server/funktor-demo/sdkgen-app/...` — silently, because that path is
        // perfectly valid. Refusing is better than guessing which root was meant.
        require(appRoot.isAbsolute) {
            "--out must be an absolute path, but was '$out'. A relative path resolves against this " +
                    "process's working directory — '${File("").absolutePath}' — which for a Gradle " +
                    "`run` task is the module directory, not the repository root. That silently " +
                    "writes the SDK somewhere plausible and wrong. Pass an absolute path, e.g. " +
                    "--out \"\$PWD/frontend\"."
        }

        // `--sdkDir` names the ONE directory the generator deletes and rewrites, so it must not be
        // able to point anywhere but inside the app. An absolute value would ignore --out entirely;
        // a `..` segment would walk the wipe back out into the app, or above it.
        require(!File(sdkDir).isAbsolute && !sdkDir.split('/', '\\').contains("..")) {
            "--sdkDir must be a relative path inside --out and must not contain '..', but was " +
                    "'$sdkDir'. It names the directory the generator OWNS and empties on every run."
        }

        // Validation happens inside build(); a failure throws before anything is planned, so the
        // target directory is never touched by a run that was going to fail.
        val result = builder.build()

        val entries = result.output.entries()

        if (verbose || dryRun) {
            println("${entries.size} file(s) planned for ${target.absolutePath}:")
            entries.forEach { println("  ${it.path}  (${it.writtenBy})") }
        }

        result.advisories.forEach { println("[advisory] $it") }

        when {
            // `--check` is not optional, and it comes BEFORE --dry-run: a stale checked-in SDK is
            // invisible until a frontend developer hits a runtime shape mismatch, so CI needs a
            // command that fails on drift rather than one that reports it in prose.
            check -> {
                val diffs = result.output.diffAgainst(target)

                if (diffs.isEmpty()) {
                    println("The generated SDK is up to date.")
                } else {
                    println("The generated SDK is STALE — ${diffs.size} difference(s):")
                    diffs.forEach { println("  $it") }
                    println("Re-run without --check to regenerate.")

                    throw StaleSdkException(diffs.size)
                }
            }

            dryRun -> println("Dry run — nothing written.")

            else -> {
                val written = result.output.writeTo(target)

                println("Wrote ${written.size} file(s) to ${target.absolutePath}")
            }
        }
    }

    /**
     * Thrown when `--check` finds drift.
     *
     * A distinct type rather than a bare `error()` so a caller embedding this command can tell "the
     * SDK is stale" apart from "generation failed", which are very different signals in CI.
     */
    class StaleSdkException(val differences: Int) : RuntimeException(
        "The generated TypeScript SDK is stale: $differences file(s) differ from what the current " +
                "API would produce. Regenerate it and commit the result."
    )
}
