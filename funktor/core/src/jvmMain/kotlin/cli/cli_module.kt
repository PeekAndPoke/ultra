package io.peekandpoke.funktor.core.cli

import io.peekandpoke.funktor.core.cli.cmd.AppConfigCliCommand
import io.peekandpoke.funktor.core.cli.cmd.AppInfoCliCommand
import io.peekandpoke.ultra.kontainer.KontainerBuilder
import io.peekandpoke.ultra.kontainer.module

internal fun KontainerBuilder.funktorCli() = module(Funktor_Cli)

internal val Funktor_Cli = module {
    dynamic(CliRunner::class)

    // Commands
    singleton(AppInfoCliCommand::class)
    singleton(AppConfigCliCommand::class)
}
