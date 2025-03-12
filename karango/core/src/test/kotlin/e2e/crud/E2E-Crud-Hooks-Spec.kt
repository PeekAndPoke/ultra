package de.peekandpoke.karango.e2e.crud

import de.peekandpoke.karango.e2e.database
import de.peekandpoke.karango.testdomain.testPersons
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import kotlin.time.Duration.Companion.seconds

@Suppress("ClassName")
class `E2E-Crud-Hooks-Spec` : StringSpec() {

    init {
        "Creating and updating entities must call all OnBeforeSave and OnAfterSave hooks" {

            // get coll and clear all entries
            val coll = database.testPersons.apply {
                removeAll()
                testHooks.reset()
            }

            val toBeSaved = listOf(
                JonBonJovi,
                EdgarAllanPoe
            )

            val saved = withClue("On creating entities all hooks must be called") {
                toBeSaved.mapIndexed { idx, it ->
                    coll.insert(it).also {
                        val expectedCount = 0 + idx + 1

                        database.testPersons.testHooks.onBeforeSave.forEach { hook ->
                            hook.received.shouldHaveSize(expectedCount)
                        }

                        eventually(1.seconds) {
                            database.testPersons.testHooks.onAfterSave.forEach { hook ->
                                hook.received.shouldHaveSize(expectedCount)
                            }
                        }

                        eventually(1.seconds) {
                            database.testPersons.testHooks.onAfterDelete.forEach { hook ->
                                hook.received.shouldBeEmpty()
                            }
                        }
                    }
                }
            }

            withClue("On updating entities all hooks must be called") {
                saved.mapIndexed { idx, it ->
                    coll.save(it).also {
                        val expectedCount = (toBeSaved.size * 1) + idx + 1

                        database.testPersons.testHooks.onBeforeSave.forEach { hook ->
                            hook.received.shouldHaveSize(expectedCount)
                        }

                        eventually(1.seconds) {
                            database.testPersons.testHooks.onAfterSave.forEach { hook ->
                                hook.received.shouldHaveSize(expectedCount)
                            }
                        }

                        eventually(1.seconds) {
                            database.testPersons.testHooks.onAfterDelete.forEach { hook ->
                                hook.received.shouldBeEmpty()
                            }
                        }
                    }
                }
            }
        }

        "Removing entities via repo.delete(entity) must call all OnAfterDelete hooks" {
            // get coll and clear all entries
            val coll = database.testPersons.apply {
                removeAll()
                testHooks.reset()
            }

            val toBeSaved = listOf(
                JonBonJovi,
                EdgarAllanPoe
            )

            val saved = toBeSaved.map { coll.insert(it) }

            withClue("On removing entities all hooks must be called") {
                saved.mapIndexed { idx, it ->
                    coll.remove(it).also {

                        val expectedDeleteCount = idx + 1

                        database.testPersons.testHooks.onBeforeSave.forEach { hook ->
                            hook.received.shouldHaveSize(toBeSaved.size)
                        }

                        eventually(1.seconds) {
                            database.testPersons.testHooks.onAfterSave.forEach { hook ->
                                hook.received.shouldHaveSize(toBeSaved.size)
                            }
                        }

                        eventually(1.seconds) {
                            database.testPersons.testHooks.onAfterDelete.forEach { hook ->
                                hook.received.shouldHaveSize(expectedDeleteCount)
                            }
                        }
                    }
                }
            }
        }
    }
}
