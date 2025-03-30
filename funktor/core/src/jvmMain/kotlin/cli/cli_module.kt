package de.peekandpoke.ktorfx.core.cli

import de.peekandpoke.ktorfx.core.cli.cmd.AppConfigCliCommand
import de.peekandpoke.ktorfx.core.cli.cmd.AppInfoCliCommand
import de.peekandpoke.ultra.kontainer.KontainerBuilder
import de.peekandpoke.ultra.kontainer.module

internal fun KontainerBuilder.ktorFxCli() = module(KtorFX_Cli)

internal val KtorFX_Cli = module {
    dynamic(CliRunner::class)

    // Commands
    singleton(AppInfoCliCommand::class)
    singleton(AppConfigCliCommand::class)
}
