package io.peekandpoke.funktor.saas

import io.peekandpoke.funktor.saas.storage.OrgsStorage
import io.peekandpoke.funktor.saas.storage.karango.KarangoOrgsRepo
import io.peekandpoke.funktor.saas.storage.monko.MonkoOrgsRepo
import io.peekandpoke.karango.vault.KarangoDriver
import io.peekandpoke.monko.MonkoDriver
import io.peekandpoke.ultra.kontainer.KontainerAware
import io.peekandpoke.ultra.kontainer.KontainerBuilder
import io.peekandpoke.ultra.kontainer.module
import io.peekandpoke.ultra.vault.hooks.TimestampedHook

fun KontainerBuilder.funktorSaas(
    builder: FunktorSaasBuilder.() -> Unit = {},
) = module(Funktor_Saas, builder)

inline val KontainerAware.funktorSaas: OrgsStorage get() = kontainer.get()

val Funktor_Saas = module { builder: FunktorSaasBuilder.() -> Unit ->
    // Organisation storage — no-op until a backend is selected
    singleton(OrgsStorage::class, OrgsStorage.Null::class)

    /////////////////////////////////////////////////////////////////////////////////
    // Apply external configuration
    FunktorSaasBuilder(this).apply(builder)
}

class FunktorSaasBuilder internal constructor(private val kontainer: KontainerBuilder) {

    fun useKarango(
        orgsRepoName: String = "system_organisations",
    ) {
        with(kontainer) {
            singleton(KarangoOrgsRepo::class) { driver: KarangoDriver, timestamped: TimestampedHook ->
                KarangoOrgsRepo(
                    driver = driver,
                    timestamped = timestamped,
                    repoName = orgsRepoName,
                )
            }

            singleton(OrgsStorage::class) { repo: KarangoOrgsRepo ->
                OrgsStorage.Vault(repo = repo)
            }
        }
    }

    fun useMonko(
        orgsRepoName: String = "system_organisations",
    ) {
        with(kontainer) {
            singleton(MonkoOrgsRepo::class) { driver: MonkoDriver, timestamped: TimestampedHook ->
                MonkoOrgsRepo(
                    name = orgsRepoName,
                    driver = driver,
                    timestamped = timestamped,
                )
            }

            singleton(OrgsStorage::class) { repo: MonkoOrgsRepo ->
                OrgsStorage.Vault(repo = repo)
            }
        }
    }
}
