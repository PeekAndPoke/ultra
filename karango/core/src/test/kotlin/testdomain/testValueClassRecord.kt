package io.peekandpoke.karango.testdomain

import io.peekandpoke.karango.vault.EntityRepository
import io.peekandpoke.karango.vault.KarangoDriver
import io.peekandpoke.ultra.reflection.kType
import io.peekandpoke.ultra.vault.Database
import io.peekandpoke.ultra.vault.Vault

val Database.testVcRecords get() = getRepository<TestVcRecordsRepository>()

class TestVcRecordsRepository(
    driver: KarangoDriver,
) : EntityRepository<TestVcRecord>(
    name = "test-vc-records",
    storedType = kType(),
    driver = driver,
)

/** A String-backed id value class — the RealmId shape. */
@JvmInline
value class TestRealmId(val value: String)

/** An Int-backed value class, [Comparable] so it can be used with ordered filters. */
@JvmInline
value class TestScore(val value: Int) : Comparable<TestScore> {
    override fun compareTo(other: TestScore): Int = value.compareTo(other.value)
}

@Vault
data class TestVcRecord(
    val realm: TestRealmId,
    val score: TestScore,
    val label: String,
)
