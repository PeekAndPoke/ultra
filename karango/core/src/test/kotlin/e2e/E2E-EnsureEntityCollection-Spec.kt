package io.peekandpoke.karango.e2e

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.future.await

@Suppress("ClassName")
class `E2E-EnsureEntityCollection-Spec` : StringSpec({

    "ensureEntityCollection awaits creation, so the collection exists as soon as it returns" {
        // Regression: when karango moved to the async Arango driver, createCollection(...) was left
        // fire-and-forget (its future not awaited). ensureEntityCollection returned before the
        // collection existed, so ensureIndexes() ran against a missing collection and failed with
        // 404 (collection or view not found) on the FIRST run of any brand-new collection.

        val name = "regression_ensure_entity_collection"
        val coll = karangoDriver.arangoDb.collection(name)

        // Arrange: guarantee the first-run condition — a collection that does not exist yet.
        if (coll.exists().await()) {
            coll.drop().await()
        }
        coll.exists().await() shouldBe false

        // Act
        karangoDriver.ensureEntityCollection(name)

        // Assert: with the fix the creation future is awaited, so the collection is guaranteed to
        // exist the moment ensureEntityCollection returns (before the fix this was racily false).
        coll.exists().await() shouldBe true

        // Cleanup
        coll.drop().await()
    }
})
