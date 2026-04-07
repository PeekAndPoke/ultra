package io.peekandpoke.ultra.vault

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.ultra.datetime.MpInstant
import io.peekandpoke.ultra.vault.addons.SoftDeletable
import io.peekandpoke.ultra.vault.addons.SoftDelete

class SoftDeletableSpec : StringSpec({

    "SoftDeletable.isDeleted is true when softDelete is set" {
        val entity = SoftDeletableEntity(
            softDelete = SoftDelete(deletedAt = MpInstant.fromEpochMillis(1_000_000L))
        )

        entity.isDeleted shouldBe true
        entity.isNotDeleted shouldBe false
    }

    "SoftDeletable.isNotDeleted is true when softDelete is null" {
        val entity = SoftDeletableEntity(softDelete = null)

        entity.isDeleted shouldBe false
        entity.isNotDeleted shouldBe true
    }

    "SoftDelete data class holds deletedAt timestamp" {
        val instant = MpInstant.fromEpochMillis(5_000_000L)
        val softDelete = SoftDelete(deletedAt = instant)

        softDelete.deletedAt shouldBe instant
    }

    "SoftDeletable.Mutable.withSoftDelete replaces the softDelete field" {
        val entity = MutableSoftDeletableEntity(softDelete = null)
        val instant = MpInstant.fromEpochMillis(2_000_000L)

        val deleted = entity.withSoftDelete(SoftDelete(deletedAt = instant))

        deleted.isDeleted shouldBe true
        deleted.softDelete?.deletedAt shouldBe instant
    }
})

// Test fixtures ///////////////////////////////////////////////////////////////////////////////////

private data class SoftDeletableEntity(
    override val softDelete: SoftDelete?,
) : SoftDeletable

private data class MutableSoftDeletableEntity(
    override val softDelete: SoftDelete?,
) : SoftDeletable.Mutable<MutableSoftDeletableEntity> {
    override fun withSoftDelete(softDelete: SoftDelete?): MutableSoftDeletableEntity {
        return copy(softDelete = softDelete)
    }
}
