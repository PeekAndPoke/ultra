package de.peekandpoke.ktorfx.core.fixtures

import de.peekandpoke.ktorfx.core.config.AppConfig
import de.peekandpoke.ktorfx.core.fixtures.cli.InstallFixturesCliCommand
import de.peekandpoke.ktorfx.core.fixtures.cli.ListFixturesCliCommand
import de.peekandpoke.ultra.kontainer.KontainerBuilder
import de.peekandpoke.ultra.kontainer.module

internal fun KontainerBuilder.ktorFxFixtures(config: AppConfig) = module(KtorFX_Fixtures, config)

/**
 * KtorFX_Fixtures kontainer module
 */
internal val KtorFX_Fixtures = module { config: AppConfig ->

    if (config.ktor.isProduction) {

        singleton(FixtureInstaller::class, NullFixtureInstaller::class)

    } else {

        singleton(FixtureInstaller::class, SimpleFixtureInstaller::class)

        // Cli commands
        dynamic(InstallFixturesCliCommand::class)
        dynamic(ListFixturesCliCommand::class)
    }
}
