package de.peekandpoke.karango.e2e.crud

import de.peekandpoke.karango.e2e.database
import de.peekandpoke.karango.e2e.kronos
import de.peekandpoke.karango.testdomain.testPersons
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

@Suppress("ClassName", "LocalVariableName")
class `E2E-Crud-OnBeforeSave-Timestamped-Spec` : StringSpec({

    "Creating a document must set the 'createdAt' and 'updatedAt' timestamps" {

        // get coll and clear all entries
        val coll = database.testPersons.apply {
            removeAll()
        }

        // create some entries
        val jonSaved = coll.insert(JonBonJovi)

        val initialCreatedAt = jonSaved.value.createdAt
        val initialUpdatedAt = jonSaved.value.updatedAt

        withClue("createdAt must be set on initial save") {
            initialCreatedAt shouldBeGreaterThan kronos.instantNow().minus(1_000.milliseconds)
        }

        withClue("updatedAt must be set on initial save") {
            initialCreatedAt shouldBeGreaterThan kronos.instantNow().minus(1_000.milliseconds)
        }

        delay(100)

        val jonSavedAgain = coll.save(jonSaved)

        withClue("createdAt must NOT be changed on subsequent save") {
            jonSavedAgain.value.createdAt shouldBe initialCreatedAt
        }

        withClue("updatedAt must be changed on subsequent save") {
            jonSavedAgain.value.updatedAt shouldBeGreaterThan initialUpdatedAt
        }
    }
})
