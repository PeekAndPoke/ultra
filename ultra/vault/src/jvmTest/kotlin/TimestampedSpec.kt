package io.peekandpoke.ultra.vault

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.ultra.datetime.MpInstant
import io.peekandpoke.ultra.vault.hooks.Timestamped
import io.peekandpoke.ultra.vault.hooks.TimestampedMillis

class TimestampedSpec : StringSpec({

    // Timestamped ///////////////////////////////////////////////////////////////////////////////

    "Timestamped.withTimestamps sets createdAt and updatedAt on first call" {
        val entity = TimestampedEntity(
            createdAt = MpInstant.Epoch,
            updatedAt = MpInstant.Epoch,
        )

        val now = MpInstant.fromEpochMillis(1_000_000L)
        val result = entity.withTimestamps(now) as TimestampedEntity

        result.createdAt shouldBe now
        result.updatedAt shouldBe now
    }

    "Timestamped.withTimestamps only sets updatedAt on subsequent calls" {
        val firstTime = MpInstant.fromEpochMillis(1_000_000L)
        val secondTime = MpInstant.fromEpochMillis(2_000_000L)

        val entity = TimestampedEntity(
            createdAt = MpInstant.Epoch,
            updatedAt = MpInstant.Epoch,
        )

        val afterFirst = entity.withTimestamps(firstTime) as TimestampedEntity
        val afterSecond = afterFirst.withTimestamps(secondTime) as TimestampedEntity

        afterSecond.createdAt shouldBe firstTime
        afterSecond.updatedAt shouldBe secondTime
    }

    "Timestamped.withTimestamps preserves existing createdAt when it is already set" {
        val originalCreated = MpInstant.fromEpochMillis(500_000L)
        val now = MpInstant.fromEpochMillis(1_000_000L)

        val entity = TimestampedEntity(
            createdAt = originalCreated,
            updatedAt = MpInstant.Epoch,
        )

        val result = entity.withTimestamps(now) as TimestampedEntity

        result.createdAt shouldBe originalCreated
        result.updatedAt shouldBe now
    }

    // TimestampedMillis /////////////////////////////////////////////////////////////////////////

    "TimestampedMillis.withTimestamps sets createdMs and updatedMs on first call" {
        val entity = TimestampedMillisEntity(createdMs = 0L, updatedMs = 0L)

        val now = 1_000_000L
        val result = entity.withTimestamps(now) as TimestampedMillisEntity

        result.createdMs shouldBe now
        result.updatedMs shouldBe now
    }

    "TimestampedMillis.withTimestamps only sets updatedMs on subsequent calls" {
        val firstTime = 1_000_000L
        val secondTime = 2_000_000L

        val entity = TimestampedMillisEntity(createdMs = 0L, updatedMs = 0L)

        val afterFirst = entity.withTimestamps(firstTime) as TimestampedMillisEntity
        val afterSecond = afterFirst.withTimestamps(secondTime) as TimestampedMillisEntity

        afterSecond.createdMs shouldBe firstTime
        afterSecond.updatedMs shouldBe secondTime
    }

    "TimestampedMillis.withTimestamps preserves existing createdMs when it is already set" {
        val originalCreated = 500_000L
        val now = 1_000_000L

        val entity = TimestampedMillisEntity(createdMs = originalCreated, updatedMs = 0L)

        val result = entity.withTimestamps(now) as TimestampedMillisEntity

        result.createdMs shouldBe originalCreated
        result.updatedMs shouldBe now
    }
})

// Test fixtures ////////////////////////////////////////////////////////////////////////////////

private data class TimestampedEntity(
    override val createdAt: MpInstant,
    override val updatedAt: MpInstant,
) : Timestamped {
    override fun withCreatedAt(instant: MpInstant): Timestamped = copy(createdAt = instant)
    override fun withUpdatedAt(instant: MpInstant): Timestamped = copy(updatedAt = instant)
}

private data class TimestampedMillisEntity(
    override val createdMs: Long,
    override val updatedMs: Long,
) : TimestampedMillis {
    override fun withCreatedMs(ms: Long): TimestampedMillis = copy(createdMs = ms)
    override fun withUpdatedMs(ms: Long): TimestampedMillis = copy(updatedMs = ms)
}
