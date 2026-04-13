package io.peekandpoke.funktor.core.cli

import com.github.ajalt.clikt.core.CliktCommand

/** Provides access to all registered CLI commands. */
class CliServices(
    commands: Lazy<List<CliktCommand>>,
) {
    val commands: List<CliktCommand> by commands
}
