package io.peekandpoke.karango.e2e.crud

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.peekandpoke.karango.e2e.createDatabase
import io.peekandpoke.karango.testdomain.TestPersonsRepository
import io.peekandpoke.ultra.vault.VaultHookScope

/**
 * Guards that the repository actually routes its after-save / after-delete hooks through the
 * driver's [VaultHookScope], rather than applying them directly.
 *
 * Without this, swapping the hook scope (as the funktor binder does at app start) would silently
 * have no effect on real repository operations.
 */
@Suppress("ClassName")
class `E2E-Crud-HookScope-Spec` : StringSpec() {

    class RecordingHookScope : VaultHookScope {
        val descriptions = mutableListOf<String>()

        override suspend fun runHook(description: String, block: suspend () -> Unit) {
            descriptions.add(description)
            block()
        }
    }

    init {
        "insert, save and remove must all route their hooks through the driver's hook scope" {

            val recording = RecordingHookScope()

            val (_, driver) = createDatabase(hookScope = recording) { driver ->
                listOf(TestPersonsRepository(driver = driver))
            }

            val repo = TestPersonsRepository(driver = driver).apply { removeAll() }

            recording.descriptions.clear()

            val stored = repo.insert(JonBonJovi)
            repo.save(stored)
            repo.remove(stored)

            recording.descriptions shouldContainExactly listOf(
                "test-persons.onAfterSave",
                "test-persons.onAfterSave",
                "test-persons.onAfterDelete",
            )
        }
    }
}
