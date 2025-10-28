package de.peekandpoke.karango.e2e.crud

import de.peekandpoke.karango.e2e.database
import de.peekandpoke.karango.e2e.kronos
import de.peekandpoke.karango.testdomain.TestTimestamped
import de.peekandpoke.karango.testdomain.testTimestamped
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

@Suppress("ClassName")
class `E2E-Crud-OnBeforeSave-Timestamped-Spec` : StringSpec({

    "Creating a document must set the 'createdAt' and 'updatedAt' timestamps" {

        // get coll and clear all entries
        val coll = database.testTimestamped.apply {
            removeAll()
        }

        // create some entries
        val entry = TestTimestamped(name = "Test")
        val saved = coll.insert(entry)

        val initialCreatedAt = saved.value.createdAt
        val initialUpdatedAt = saved.value.updatedAt

        withClue("createdAt must be set on initial save") {
            initialCreatedAt shouldBeGreaterThan kronos.instantNow().minus(1_000.milliseconds)
        }

        withClue("updatedAt must be set on initial save") {
            initialCreatedAt shouldBeGreaterThan kronos.instantNow().minus(1_000.milliseconds)
        }

        delay(100)

        val savedAgain = coll.save(saved)

        withClue("createdAt must NOT be changed on subsequent save") {
            savedAgain.value.createdAt shouldBe initialCreatedAt
        }

        withClue("updatedAt must be changed on subsequent save") {
            savedAgain.value.updatedAt shouldBeGreaterThan initialUpdatedAt.plus(99.milliseconds)
            savedAgain.value.updatedAt shouldBeLessThan initialUpdatedAt.plus(200.milliseconds)
        }
    }
})
