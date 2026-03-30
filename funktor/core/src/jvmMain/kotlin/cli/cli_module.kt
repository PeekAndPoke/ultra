package io.peekandpoke.funktor.core.cli

import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.peekandpoke.funktor.core.cli.cmd.AppConfigCliCommand
import io.peekandpoke.funktor.core.cli.cmd.AppInfoCliCommand
import io.peekandpoke.funktor.core.kontainer
import io.peekandpoke.ultra.kontainer.KontainerAware
import io.peekandpoke.ultra.kontainer.KontainerBuilder
import io.peekandpoke.ultra.kontainer.module

inline val KontainerAware.cliServices: CliServices get() = kontainer.get()
inline val ApplicationCall.cliServices: CliServices get() = kontainer.cliServices
inline val RoutingContext.cliServices: CliServices get() = call.cliServices

internal fun KontainerBuilder.funktorCli() = module(Funktor_Cli)

internal val Funktor_Cli = module {
    dynamic(CliRunner::class)
    dynamic(CliServices::class)

    // Commands
    singleton(AppInfoCliCommand::class)
    singleton(AppConfigCliCommand::class)
}
