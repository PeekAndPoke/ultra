package de.peekandpoke.funktor.auth

import de.peekandpoke.funktor.auth.api.AuthApiFeature
import de.peekandpoke.funktor.auth.cli.AuthGenerateJwtSigningSecretCliCommand
import de.peekandpoke.funktor.auth.db.karango.KarangoAuthRecordsRepo
import de.peekandpoke.funktor.auth.db.monko.MonkoAuthRecordsRepo
import de.peekandpoke.funktor.auth.provider.EmailAndPasswordAuth
import de.peekandpoke.funktor.auth.provider.GithubSsoAuth
import de.peekandpoke.funktor.auth.provider.GoogleSsoAuth
import de.peekandpoke.funktor.core.kontainer
import de.peekandpoke.karango.vault.KarangoDriver
import de.peekandpoke.monko.MonkoDriver
import de.peekandpoke.ultra.kontainer.KontainerAware
import de.peekandpoke.ultra.kontainer.KontainerBuilder
import de.peekandpoke.ultra.kontainer.module
import de.peekandpoke.ultra.vault.hooks.TimestampedHook
import io.ktor.server.application.*
import io.ktor.server.routing.*

/** Helper for importing [FunktorAuth] into a [KontainerBuilder] */
fun KontainerBuilder.funktorAuth(
    builder: FunktorAuthBuilder.() -> Unit = {},
) = module(FunktorAuth, builder)

inline val KontainerAware.funktorAuth: AuthSystem get() = kontainer.get()
inline val ApplicationCall.funktorAuth: AuthSystem get() = kontainer.funktorAuth
inline val RoutingContext.funktorAuth: AuthSystem get() = call.funktorAuth

val FunktorAuth = module { builder: FunktorAuthBuilder.() -> Unit ->
    // Facade
    dynamic(AuthSystem::class)
    dynamic(AuthSystem.Deps::class)
    // Services
    instance(AuthRandom())
    // Provider Factories
    dynamic(EmailAndPasswordAuth.Factory::class)
    dynamic(GoogleSsoAuth.Factory::class)
    dynamic(GithubSsoAuth.Factory::class)
    // Storage
    dynamic(AuthRecordStorage::class)
    dynamic(AuthRecordStorage.Adapter::class) { AuthRecordStorage.Adapter.Null }
    // API
    singleton(AuthApiFeature::class)
    // CLI
    singleton(AuthGenerateJwtSigningSecretCliCommand::class)

    FunktorAuthBuilder(this).apply(builder)
}

class FunktorAuthBuilder internal constructor(private val kontainer: KontainerBuilder) {

    companion object {
        const val DEFAULT_AUTH_RECORDS_REPO_NAME = "system_auth_records"
    }

    fun useKarango(
        authRecordsRepoName: String = DEFAULT_AUTH_RECORDS_REPO_NAME,
    ) {
        with(kontainer) {
            dynamic(AuthRecordStorage.Adapter::class, AuthRecordStorage.Adapter.Vault::class)

            dynamic(KarangoAuthRecordsRepo::class) {
                    driver: KarangoDriver,
                    onAfterSaves: List<AuthRecordStorage.OnAfterSave>,
                    timestamped: TimestampedHook,
                ->
                KarangoAuthRecordsRepo(
                    name = authRecordsRepoName,
                    driver = driver,
                    onAfterSaves = onAfterSaves,
                    timestamped = timestamped,
                )
            }

            dynamic(KarangoAuthRecordsRepo.Fixtures::class)
        }
    }

    fun useMonko(
        authRecordsRepoName: String = DEFAULT_AUTH_RECORDS_REPO_NAME,
    ) {
        with(kontainer) {
            dynamic(AuthRecordStorage.Adapter::class, AuthRecordStorage.Adapter.Vault::class)

            dynamic(MonkoAuthRecordsRepo::class) {
                    driver: MonkoDriver,
                    onAfterSaves: List<AuthRecordStorage.OnAfterSave>,
                    timestamped: TimestampedHook,
                ->
                MonkoAuthRecordsRepo(
                    name = authRecordsRepoName,
                    driver = driver,
                    onAfterSaves = onAfterSaves,
                    timestamped = timestamped,
                )
            }

            dynamic(MonkoAuthRecordsRepo.Fixtures::class)
        }
    }
}
