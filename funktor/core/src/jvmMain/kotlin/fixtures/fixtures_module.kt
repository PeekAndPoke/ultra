package io.peekandpoke.funktor.core.fixtures

import io.peekandpoke.funktor.core.config.AppConfig
import io.peekandpoke.funktor.core.fixtures.cli.InstallFixturesCliCommand
import io.peekandpoke.funktor.core.fixtures.cli.ListFixturesCliCommand
import io.peekandpoke.ultra.kontainer.KontainerBuilder
import io.peekandpoke.ultra.kontainer.module

internal fun KontainerBuilder.funktorFixtures(config: AppConfig) = module(Funktor_Fixtures, config)

/**
 * Funktor_Fixtures kontainer module
 */
internal val Funktor_Fixtures = module { config: AppConfig ->

    if (config.ktor.isProduction) {

        singleton(FixtureInstaller::class, NullFixtureInstaller::class)
    } else {

        singleton(FixtureInstaller::class, SimpleFixtureInstaller::class)

        // Cli commands
        dynamic(InstallFixturesCliCommand::class)
        dynamic(ListFixturesCliCommand::class)
    }
}
