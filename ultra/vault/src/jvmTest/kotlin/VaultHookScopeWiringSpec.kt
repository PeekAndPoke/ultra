package io.peekandpoke.ultra.vault

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.ultra.datetime.Kronos
import io.peekandpoke.ultra.kontainer.ServiceProvider
import io.peekandpoke.ultra.kontainer.kontainer
import io.peekandpoke.ultra.log.ultraLogging

class VaultHookScopeWiringSpec : StringSpec({

    "the hook scope must be shared across kontainers created from one blueprint" {
        val blueprint = kontainer {
            singleton(Kronos::class) { Kronos.systemUtc }

            ultraLogging()
            ultraVault(VaultConfig())
        }

        // The binder binds the app scope on the startup kontainer, while every request creates its
        // own. If these were different instances, bind() would be invisible to all requests and
        // the deferred path would silently never run.
        val fromSystemKontainer = blueprint.create().get(VaultHookScope::class)
        val fromRequestKontainer = blueprint.create().get(VaultHookScope::class)

        (fromSystemKontainer === fromRequestKontainer) shouldBe true
    }

    "the hook scope must be a true singleton, not semi-dynamic" {
        val blueprint = kontainer {
            singleton(Kronos::class) { Kronos.systemUtc }

            ultraLogging()
            ultraVault(VaultConfig())
        }

        // Guards the root cause directly: any (transitively) dynamic constructor dependency
        // promotes this to SemiDynamic, which is one instance per kontainer
        blueprint.create().getProvider(DeferredVaultHookScope::class).type shouldBe
                ServiceProvider.Type.Singleton
    }
})
