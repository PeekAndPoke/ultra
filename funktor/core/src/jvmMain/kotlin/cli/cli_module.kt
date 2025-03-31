package de.peekandpoke.funktor.core.cli

import de.peekandpoke.funktor.core.cli.cmd.AppConfigCliCommand
import de.peekandpoke.funktor.core.cli.cmd.AppInfoCliCommand
import de.peekandpoke.ultra.kontainer.KontainerBuilder
import de.peekandpoke.ultra.kontainer.module

internal fun KontainerBuilder.funktorCli() = module(Funktor_Cli)

internal val Funktor_Cli = module {
    dynamic(CliRunner::class)

    // Commands
    singleton(AppInfoCliCommand::class)
    singleton(AppConfigCliCommand::class)
}
